package com.karaoq.app.presentation.home

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import android.content.Intent
import androidx.core.content.FileProvider
import com.karaoq.app.data.audio.MicrophoneManager
import com.karaoq.app.data.audio.PerformanceRecorder
import com.karaoq.app.data.model.JobStatus
import com.karaoq.app.data.model.LeaderboardSubmitRequest
import com.karaoq.app.data.model.SeparationStatusResponse
import com.karaoq.app.data.remote.ApiClient
import com.karaoq.app.data.remote.SeparationWebSocketManager
import com.karaoq.app.domain.audio.KaraokeScoringEngine
import com.karaoq.app.domain.model.KaraoqTrack
import com.karaoq.app.domain.model.NavigationTab
import com.karaoq.app.domain.model.SavedSong
import com.karaoq.app.domain.model.SeparationUiState
import com.karaoq.app.domain.model.SongCreatePayloadDto
import com.karaoq.app.domain.model.SongLyrics
import com.karaoq.app.domain.model.StemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState(backendUrl = ApiClient.currentBaseUrl))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val microphoneManager = MicrophoneManager(application)
    val performanceRecorder = PerformanceRecorder(application)
    private val scoringEngine = KaraokeScoringEngine()
    private val webSocketManager = SeparationWebSocketManager()
    private var recordedPlayer: ExoPlayer? = null

    private var pollingJob: Job? = null
    private var progressTrackingJob: Job? = null
    private var countdownJob: Job? = null
    private var separationWebSocket: WebSocket? = null

    // DataSource HTTP com suporte obrigatório a redirecionamentos cross-protocol (http -> https)
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(30000)
        .setReadTimeoutMs(30000)

    private val mediaSourceFactory = DefaultMediaSourceFactory(application)
        .setDataSourceFactory(httpDataSourceFactory)

    // ExoPlayer para reprodução sincronizada de voz e playback
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(application)
        .setMediaSourceFactory(mediaSourceFactory)
        .build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            _uiState.update {
                                it.copy(
                                    durationMs = duration.coerceAtLeast(0L),
                                    errorMessage = null
                                )
                            }
                        }
                        Player.STATE_ENDED -> {
                            _uiState.update { it.copy(isPlaying = false) }
                            if (_uiState.value.isKaraokeActive) {
                                val recordedFile = performanceRecorder.stopRecording()
                                val song = _uiState.value.activeKaraokeSong
                                val summary = scoringEngine.finish(
                                    title = song?.title ?: "KaraoQ Track",
                                    artist = song?.artist ?: ""
                                )
                                _uiState.update {
                                    it.copy(
                                        isScoreModalVisible = true,
                                        scoreSummary = summary,
                                        recordedPerformanceFile = recordedFile,
                                        isScoreSubmitted = false
                                    )
                                }
                                song?.id?.let { loadLeaderboard(it) }
                                stopMicrophone()
                            }
                        }
                        else -> Unit
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("KaraoQ", "Erro de reprodução no ExoPlayer: ${error.errorCodeName}", error)
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            errorMessage = "Erro ao tocar áudio: ${error.message ?: error.errorCodeName}"
                        )
                    }
                }
            })
        }

    init {
        startPlayerTracking()
        loadSavedSongs()
        observeMicrophone()
        microphoneManager.pcmDataListener = { buffer, count ->
            performanceRecorder.writePcmBuffer(buffer, count)
        }
    }

    private fun observeMicrophone() {
        viewModelScope.launch {
            microphoneManager.amplitude.collect { amp ->
                if (_uiState.value.isMicActive) {
                    _uiState.update { it.copy(micAmplitude = amp) }
                }
            }
        }

        viewModelScope.launch {
            microphoneManager.pitch.collect { pitch ->
                if (_uiState.value.isMicActive && _uiState.value.isKaraokeActive && !_uiState.value.isCountdownRunning && _uiState.value.isPlaying) {
                    val frameResult = scoringEngine.processFrame(pitch)
                    _uiState.update {
                        it.copy(
                            currentPitchNote = pitch.noteName,
                            currentPitchHz = pitch.frequencyHz,
                            centsDeviation = pitch.centsDeviation,
                            isVoicePitched = pitch.isPitched,
                            vocalScore = frameResult.score,
                            comboCount = frameResult.combo,
                            comboMultiplier = frameResult.multiplier,
                            lastFeedbackText = if (frameResult.feedback.text.isNotEmpty()) frameResult.feedback.text else it.lastFeedbackText
                        )
                    }
                } else if (!_uiState.value.isMicActive) {
                    _uiState.update {
                        it.copy(
                            currentPitchNote = "",
                            currentPitchHz = 0f,
                            centsDeviation = 0,
                            isVoicePitched = false
                        )
                    }
                }
            }
        }
    }

    private fun startPlayerTracking() {
        progressTrackingJob = viewModelScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _uiState.update {
                        it.copy(
                            currentPositionMs = exoPlayer.currentPosition,
                            durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(250)
            }
        }
    }

    private fun sanitizeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http://") &&
            !url.contains("localhost") &&
            !url.contains("127.0.0.1") &&
            !url.contains("10.0.2.2")
        ) {
            url.replaceFirst("http://", "https://")
        } else {
            url
        }
    }

    fun switchTab(tab: NavigationTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == NavigationTab.LIBRARY) {
            loadSavedSongs()
        }
    }

    fun updateArtistInput(artist: String) {
        _uiState.update { it.copy(artistInput = artist) }
    }

    fun updateTitleInput(title: String) {
        _uiState.update { it.copy(titleInput = title) }
    }

    fun updateBackendUrl(newUrl: String) {
        ApiClient.updateBaseUrl(newUrl)
        _uiState.update { it.copy(backendUrl = ApiClient.currentBaseUrl) }
    }

    fun onFileSelected(uri: Uri) {
        val fileName = getFileNameFromUri(uri)
        val nameWithoutExt = fileName.substringBeforeLast(".")

        var detectedArtist = ""
        var detectedTitle = nameWithoutExt

        if (nameWithoutExt.contains(" - ")) {
            val parts = nameWithoutExt.split(" - ", limit = 2)
            detectedArtist = parts[0].trim()
            detectedTitle = parts[1].trim()
        }

        _uiState.update {
            it.copy(
                selectedFileUri = uri,
                selectedFileName = fileName,
                artistInput = if (it.artistInput.isBlank()) detectedArtist else it.artistInput,
                titleInput = if (it.titleInput.isBlank()) detectedTitle else it.titleInput,
                separationState = SeparationUiState.Idle,
                isSongSaved = false,
                errorMessage = null
            )
        }

        // Se já temos artista e título, busca a letra automaticamente
        if (detectedArtist.isNotBlank() && detectedTitle.isNotBlank()) {
            searchLyrics(detectedArtist, detectedTitle)
        }
    }

    fun searchLyrics(artist: String = _uiState.value.artistInput, title: String = _uiState.value.titleInput) {
        if (artist.isBlank() || title.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingLyrics = true, errorMessage = null) }
            try {
                val durationSec = if (_uiState.value.durationMs > 0) (_uiState.value.durationMs / 1000).toInt() else null
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.searchLyrics(
                        artist = artist.trim(),
                        title = title.trim(),
                        durationSeconds = durationSec
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val lyrics = SongLyrics(
                        isSynced = body.isSynced,
                        lines = body.lines,
                        plainText = body.plainText,
                        rawLrc = body.rawLrc,
                        source = body.source
                    )
                    _uiState.update {
                        it.copy(
                            lyrics = lyrics,
                            isSearchingLyrics = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSearchingLyrics = false) }
                }
            } catch (e: Exception) {
                Log.w("KaraoQ", "Falha na busca de letra: ${e.message}")
                _uiState.update { it.copy(isSearchingLyrics = false) }
            }
        }
    }

    fun startSeparation() {
        val uri = _uiState.value.selectedFileUri ?: return
        val filename = _uiState.value.selectedFileName ?: "audio.mp3"

        // Garante busca de letra caso ainda não tenha buscado
        if (_uiState.value.lyrics == null && _uiState.value.artistInput.isNotBlank() && _uiState.value.titleInput.isNotBlank()) {
            searchLyrics()
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    separationState = SeparationUiState.Uploading(10),
                    errorMessage = null
                )
            }

            try {
                val fileBytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.readBytes()
                    }
                }

                if (fileBytes == null || fileBytes.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            separationState = SeparationUiState.Error("Não foi possível ler o arquivo de áudio selecionado."),
                            errorMessage = "Erro ao acessar o arquivo de áudio local."
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(separationState = SeparationUiState.Uploading(60)) }

                val mediaType = "audio/*".toMediaTypeOrNull()
                val requestBody = fileBytes.toRequestBody(mediaType)
                val part = MultipartBody.Part.createFormData("audio_file", filename, requestBody)

                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.separateAudio(part)
                }

                if (response.isSuccessful && response.body() != null) {
                    val createData = response.body()!!
                    _uiState.update {
                        it.copy(
                            separationState = SeparationUiState.Processing(
                                progress = 10,
                                message = "Arquivo enviado! Aguardando processamento da IA..."
                            )
                        )
                    }
                    startTrackingStatus(createData.taskId, filename)
                } else {
                    val errorDetail = response.errorBody()?.string() ?: response.message()
                    _uiState.update {
                        it.copy(
                            separationState = SeparationUiState.Error("Erro no envio: $errorDetail"),
                            errorMessage = "Falha no servidor: ${response.code()}"
                        )
                    }
                }
            } catch (ex: Exception) {
                _uiState.update {
                    it.copy(
                        separationState = SeparationUiState.Error("Erro de conexão com o backend: ${ex.localizedMessage}"),
                        errorMessage = "Verifique se o backend está rodando no endereço informado."
                    )
                }
            }
        }
    }

    private fun startTrackingStatus(taskId: String, filename: String) {
        pollingJob?.cancel()
        separationWebSocket?.close(1000, null)
        separationWebSocket = null

        var hasReceivedWsUpdate = false

        try {
            separationWebSocket = webSocketManager.startStreaming(
                taskId = taskId,
                baseUrl = _uiState.value.backendUrl,
                onUpdate = { status ->
                    hasReceivedWsUpdate = true
                    viewModelScope.launch(Dispatchers.Main) {
                        handleSeparationStatusUpdate(status, taskId, filename)
                    }
                },
                onError = {
                    // Fallback imediato para HTTP Polling se o WebSocket falhar
                    if (!hasReceivedWsUpdate) {
                        viewModelScope.launch(Dispatchers.Main) {
                            startPollingStatus(taskId, filename)
                        }
                    }
                },
                onComplete = {
                    separationWebSocket = null
                }
            )
        } catch (e: Exception) {
            startPollingStatus(taskId, filename)
        }
    }

    private fun handleSeparationStatusUpdate(
        status: SeparationStatusResponse,
        taskId: String,
        filename: String
    ) {
        _uiState.update { it.copy(taskStatus = status) }

        when (JobStatus.fromString(status.status)) {
            JobStatus.COMPLETED -> {
                separationWebSocket?.close(1000, null)
                separationWebSocket = null

                val instrumentalSafe = sanitizeUrl(status.instrumentalUrl)
                val vocalsSafe = sanitizeUrl(status.vocalsUrl)
                val title = _uiState.value.titleInput.ifBlank { filename }
                val artist = _uiState.value.artistInput

                val track = KaraoqTrack(
                    id = taskId,
                    title = title,
                    instrumentalUrl = instrumentalSafe,
                    vocalsUrl = vocalsSafe
                )
                _uiState.update {
                    it.copy(
                        separationState = SeparationUiState.Ready(track),
                        activeStem = StemType.INSTRUMENTAL,
                        errorMessage = null
                    )
                }
                instrumentalSafe?.let { preparePlayer(it) }
                autoSaveSongToStorage(taskId, title, artist, instrumentalSafe, vocalsSafe)
            }
            JobStatus.FAILED -> {
                separationWebSocket?.close(1000, null)
                separationWebSocket = null

                val err = status.error ?: status.message
                _uiState.update {
                    it.copy(
                        separationState = SeparationUiState.Error(err),
                        errorMessage = err
                    )
                }
            }
            JobStatus.PROCESSING, JobStatus.QUEUED -> {
                _uiState.update {
                    it.copy(
                        separationState = SeparationUiState.Processing(
                            progress = status.progressPercentage.coerceAtLeast(12),
                            message = status.message
                        )
                    )
                }
            }
            else -> Unit
        }
    }

    private fun startPollingStatus(taskId: String, filename: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 300

            while (isActive && attempts < maxAttempts) {
                attempts++
                delay(2000)

                try {
                    val res = withContext(Dispatchers.IO) {
                        ApiClient.apiService.getSeparationStatus(taskId)
                    }

                    if (res.isSuccessful && res.body() != null) {
                        val status = res.body()!!
                        handleSeparationStatusUpdate(status, taskId, filename)
                        if (status.status == JobStatus.COMPLETED.name || status.status == JobStatus.FAILED.name) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    // Ignora falhas de rede temporárias no polling
                }
            }
        }
    }

    private fun autoSaveSongToStorage(
        taskId: String,
        title: String,
        artist: String,
        instrumentalUrl: String?,
        vocalsUrl: String?
    ) {
        viewModelScope.launch {
            try {
                val payload = SongCreatePayloadDto(
                    id = taskId,
                    title = title,
                    artist = artist,
                    durationMs = _uiState.value.durationMs,
                    vocalsUrl = vocalsUrl,
                    instrumentalUrl = instrumentalUrl,
                    lyrics = _uiState.value.lyrics
                )
                val res = withContext(Dispatchers.IO) {
                    ApiClient.apiService.saveSong(payload)
                }
                if (res.isSuccessful) {
                    _uiState.update { it.copy(isSongSaved = true) }
                    loadSavedSongs()
                }
            } catch (e: Exception) {
                Log.w("KaraoQ", "Erro ao salvar automaticamente no storage: ${e.message}")
            }
        }
    }

    fun loadSavedSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSavedSongs = true) }
            try {
                val res = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getSavedSongs()
                }
                if (res.isSuccessful && res.body() != null) {
                    val sanitizedSongs = res.body()!!.map { song ->
                        song.copy(
                            vocalsUrl = sanitizeUrl(song.vocalsUrl),
                            instrumentalUrl = sanitizeUrl(song.instrumentalUrl)
                        )
                    }
                    _uiState.update {
                        it.copy(
                            savedSongs = sanitizedSongs,
                            isLoadingSavedSongs = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingSavedSongs = false) }
                }
            } catch (e: Exception) {
                Log.w("KaraoQ", "Erro ao carregar músicas salvas: ${e.message}")
                _uiState.update { it.copy(isLoadingSavedSongs = false) }
            }
        }
    }

    fun playSavedSong(song: SavedSong) {
        startKaraoke(song)
    }

    fun startKaraoke(song: SavedSong) {
        countdownJob?.cancel()
        scoringEngine.reset()
        val track = KaraoqTrack(
            id = song.id,
            title = "${song.artist} - ${song.title}".trim().trim('-').trim(),
            instrumentalUrl = sanitizeUrl(song.instrumentalUrl),
            vocalsUrl = sanitizeUrl(song.vocalsUrl),
            durationMs = song.durationMs
        )

        _uiState.update {
            it.copy(
                isKaraokeActive = true,
                activeKaraokeSong = song,
                separationState = SeparationUiState.Ready(track),
                lyrics = song.lyrics,
                activeStem = StemType.INSTRUMENTAL,
                isVocalGuideActive = false,
                countdownRemaining = 5,
                isCountdownRunning = true,
                isPlaying = false,
                vocalScore = 0,
                comboCount = 0,
                comboMultiplier = 1,
                lastFeedbackText = "",
                isScoreModalVisible = false,
                scoreSummary = null,
                currentPitchNote = "",
                currentPitchHz = 0f,
                centsDeviation = 0,
                isVoicePitched = false,
                errorMessage = null
            )
        }

        // Prepara o playback instrumental em pausa na posição 0
        song.instrumentalUrl?.let {
            preparePlayer(it, autoPlay = false)
            viewModelScope.launch(Dispatchers.Main) {
                exoPlayer.seekTo(0L)
            }
        }

        // Inicia contagem regressiva de 5 segundos
        countdownJob = viewModelScope.launch {
            for (sec in 5 downTo 1) {
                _uiState.update { it.copy(countdownRemaining = sec, isCountdownRunning = true) }
                delay(1000)
            }
            _uiState.update { it.copy(countdownRemaining = 0, isCountdownRunning = false) }
            withContext(Dispatchers.Main) {
                exoPlayer.seekTo(0L)
                exoPlayer.play()
            }
            startMicrophoneIfActive()
            performanceRecorder.startRecording(song.id)
        }
    }

    fun skipCountdown() {
        countdownJob?.cancel()
        val songId = _uiState.value.activeKaraokeSong?.id ?: "unknown"
        _uiState.update { it.copy(countdownRemaining = 0, isCountdownRunning = false) }
        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
        }
        startMicrophoneIfActive()
        performanceRecorder.startRecording(songId)
    }

    fun restartKaraoke() {
        countdownJob?.cancel()
        scoringEngine.reset()
        performanceRecorder.stopRecording()
        recordedPlayer?.pause()
        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer.pause()
            exoPlayer.seekTo(0L)
        }
        stopMicrophone()
        _uiState.update {
            it.copy(
                countdownRemaining = 5,
                isCountdownRunning = true,
                isPlaying = false,
                vocalScore = 0,
                comboCount = 0,
                comboMultiplier = 1,
                lastFeedbackText = "",
                isScoreModalVisible = false,
                scoreSummary = null,
                recordedPerformanceFile = null,
                isPlayingRecordedPerformance = false,
                isScoreSubmitted = false,
                currentPitchNote = "",
                currentPitchHz = 0f,
                centsDeviation = 0,
                isVoicePitched = false
            )
        }
        val songId = _uiState.value.activeKaraokeSong?.id ?: "unknown"
        countdownJob = viewModelScope.launch {
            for (sec in 5 downTo 1) {
                _uiState.update { it.copy(countdownRemaining = sec, isCountdownRunning = true) }
                delay(1000)
            }
            _uiState.update { it.copy(countdownRemaining = 0, isCountdownRunning = false) }
            withContext(Dispatchers.Main) {
                exoPlayer.seekTo(0L)
                exoPlayer.play()
            }
            startMicrophoneIfActive()
            performanceRecorder.startRecording(songId)
        }
    }

    fun finishKaraokeManually() {
        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer.pause()
        }
        stopMicrophone()
        val recordedFile = performanceRecorder.stopRecording()
        val song = _uiState.value.activeKaraokeSong
        val summary = scoringEngine.finish(
            title = song?.title ?: "KaraoQ Track",
            artist = song?.artist ?: ""
        )
        _uiState.update {
            it.copy(
                isPlaying = false,
                isScoreModalVisible = true,
                scoreSummary = summary,
                recordedPerformanceFile = recordedFile,
                isScoreSubmitted = false
            )
        }
        song?.id?.let { loadLeaderboard(it) }
    }

    fun dismissScoreModal() {
        recordedPlayer?.pause()
        _uiState.update {
            it.copy(
                isScoreModalVisible = false,
                isPlayingRecordedPerformance = false
            )
        }
    }

    fun exitKaraoke() {
        countdownJob?.cancel()
        performanceRecorder.stopRecording()
        recordedPlayer?.pause()
        recordedPlayer?.release()
        recordedPlayer = null
        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer.pause()
        }
        stopMicrophone()
        _uiState.update {
            it.copy(
                isKaraokeActive = false,
                activeKaraokeSong = null,
                isCountdownRunning = false,
                countdownRemaining = 0,
                isScoreModalVisible = false,
                scoreSummary = null,
                recordedPerformanceFile = null,
                isPlayingRecordedPerformance = false
            )
        }
    }

    // --- Reprodução e Compartilhamento da Performance Gravada ---
    fun togglePlayRecordedPerformance() {
        val file = _uiState.value.recordedPerformanceFile ?: return
        if (_uiState.value.isPlayingRecordedPerformance) {
            recordedPlayer?.pause()
            _uiState.update { it.copy(isPlayingRecordedPerformance = false) }
        } else {
            if (recordedPlayer == null) {
                recordedPlayer = ExoPlayer.Builder(getApplication()).build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED) {
                                _uiState.update { it.copy(isPlayingRecordedPerformance = false) }
                            }
                        }
                    })
                }
            }
            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            recordedPlayer?.setMediaItem(mediaItem)
            recordedPlayer?.prepare()
            recordedPlayer?.play()
            _uiState.update { it.copy(isPlayingRecordedPerformance = true) }
        }
    }

    fun getShareIntentForRecordedPerformance(): Intent? {
        val file = _uiState.value.recordedPerformanceFile ?: return null
        val context = getApplication<Application>()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val songTitle = _uiState.value.activeKaraokeSong?.title ?: "música"
        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Minha performance no KaraoQ!")
            putExtra(Intent.EXTRA_TEXT, "Ouça como cantei a música '$songTitle' no KaraoQ! 🎤🔥")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // --- Transcrição com IA Multimodal (Gemini Audio) ---
    fun transcribeWithAi() {
        val taskId = _uiState.value.taskStatus?.taskId ?: return
        val artist = _uiState.value.artistInput
        val title = _uiState.value.titleInput

        viewModelScope.launch {
            _uiState.update { it.copy(isTranscribingLyrics = true, errorMessage = null) }
            try {
                val res = withContext(Dispatchers.IO) {
                    ApiClient.apiService.transcribeLyricsWithAi(
                        taskId = taskId,
                        artist = artist.trim(),
                        title = title.trim()
                    )
                }

                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    val lyrics = SongLyrics(
                        isSynced = body.isSynced,
                        lines = body.lines,
                        plainText = body.plainText,
                        rawLrc = body.rawLrc,
                        source = "gemini_ai"
                    )
                    _uiState.update {
                        it.copy(
                            lyrics = lyrics,
                            isTranscribingLyrics = false
                        )
                    }
                    Log.i("KaraoQ", "Letra transcrita com sucesso via Gemini Audio: ${lyrics.lines.size} versos.")
                } else {
                    val err = res.errorBody()?.string() ?: res.message()
                    _uiState.update {
                        it.copy(
                            isTranscribingLyrics = false,
                            errorMessage = "Erro na transcrição: $err"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("KaraoQ", "Exceção ao transcrever com IA: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isTranscribingLyrics = false,
                        errorMessage = "Falha ao transcrever com IA: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // --- Placar Global de Líderes (Leaderboard) ---
    fun loadLeaderboard(songId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLeaderboard = true) }
            try {
                val res = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getSongLeaderboard(songId, limit = 10)
                }
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update {
                        it.copy(
                            leaderboardEntries = res.body()!!,
                            isLoadingLeaderboard = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingLeaderboard = false) }
                }
            } catch (e: Exception) {
                Log.w("KaraoQ", "Erro ao buscar leaderboard: ${e.message}")
                _uiState.update { it.copy(isLoadingLeaderboard = false) }
            }
        }
    }

    fun updateSingerNameInput(name: String) {
        _uiState.update { it.copy(singerNameInput = name) }
    }

    fun submitCurrentScore(singerName: String) {
        val song = _uiState.value.activeKaraokeSong ?: return
        val summary = _uiState.value.scoreSummary ?: return
        val name = singerName.ifBlank { _uiState.value.singerNameInput.ifBlank { "Cantor Anônimo" } }

        viewModelScope.launch {
            try {
                val req = LeaderboardSubmitRequest(
                    singerName = name,
                    score = summary.totalScore,
                    rank = summary.rank.symbol,
                    maxCombo = summary.maxCombo,
                    perfectHits = summary.perfectHits
                )
                val res = withContext(Dispatchers.IO) {
                    ApiClient.apiService.submitScore(song.id, req)
                }
                if (res.isSuccessful) {
                    _uiState.update { it.copy(isScoreSubmitted = true) }
                    loadLeaderboard(song.id)
                }
            } catch (e: Exception) {
                Log.e("KaraoQ", "Erro ao submeter pontuação: ${e.message}", e)
            }
        }
    }

    fun toggleMic() {
        val next = !_uiState.value.isMicActive
        _uiState.update { it.copy(isMicActive = next) }
        if (next) {
            startMicrophoneIfActive()
        } else {
            stopMicrophone()
            _uiState.update { it.copy(micAmplitude = 0f) }
        }
    }

    fun toggleVocalGuide() {
        val current = _uiState.value.isVocalGuideActive
        val next = !current
        _uiState.update { it.copy(isVocalGuideActive = next) }
        val targetStem = if (next) StemType.VOCALS else StemType.INSTRUMENTAL
        switchStem(targetStem)
    }

    fun startMicrophoneIfActive() {
        if (_uiState.value.isMicActive && microphoneManager.hasPermission()) {
            microphoneManager.start(viewModelScope)
        }
    }

    fun stopMicrophone() {
        microphoneManager.stop()
    }

    fun onMicPermissionResult(granted: Boolean) {
        if (granted && _uiState.value.isKaraokeActive && !_uiState.value.isCountdownRunning) {
            startMicrophoneIfActive()
        }
    }

    fun deleteSavedSong(songId: String) {
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    ApiClient.apiService.deleteSavedSong(songId)
                }
                if (res.isSuccessful) {
                    loadSavedSongs()
                }
            } catch (e: Exception) {
                Log.w("KaraoQ", "Erro ao deletar música salva: ${e.message}")
            }
        }
    }

    private fun preparePlayer(audioUrl: String, autoPlay: Boolean = false) {
        val safeUrl = sanitizeUrl(audioUrl) ?: return
        Log.i("KaraoQ", "Preparando ExoPlayer com URL: $safeUrl")
        viewModelScope.launch(Dispatchers.Main) {
            val mediaItem = MediaItem.fromUri(safeUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (autoPlay) {
                exoPlayer.play()
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            val readyState = _uiState.value.separationState as? SeparationUiState.Ready
            val currentStem = _uiState.value.activeStem
            val currentTargetUrl = if (currentStem == StemType.VOCALS) {
                readyState?.track?.vocalsUrl
            } else {
                readyState?.track?.instrumentalUrl
            }

            if (exoPlayer.playbackState == Player.STATE_IDLE && currentTargetUrl != null) {
                preparePlayer(currentTargetUrl, autoPlay = true)
            } else {
                exoPlayer.play()
            }
        }
    }

    fun switchStem(stem: StemType) {
        val readyState = _uiState.value.separationState as? SeparationUiState.Ready ?: return
        val currentPos = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying

        val rawUrl = when (stem) {
            StemType.INSTRUMENTAL -> readyState.track.instrumentalUrl
            StemType.VOCALS -> readyState.track.vocalsUrl
            StemType.ORIGINAL -> readyState.track.instrumentalUrl
        } ?: return

        val targetUrl = sanitizeUrl(rawUrl) ?: return
        _uiState.update { it.copy(activeStem = stem) }

        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer.setMediaItem(MediaItem.fromUri(targetUrl))
            exoPlayer.prepare()
            exoPlayer.seekTo(currentPos)
            if (wasPlaying) {
                exoPlayer.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "audio_${System.currentTimeMillis()}.mp3"
        val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        pollingJob?.cancel()
        progressTrackingJob?.cancel()
        separationWebSocket?.close(1000, null)
        microphoneManager.stop()
        exoPlayer.release()
    }
}

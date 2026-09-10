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
import com.karaoq.app.data.model.JobStatus
import com.karaoq.app.data.remote.ApiClient
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

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState(backendUrl = ApiClient.currentBaseUrl))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var progressTrackingJob: Job? = null

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
                    startPollingStatus(createData.taskId, filename)
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
                        _uiState.update { it.copy(taskStatus = status) }

                        when (JobStatus.fromString(status.status)) {
                            JobStatus.COMPLETED -> {
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

                                // Salva automaticamente no storage para a biblioteca
                                autoSaveSongToStorage(taskId, title, artist, instrumentalSafe, vocalsSafe)
                                break
                            }
                            JobStatus.FAILED -> {
                                val err = status.error ?: status.message
                                _uiState.update {
                                    it.copy(
                                        separationState = SeparationUiState.Error(err),
                                        errorMessage = err
                                    )
                                }
                                break
                            }
                            JobStatus.PROCESSING, JobStatus.QUEUED -> {
                                _uiState.update {
                                    it.copy(
                                        separationState = SeparationUiState.Processing(
                                            progress = status.progressPercentage.coerceAtLeast(15),
                                            message = status.message
                                        )
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                } catch (e: Exception) {
                    // Ignora falhas de polling temporárias
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
        val track = KaraoqTrack(
            id = song.id,
            title = "${song.artist} - ${song.title}".trim().trim('-').trim(),
            instrumentalUrl = sanitizeUrl(song.instrumentalUrl),
            vocalsUrl = sanitizeUrl(song.vocalsUrl),
            durationMs = song.durationMs
        )

        _uiState.update {
            it.copy(
                separationState = SeparationUiState.Ready(track),
                lyrics = song.lyrics,
                activeStem = StemType.INSTRUMENTAL,
                currentTab = NavigationTab.CREATE, // Muda para a tela com o player
                artistInput = song.artist,
                titleInput = song.title,
                isSongSaved = true,
                errorMessage = null
            )
        }

        song.instrumentalUrl?.let {
            preparePlayer(it, autoPlay = true)
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
        pollingJob?.cancel()
        progressTrackingJob?.cancel()
        exoPlayer.release()
    }
}

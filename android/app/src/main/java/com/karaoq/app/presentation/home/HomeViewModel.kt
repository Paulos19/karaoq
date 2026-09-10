package com.karaoq.app.presentation.home

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.karaoq.app.data.model.JobStatus
import com.karaoq.app.data.remote.ApiClient
import com.karaoq.app.domain.model.KaraoqTrack
import com.karaoq.app.domain.model.SeparationUiState
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

    // ExoPlayer para reprodução sincronizada
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _uiState.update { it.copy(durationMs = duration.coerceAtLeast(0L)) }
                }
            }
        })
    }

    init {
        startPlayerTracking()
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
                delay(300)
            }
        }
    }

    fun updateBackendUrl(newUrl: String) {
        ApiClient.updateBaseUrl(newUrl)
        _uiState.update { it.copy(backendUrl = ApiClient.currentBaseUrl) }
    }

    fun onFileSelected(uri: Uri) {
        val fileName = getFileNameFromUri(uri)
        _uiState.update {
            it.copy(
                selectedFileUri = uri,
                selectedFileName = fileName,
                separationState = SeparationUiState.Idle,
                errorMessage = null
            )
        }
    }

    fun startSeparation() {
        val uri = _uiState.value.selectedFileUri ?: return
        val filename = _uiState.value.selectedFileName ?: "audio.mp3"

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    separationState = SeparationUiState.Uploading(10),
                    errorMessage = null
                )
            }

            try {
                // Lê os bytes do arquivo selecionado de forma assíncrona
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
            val maxAttempts = 300 // até ~10 minutos para áudios longos

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
                                val track = KaraoqTrack(
                                    id = taskId,
                                    title = filename,
                                    instrumentalUrl = status.instrumentalUrl,
                                    vocalsUrl = status.vocalsUrl
                                )
                                _uiState.update {
                                    it.copy(
                                        separationState = SeparationUiState.Ready(track),
                                        activeStem = StemType.INSTRUMENTAL
                                    )
                                }
                                // Prepara o áudio instrumental padrão no player
                                status.instrumentalUrl?.let { preparePlayer(it) }
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
                    // Ignora falhas pontuais de rede no polling
                }
            }
        }
    }

    private fun preparePlayer(audioUrl: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val mediaItem = MediaItem.fromUri(audioUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun switchStem(stem: StemType) {
        val readyState = _uiState.value.separationState as? SeparationUiState.Ready ?: return
        val currentPos = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying

        val targetUrl = when (stem) {
            StemType.INSTRUMENTAL -> readyState.track.instrumentalUrl
            StemType.VOCALS -> readyState.track.vocalsUrl
            StemType.ORIGINAL -> readyState.track.instrumentalUrl
        } ?: return

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

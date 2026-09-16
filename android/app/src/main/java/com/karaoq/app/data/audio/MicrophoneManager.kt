package com.karaoq.app.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MicrophoneManager(private val context: Context) {

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _pitch = MutableStateFlow(PitchResult())
    val pitch: StateFlow<PitchResult> = _pitch.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    var pcmDataListener: ((ShortArray, Int) -> Unit)? = null

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val pitchDetector = PitchDetector(sampleRate = sampleRate)

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start(scope: CoroutineScope) {
        if (!hasPermission()) {
            Log.w("MicrophoneManager", "Permissão RECORD_AUDIO não concedida.")
            return
        }

        if (_isRecording.value) return

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = (minBufferSize * 4).coerceAtLeast(4096)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("MicrophoneManager", "Falha ao inicializar AudioRecord.")
                    return@launch
                }

                audioRecord?.startRecording()
                _isRecording.value = true

                // Buffer de leitura de 2048 amostras (~46ms a 44.1kHz)
                val audioBuffer = ShortArray(2048)

                while (isActive && _isRecording.value) {
                    val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readCount > 0) {
                        // Notifica o gravador de performance vocal se ativo
                        pcmDataListener?.invoke(audioBuffer, readCount)

                        // 1. Calcula RMS (Root Mean Square) da amplitude
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            val sample = audioBuffer[i]
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readCount)
                        val normalized = (rms / 12000.0).toFloat().coerceIn(0f, 1f)

                        val current = _amplitude.value
                        val smoothed = current * 0.3f + normalized * 0.7f
                        _amplitude.value = smoothed

                        // 2. Detecção de afinação e nota musical em tempo real via YIN
                        if (readCount >= 1650) {
                            val detectedPitch = pitchDetector.detectPitch(audioBuffer, readCount)
                            _pitch.value = detectedPitch
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MicrophoneManager", "Erro na captura de áudio do microfone: ${e.message}", e)
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        try {
            _isRecording.value = false
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
            audioRecord = null
            _amplitude.value = 0f
            _pitch.value = PitchResult()
            recordingJob?.cancel()
            recordingJob = null
        } catch (e: Exception) {
            Log.w("MicrophoneManager", "Erro ao parar gravação: ${e.message}")
        }
    }
}

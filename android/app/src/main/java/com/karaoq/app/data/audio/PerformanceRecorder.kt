package com.karaoq.app.data.audio

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class PerformanceRecorder(private val context: Context) {

    private var currentFile: File? = null
    private var outputStream: FileOutputStream? = null
    private var totalPcmBytes: Long = 0
    private var isRecording: Boolean = false

    private val sampleRate = 44100
    private val channels = 1
    private val bitsPerSample = 16

    fun startRecording(songId: String): File? {
        stopRecording()

        return try {
            val dir = File(context.filesDir, "recordings")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val file = File(dir, "performance_${songId}_$timestamp.wav")
            currentFile = file
            outputStream = FileOutputStream(file)
            totalPcmBytes = 0

            // Escreve um cabeçalho temporário vazio de 44 bytes para ser preenchido ao final
            val placeholderHeader = ByteArray(44)
            outputStream?.write(placeholderHeader)

            isRecording = true
            Log.i("KaraoQ.Recorder", "Gravação iniciada: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("KaraoQ.Recorder", "Erro ao iniciar gravação da performance: ${e.message}", e)
            null
        }
    }

    fun writePcmBuffer(buffer: ShortArray, readCount: Int) {
        if (!isRecording || outputStream == null || readCount <= 0) return

        try {
            // Converte ShortArray (Little Endian PCM 16-bit) para ByteArray
            val byteBuffer = ByteArray(readCount * 2)
            for (i in 0 until readCount) {
                val sample = buffer[i].toInt()
                byteBuffer[i * 2] = (sample and 0x00FF).toByte()
                byteBuffer[i * 2 + 1] = ((sample shr 8) and 0x00FF).toByte()
            }
            outputStream?.write(byteBuffer)
            totalPcmBytes += byteBuffer.size
        } catch (e: Exception) {
            Log.e("KaraoQ.Recorder", "Erro ao escrever buffer PCM: ${e.message}", e)
        }
    }

    fun stopRecording(): File? {
        if (!isRecording && currentFile == null) return null

        isRecording = false
        try {
            outputStream?.flush()
            outputStream?.close()
            outputStream = null

            val file = currentFile
            if (file != null && file.exists() && totalPcmBytes > 0) {
                // Reescreve o cabeçalho WAV canônico com o tamanho total de bytes gravados
                writeWavHeader(file, totalPcmBytes, sampleRate, channels, bitsPerSample)
                Log.i("KaraoQ.Recorder", "Gravação concluída com sucesso (${totalPcmBytes} bytes): ${file.absolutePath}")
                return file
            }
        } catch (e: Exception) {
            Log.e("KaraoQ.Recorder", "Erro ao finalizar arquivo WAV: ${e.message}", e)
        }
        return null
    }

    private fun writeWavHeader(
        file: File,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Tamanho do subchunk format (16 para PCM)
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Formato de áudio = PCM (1)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (blockAlign.toInt() and 0xff).toByte()
        header[33] = ((blockAlign.toInt() shr 8) and 0xff).toByte()
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        raf.write(header)
        raf.close()
    }
}

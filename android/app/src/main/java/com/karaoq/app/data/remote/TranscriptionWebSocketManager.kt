package com.karaoq.app.data.remote

import android.util.Log
import com.google.gson.Gson
import com.karaoq.app.data.model.TranscriptionWebSocketMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class TranscriptionWebSocketManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {
    /**
     * Conecta ao canal WebSocket de transcrição Whisper em tempo real.
     */
    fun startStreaming(
        taskId: String,
        baseUrl: String,
        onUpdate: (TranscriptionWebSocketMessage) -> Unit,
        onError: (Throwable) -> Unit,
        onComplete: () -> Unit
    ): WebSocket {
        val cleanBase = baseUrl.trimEnd('/')
        val wsUrl = if (cleanBase.startsWith("https://")) {
            cleanBase.replaceFirst("https://", "wss://")
        } else {
            cleanBase.replaceFirst("http://", "ws://")
        } + "/api/v1/lyrics/ws/transcribe/$taskId"

        Log.i("KaraoQ.WhisperWS", "Conectando WebSocket de transcrição Whisper para tarefa $taskId em: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("KaraoQ.WhisperWS", "Conexão WebSocket de transcrição estabelecida para tarefa $taskId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, TranscriptionWebSocketMessage::class.java)
                    onUpdate(msg)
                } catch (e: Exception) {
                    Log.e("KaraoQ.WhisperWS", "Erro ao desserializar mensagem de transcrição: ${e.message}", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("KaraoQ.WhisperWS", "WebSocket de transcrição concluído.")
                onComplete()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("KaraoQ.WhisperWS", "Falha na conexão WebSocket de transcrição: ${t.message}")
                onError(t)
            }
        })
    }
}

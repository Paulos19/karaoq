package com.karaoq.app.data.remote

import android.util.Log
import com.google.gson.Gson
import com.karaoq.app.data.model.SeparationStatusResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class SeparationWebSocketManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Mantém conexão aberta sem timeout de leitura
        .pingInterval(15, TimeUnit.SECONDS)   // Heartbeat a cada 15 segundos
        .build(),
    private val gson: Gson = Gson()
) {

    /**
     * Inicia a conexão WebSocket para streaming de progresso em tempo real da tarefa.
     */
    fun startStreaming(
        taskId: String,
        baseUrl: String,
        onUpdate: (SeparationStatusResponse) -> Unit,
        onError: (Throwable) -> Unit,
        onComplete: () -> Unit
    ): WebSocket {
        val cleanBase = baseUrl.trimEnd('/')
        val wsUrl = if (cleanBase.startsWith("https://")) {
            cleanBase.replaceFirst("https://", "wss://")
        } else {
            cleanBase.replaceFirst("http://", "ws://")
        } + "/api/v1/separate/ws/$taskId"

        Log.i("KaraoQ.WS", "Conectando WebSocket para tarefa $taskId em: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("KaraoQ.WS", "Conexão WebSocket estabelecida com sucesso para tarefa $taskId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val statusResponse = gson.fromJson(text, SeparationStatusResponse::class.java)
                    onUpdate(statusResponse)
                } catch (e: Exception) {
                    Log.e("KaraoQ.WS", "Erro ao desserializar mensagem WebSocket: ${e.message}", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("KaraoQ.WS", "WebSocket fechando (code $code): $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("KaraoQ.WS", "WebSocket fechado com sucesso.")
                onComplete()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("KaraoQ.WS", "Falha na conexão WebSocket: ${t.message}. Acionando fallback HTTP...")
                onError(t)
            }
        })
    }
}

package com.karaoq.app.data.model

import com.google.gson.annotations.SerializedName

data class TranscriptionWebSocketMessage(
    @SerializedName("status")
    val status: String,
    @SerializedName("progress")
    val progress: Float = 0f,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("task_id")
    val taskId: String? = null,
    @SerializedName("time_ms")
    val timeMs: Long? = null,
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("lrc_line")
    val lrcLine: String? = null,
    @SerializedName("language")
    val language: String? = null,
    @SerializedName("result")
    val result: TranscriptionResult? = null
)

data class TranscriptionResult(
    @SerializedName("source")
    val source: String,
    @SerializedName("is_synced")
    val isSynced: Boolean,
    @SerializedName("lines")
    val lines: List<TranscriptionLineModel> = emptyList(),
    @SerializedName("raw_lrc")
    val rawLrc: String? = null,
    @SerializedName("plain_text")
    val plainText: String = ""
)

data class TranscriptionLineModel(
    @SerializedName("time_ms")
    val timeMs: Long,
    @SerializedName("text")
    val text: String
)

package com.karaoq.app.domain.model

import com.google.gson.annotations.SerializedName

data class LyricLine(
    @SerializedName("time_ms")
    val timeMs: Long,
    @SerializedName("text")
    val text: String
)

data class SongLyrics(
    @SerializedName("is_synced")
    val isSynced: Boolean = false,
    @SerializedName("lines")
    val lines: List<LyricLine> = emptyList(),
    @SerializedName("plain_text")
    val plainText: String = "",
    @SerializedName("raw_lrc")
    val rawLrc: String? = null,
    @SerializedName("source")
    val source: String? = null
)

data class SavedSong(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("artist")
    val artist: String,
    @SerializedName("duration_ms")
    val durationMs: Long = 0L,
    @SerializedName("vocals_url")
    val vocalsUrl: String? = null,
    @SerializedName("instrumental_url")
    val instrumentalUrl: String? = null,
    @SerializedName("lyrics")
    val lyrics: SongLyrics? = null,
    @SerializedName("created_at")
    val createdAt: Double = 0.0
)

data class LyricsSearchResponseDto(
    @SerializedName("source")
    val source: String,
    @SerializedName("is_synced")
    val isSynced: Boolean,
    @SerializedName("artist")
    val artist: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("lines")
    val lines: List<LyricLine>,
    @SerializedName("raw_lrc")
    val rawLrc: String?,
    @SerializedName("plain_text")
    val plainText: String
)

data class SongCreatePayloadDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("artist")
    val artist: String,
    @SerializedName("duration_ms")
    val durationMs: Long = 0L,
    @SerializedName("vocals_url")
    val vocalsUrl: String? = null,
    @SerializedName("instrumental_url")
    val instrumentalUrl: String? = null,
    @SerializedName("lyrics")
    val lyrics: SongLyrics? = null
)

enum class NavigationTab(val title: String) {
    CREATE("Criar Karaokê"),
    LIBRARY("Músicas Salvas")
}

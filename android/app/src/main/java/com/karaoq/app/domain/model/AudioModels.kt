package com.karaoq.app.domain.model

enum class StemType(val label: String) {
    INSTRUMENTAL("Playback (Sem Voz)"),
    VOCALS("Apenas Voz (Acapella)"),
    ORIGINAL("Áudio Original")
}

data class KaraoqTrack(
    val id: String,
    val title: String,
    val instrumentalUrl: String?,
    val vocalsUrl: String?,
    val durationMs: Long = 0L
)

sealed interface SeparationUiState {
    data object Idle : SeparationUiState
    data class Uploading(val progress: Int) : SeparationUiState
    data class Processing(val progress: Int, val message: String) : SeparationUiState
    data class Ready(val track: KaraoqTrack) : SeparationUiState
    data class Error(val message: String) : SeparationUiState
}

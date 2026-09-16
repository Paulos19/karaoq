package com.karaoq.app.presentation.home

import android.net.Uri
import com.karaoq.app.data.model.LeaderboardEntry
import com.karaoq.app.data.model.SeparationStatusResponse
import com.karaoq.app.domain.audio.KaraokeScoreSummary
import com.karaoq.app.domain.model.NavigationTab
import com.karaoq.app.domain.model.SavedSong
import com.karaoq.app.domain.model.SeparationUiState
import com.karaoq.app.domain.model.SongLyrics
import com.karaoq.app.domain.model.StemType
import java.io.File

data class HomeUiState(
    val backendUrl: String = "https://services-karaoq.khdya3.easypanel.host/",
    val currentTab: NavigationTab = NavigationTab.CREATE,

    // Seleção e metadados da música
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val artistInput: String = "",
    val titleInput: String = "",

    // Letras & Transcrição IA (Whisper)
    val lyrics: SongLyrics? = null,
    val isSearchingLyrics: Boolean = false,
    val isTranscribingLyrics: Boolean = false,
    val transcriptionProgress: Float = 0f,
    val partialTranscribedVerse: String = "",

    // Separação
    val separationState: SeparationUiState = SeparationUiState.Idle,
    val taskStatus: SeparationStatusResponse? = null,

    // Player
    val isPlaying: Boolean = false,
    val activeStem: StemType = StemType.INSTRUMENTAL,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,

    // Biblioteca de Músicas no Storage
    val savedSongs: List<SavedSong> = emptyList(),
    val isLoadingSavedSongs: Boolean = false,
    val isSavingSong: Boolean = false,
    val isSongSaved: Boolean = false,

    // Modo Karaokê (Palco)
    val isKaraokeActive: Boolean = false,
    val activeKaraokeSong: SavedSong? = null,
    val countdownRemaining: Int = 0,
    val isCountdownRunning: Boolean = false,
    val isMicActive: Boolean = true,
    val micAmplitude: Float = 0f,
    val isVocalGuideActive: Boolean = false,

    // Detecção de Tom Vocal e Pontuação em Tempo Real
    val currentPitchNote: String = "",
    val currentPitchHz: Float = 0f,
    val centsDeviation: Int = 0,
    val isVoicePitched: Boolean = false,
    val vocalScore: Int = 0,
    val comboCount: Int = 0,
    val comboMultiplier: Int = 1,
    val lastFeedbackText: String = "",
    val isScoreModalVisible: Boolean = false,
    val scoreSummary: KaraokeScoreSummary? = null,

    // Gravação da Performance Vocal
    val recordedPerformanceFile: File? = null,
    val isPlayingRecordedPerformance: Boolean = false,

    // Placar Global de Líderes (Leaderboard)
    val singerNameInput: String = "",
    val leaderboardEntries: List<LeaderboardEntry> = emptyList(),
    val isLoadingLeaderboard: Boolean = false,
    val isScoreSubmitted: Boolean = false,

    val errorMessage: String? = null
)

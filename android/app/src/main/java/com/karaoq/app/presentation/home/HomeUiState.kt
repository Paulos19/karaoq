package com.karaoq.app.presentation.home

import android.net.Uri
import com.karaoq.app.data.model.SeparationStatusResponse
import com.karaoq.app.domain.model.NavigationTab
import com.karaoq.app.domain.model.SavedSong
import com.karaoq.app.domain.model.SeparationUiState
import com.karaoq.app.domain.model.SongLyrics
import com.karaoq.app.domain.model.StemType

data class HomeUiState(
    val backendUrl: String = "https://services-karaoq.khdya3.easypanel.host/",
    val currentTab: NavigationTab = NavigationTab.CREATE,

    // Seleção e metadados da música
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val artistInput: String = "",
    val titleInput: String = "",

    // Letras
    val lyrics: SongLyrics? = null,
    val isSearchingLyrics: Boolean = false,

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

    val errorMessage: String? = null
)

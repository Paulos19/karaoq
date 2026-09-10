package com.karaoq.app.presentation.home

import android.net.Uri
import com.karaoq.app.data.model.SeparationStatusResponse
import com.karaoq.app.domain.model.SeparationUiState
import com.karaoq.app.domain.model.StemType

data class HomeUiState(
    val backendUrl: String = "http://localhost:8000/",
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val separationState: SeparationUiState = SeparationUiState.Idle,
    val taskStatus: SeparationStatusResponse? = null,
    val isPlaying: Boolean = false,
    val activeStem: StemType = StemType.INSTRUMENTAL,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

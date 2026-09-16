package com.karaoq.app.presentation.karaoke

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.karaoq.app.data.model.LeaderboardEntry
import com.karaoq.app.domain.audio.KaraokeScoreSummary
import com.karaoq.app.presentation.components.LyricsView
import com.karaoq.app.presentation.home.HomeUiState
import com.karaoq.app.presentation.home.HomeViewModel
import com.karaoq.app.presentation.ui.theme.AlertRed
import com.karaoq.app.presentation.ui.theme.DarkBackground
import com.karaoq.app.presentation.ui.theme.DarkSurface
import com.karaoq.app.presentation.ui.theme.DarkSurfaceBorder
import com.karaoq.app.presentation.ui.theme.DarkSurfaceVariant
import com.karaoq.app.presentation.ui.theme.ElectricGreen
import com.karaoq.app.presentation.ui.theme.NeonCyan
import com.karaoq.app.presentation.ui.theme.NeonPink
import com.karaoq.app.presentation.ui.theme.NeonPurple
import com.karaoq.app.presentation.ui.theme.TextPrimary
import com.karaoq.app.presentation.ui.theme.TextSecondary
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaraokeScreen(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    onExit: () -> Unit
) {
    val song = uiState.activeKaraokeSong

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onMicPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        if (!viewModel.microphoneManager.hasPermission()) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Barra Superior do Palco
            KaraokeTopBar(
                title = song?.title ?: "KaraoQ",
                artist = song?.artist ?: "",
                isVocalGuide = uiState.isVocalGuideActive,
                onToggleVocalGuide = { viewModel.toggleVocalGuide() },
                onExit = onExit
            )

            // 2. Placar Dinâmico & Indicador de Afinação em Tempo Real
            KaraokeLiveScoreBar(
                score = uiState.vocalScore,
                combo = uiState.comboCount,
                multiplier = uiState.comboMultiplier,
                note = uiState.currentPitchNote,
                isVoicePitched = uiState.isVoicePitched,
                feedback = uiState.lastFeedbackText
            )

            // 3. Temporizador Inicial Regressivo de 5 Segundos
            if (uiState.isCountdownRunning) {
                KaraokeCountdownBanner(
                    secondsRemaining = uiState.countdownRemaining,
                    onSkip = { viewModel.skipCountdown() }
                )
            }

            // 4. Cartão do Microfone e Afinador
            MicrophoneTunerCard(
                isMicActive = uiState.isMicActive,
                hasPermission = viewModel.microphoneManager.hasPermission(),
                amplitude = uiState.micAmplitude,
                pitchHz = uiState.currentPitchHz,
                pitchNote = uiState.currentPitchNote,
                centsDeviation = uiState.centsDeviation,
                isPitched = uiState.isVoicePitched,
                onToggleMic = { viewModel.toggleMic() },
                onRequestPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )

            // 5. Área Central de Letras (Teleprompter)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val lyrics = uiState.lyrics
                if (lyrics != null && lyrics.lines.isNotEmpty()) {
                    LyricsView(
                        modifier = Modifier.fillMaxSize(),
                        lyrics = lyrics,
                        currentPositionMs = uiState.currentPositionMs,
                        onSeek = { viewModel.seekTo(it) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "Aproveite o Playback Instrumental!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Solte a sua voz! O medidor de afinação e a pontuação já estão ativos captando suas notas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.taskStatus?.taskId != null) {
                                Button(
                                    onClick = { viewModel.transcribeWithAi() },
                                    enabled = !uiState.isTranscribingLyrics,
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (uiState.isTranscribingLyrics) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Transcrevendo com IA...", color = TextPrimary, fontSize = 12.sp)
                                    } else {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Gerar Letra com IA (Gemini) ✨", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Controles de Reprodução, Seek e Botão Finalizar
            KaraokeControlsBar(
                isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                onTogglePlay = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekTo(it) },
                onRestart = { viewModel.restartKaraoke() },
                onFinishEarly = { viewModel.finishKaraokeManually() }
            )
        }

        // 7. Modal de Fim de Música e Placar Final
        if (uiState.isScoreModalVisible && uiState.scoreSummary != null) {
            val context = LocalContext.current
            KaraokeResultDialog(
                summary = uiState.scoreSummary,
                recordedPerformanceFile = uiState.recordedPerformanceFile,
                isPlayingRecorded = uiState.isPlayingRecordedPerformance,
                onTogglePlayRecorded = { viewModel.togglePlayRecordedPerformance() },
                onSharePerformance = {
                    val shareIntent = viewModel.getShareIntentForRecordedPerformance()
                    if (shareIntent != null) {
                        val chooser = Intent.createChooser(shareIntent, "Compartilhar Gravação do KaraoQ")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    }
                },
                singerNameInput = uiState.singerNameInput,
                onSingerNameChange = { viewModel.updateSingerNameInput(it) },
                onSubmitScore = { viewModel.submitCurrentScore(uiState.singerNameInput) },
                isScoreSubmitted = uiState.isScoreSubmitted,
                leaderboardEntries = uiState.leaderboardEntries,
                isLoadingLeaderboard = uiState.isLoadingLeaderboard,
                onRestart = { viewModel.restartKaraoke() },
                onExit = onExit,
                onDismiss = { viewModel.dismissScoreModal() }
            )
        }
    }
}

@Composable
fun KaraokeLiveScoreBar(
    score: Int,
    combo: Int,
    multiplier: Int,
    note: String,
    isVoicePitched: Boolean,
    feedback: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Pontuação Acumulada
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = ElectricGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = String.format(Locale.getDefault(), "%,d", score),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "pts",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            // Nota Cantada no Momento
            if (isVoicePitched && note.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🎵 $note",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan
                    )
                }
            } else if (feedback.isNotBlank()) {
                Text(
                    text = feedback,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink
                )
            }

            // Multiplicador de Combo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (combo > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (multiplier >= 3) {
                                    Brush.horizontalGradient(listOf(NeonPink, NeonPurple))
                                } else {
                                    Brush.horizontalGradient(listOf(ElectricGreen, NeonCyan))
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "COMBO x$multiplier",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = DarkBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KaraokeTopBar(
    title: String,
    artist: String,
    isVocalGuide: Boolean,
    onToggleVocalGuide: () -> Unit,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkSurface)
                .border(1.dp, DarkSurfaceBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist.ifBlank { "KaraoQ Stage" },
                style = MaterialTheme.typography.bodySmall,
                color = NeonCyan,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isVocalGuide) {
                        Brush.horizontalGradient(listOf(NeonPink, NeonPurple))
                    } else {
                        Brush.horizontalGradient(listOf(DarkSurface, DarkSurface))
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isVocalGuide) NeonPink else DarkSurfaceBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onToggleVocalGuide() }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isVocalGuide) Icons.Default.Person else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isVocalGuide) TextPrimary else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isVocalGuide) "Voz Guia: ON" else "Playback",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isVocalGuide) TextPrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
fun KaraokeCountdownBanner(
    secondsRemaining: Int,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(listOf(NeonPink, NeonCyan, ElectricGreen)),
                shape = RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(NeonPink, NeonPurple))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$secondsRemaining",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Column {
                    Text(
                        text = "Prepare sua voz!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Entrada em $secondsRemaining segundo${if (secondsRemaining > 1) "s" else ""}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onSkip,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Pular",
                        tint = DarkBackground,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Pular",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MicrophoneTunerCard(
    isMicActive: Boolean,
    hasPermission: Boolean,
    amplitude: Float,
    pitchHz: Float,
    pitchNote: String,
    centsDeviation: Int,
    isPitched: Boolean,
    onToggleMic: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMicActive && hasPermission) ElectricGreen.copy(alpha = 0.2f) else AlertRed.copy(alpha = 0.2f)
                        )
                        .border(
                            1.dp,
                            if (isMicActive && hasPermission) ElectricGreen else AlertRed,
                            CircleShape
                        )
                        .clickable { onToggleMic() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMicActive && hasPermission) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microfone",
                        tint = if (isMicActive && hasPermission) ElectricGreen else AlertRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = if (!hasPermission) "Microfone Desativado" else if (isPitched && pitchNote.isNotBlank()) "Cantando: $pitchNote (${pitchHz.toInt()} Hz)" else if (isMicActive) "Microfone Pronto" else "Microfone Mudo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPitched) NeonCyan else TextPrimary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (!hasPermission) "Toque para autorizar" else if (isPitched) "Afinador: ${if (centsDeviation >= 0) "+$centsDeviation" else "$centsDeviation"} cents" else "Detecção vocal YIN ativa",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            if (hasPermission && isMicActive) {
                MicrophoneVisualizer(amplitude = amplitude)
            } else if (!hasPermission) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Permitir",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MicrophoneVisualizer(amplitude: Float) {
    val multipliers = listOf(0.4f, 0.7f, 1.0f, 0.8f, 0.5f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(24.dp)
    ) {
        multipliers.forEach { mult ->
            val barHeight by animateFloatAsState(
                targetValue = (5.dp.value + (amplitude * 19.dp.value * mult)).coerceIn(5f, 24f),
                animationSpec = tween(100),
                label = "barHeight"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(listOf(NeonCyan, ElectricGreen))
                    )
            )
        }
    }
}

@Composable
fun KaraokeControlsBar(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onRestart: () -> Unit,
    onFinishEarly: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            var isUserDragging by remember { mutableStateOf(false) }
            var sliderPos by remember { mutableFloatStateOf(0f) }

            val displayPos = if (isUserDragging) sliderPos.toLong() else currentPositionMs

            Slider(
                value = if (isUserDragging) sliderPos else currentPositionMs.toFloat(),
                onValueChange = {
                    isUserDragging = true
                    sliderPos = it
                },
                onValueChangeFinished = {
                    isUserDragging = false
                    onSeek(sliderPos.toLong())
                },
                valueRange = 0f..(durationMs.coerceAtLeast(1L)).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = DarkSurfaceBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(displayPos),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão Reiniciar Show
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkBackground)
                        .border(1.dp, DarkSurfaceBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Botão Central Play / Pause
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, NeonPink))
                        )
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Tocar",
                        tint = DarkBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Botão Finalizar e Ver Placar
                IconButton(
                    onClick = onFinishEarly,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkBackground)
                        .border(1.dp, DarkSurfaceBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Ver Placar",
                        tint = ElectricGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KaraokeResultDialog(
    summary: KaraokeScoreSummary,
    recordedPerformanceFile: File?,
    isPlayingRecorded: Boolean,
    onTogglePlayRecorded: () -> Unit,
    onSharePerformance: () -> Unit,
    singerNameInput: String,
    onSingerNameChange: (String) -> Unit,
    onSubmitScore: () -> Unit,
    isScoreSubmitted: Boolean,
    leaderboardEntries: List<LeaderboardEntry>,
    isLoadingLeaderboard: Boolean,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan, NeonPink, ElectricGreen)),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header com botão fechar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FIM DO SHOW!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                // Badge de Rank
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(NeonPink, NeonPurple))
                        )
                        .border(2.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = summary.rank.symbol,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Text(
                    text = summary.rank.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                // Estrelas
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i <= summary.stars) Color(0xFFFFD700) else DarkSurfaceBorder,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Pontuação Grande
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.getDefault(), "%,d", summary.totalScore),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricGreen
                    )
                    Text(
                        text = "de 10.000 pontos possíveis",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Tabela de Estatísticas
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatRow(label = "Maior Combo:", value = "${summary.maxCombo}x", color = NeonCyan)
                        StatRow(label = "Afinação Perfeita:", value = "${summary.perfectHits}", color = ElectricGreen)
                        StatRow(label = "Muito Bom:", value = "${summary.goodHits}", color = NeonPink)
                        StatRow(label = "Quase lá:", value = "${summary.okHits}", color = TextSecondary)
                    }
                }

                // --- Seção: Gravação da Sua Voz (Ouvir & Compartilhar) ---
                if (recordedPerformanceFile != null && recordedPerformanceFile.exists()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Sua Performance Gravada (WAV)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onTogglePlayRecorded,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlayingRecorded) NeonPink else NeonCyan
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingRecorded) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = DarkBackground,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPlayingRecorded) "Pausar" else "Ouvir Voz",
                                        color = DarkBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = onSharePerformance,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = NeonPink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Compartilhar",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Seção: Placar de Líderes (Top 10) ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = ElectricGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Top 10 do KaraoQ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Formulário de Envio de Pontuação
                        if (!isScoreSubmitted) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = singerNameInput,
                                    onValueChange = onSingerNameChange,
                                    label = { Text("Seu Apelido", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricGreen,
                                        unfocusedBorderColor = DarkSurfaceBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Button(
                                    onClick = onSubmitScore,
                                    enabled = singerNameInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(
                                        text = "Salvar 🏆",
                                        color = DarkBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElectricGreen.copy(alpha = 0.15f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ElectricGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Sua pontuação foi registrada no ranking!",
                                    color = ElectricGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Lista do Ranking
                        if (isLoadingLeaderboard) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ElectricGreen,
                                    strokeWidth = 2.dp
                                )
                            }
                        } else if (leaderboardEntries.isEmpty()) {
                            Text(
                                text = "Nenhuma pontuação registrada ainda. Seja o primeiro!",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                leaderboardEntries.forEachIndexed { index, entry ->
                                    val medal = when (index) {
                                        0 -> "🥇"
                                        1 -> "🥈"
                                        2 -> "🥉"
                                        else -> "${index + 1}º"
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (index == 0) NeonCyan.copy(alpha = 0.08f)
                                                else DarkSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = medal,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (index < 3) ElectricGreen else TextSecondary
                                            )
                                            Text(
                                                text = entry.singerName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "(${entry.rank})",
                                                fontSize = 10.sp,
                                                color = NeonPink
                                            )
                                        }
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%,d", entry.score)} pts",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Botões de Ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onRestart()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cantar Novamente",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onExit()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Sair do Palco",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

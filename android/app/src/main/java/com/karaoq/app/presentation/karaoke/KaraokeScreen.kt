package com.karaoq.app.presentation.karaoke

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.shadow
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
import com.karaoq.app.data.model.LeaderboardEntry
import com.karaoq.app.domain.audio.KaraokeScoreSummary
import com.karaoq.app.presentation.components.GlassCard
import com.karaoq.app.presentation.components.KaraoqMedalBadge
import com.karaoq.app.presentation.components.KaraoqPillButton
import com.karaoq.app.presentation.components.LyricsView
import com.karaoq.app.presentation.home.HomeUiState
import com.karaoq.app.presentation.home.HomeViewModel
import com.karaoq.app.presentation.ui.theme.AlertRed
import com.karaoq.app.presentation.ui.theme.AmberGlow
import com.karaoq.app.presentation.ui.theme.CardDarkSurface
import com.karaoq.app.presentation.ui.theme.CardElevated
import com.karaoq.app.presentation.ui.theme.FlameOrange
import com.karaoq.app.presentation.ui.theme.FlameOrangeLight
import com.karaoq.app.presentation.ui.theme.GlassBorder
import com.karaoq.app.presentation.ui.theme.GoldMedal
import com.karaoq.app.presentation.ui.theme.ObsidianDeep
import com.karaoq.app.presentation.ui.theme.PitchMint
import com.karaoq.app.presentation.ui.theme.SunsetCoral
import com.karaoq.app.presentation.ui.theme.TextMuted
import com.karaoq.app.presentation.ui.theme.TextPureWhite
import com.karaoq.app.presentation.ui.theme.TextSecondary
import java.io.File
import java.util.Locale

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDeep)
    ) {
        // 1. Palco Principal: Área de Letras Imersiva (O Cântico)
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val lyrics = uiState.lyrics
            if (lyrics != null && lyrics.lines.isNotEmpty()) {
                LyricsView(
                    modifier = Modifier.fillMaxSize(),
                    lyrics = lyrics,
                    currentPositionMs = uiState.currentPositionMs,
                    isStageMode = true,
                    onSeek = { viewModel.seekTo(it) }
                )
            } else {
                // Estado sem letra LRC: Guia para playback com Whisper AI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(FlameOrange.copy(alpha = 0.15f))
                                .border(1.dp, FlameOrange.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = FlameOrange,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Playback Instrumental Ativo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Solte a sua voz! O microfone e o medidor de afinação estão captando suas notas ao vivo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        if (uiState.taskStatus?.taskId != null) {
                            KaraoqPillButton(
                                text = if (uiState.isTranscribingLyrics) "Transcrevendo com Whisper..." else "Gerar Letra com Whisper IA",
                                icon = Icons.Rounded.AutoAwesome,
                                enabled = !uiState.isTranscribingLyrics,
                                onClick = { viewModel.transcribeWithAi() }
                            )
                        }
                    }
                }
            }
        }

        // 2. Barra Superior Minimalista (com statusBarsPadding para nunca cortar no BlueStacks)
        GeminiStageTopBar(
            title = song?.title ?: "KaraoQ Stage",
            artist = song?.artist.orEmpty(),
            score = uiState.vocalScore,
            comboMultiplier = uiState.comboMultiplier,
            isVocalGuide = uiState.isVocalGuideActive,
            onToggleVocalGuide = { viewModel.toggleVocalGuide() },
            onExit = onExit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // 3. Lead-in Countdown Flutuante (quando isCountdownRunning)
        AnimatedVisibility(
            visible = uiState.isCountdownRunning,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp, start = 20.dp, end = 20.dp)
        ) {
            GeminiCountdownPill(
                secondsRemaining = uiState.countdownRemaining,
                onSkip = { viewModel.skipCountdown() }
            )
        }

        // 4. Gemini Floating Bottom Dock (Controles + WaveProgress Voice Detector)
        GeminiStageDock(
            isPlaying = uiState.isPlaying,
            currentPositionMs = uiState.currentPositionMs,
            durationMs = uiState.durationMs,
            micAmplitude = uiState.micAmplitude,
            pitchNote = uiState.currentPitchNote,
            isVoiceDetected = uiState.isVoicePitched || uiState.micAmplitude > 0.08f,
            isMicActive = uiState.isMicActive,
            hasMicPermission = viewModel.microphoneManager.hasPermission(),
            onTogglePlay = { viewModel.togglePlayPause() },
            onSeek = { viewModel.seekTo(it) },
            onRestart = { viewModel.restartKaraoke() },
            onFinishShow = { viewModel.finishKaraokeManually() },
            onRequestMicPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )

        // 5. Modal de Fim de Música e Placar Final
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

/**
 * TopBar limpa inspirada no Gemini com proteção contra corte de status bar no BlueStacks.
 */
@Composable
private fun GeminiStageTopBar(
    title: String,
    artist: String,
    score: Int,
    comboMultiplier: Int,
    isVocalGuide: Boolean,
    onToggleVocalGuide: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Botão Voltar circular estilo Gemini
        IconButton(
            onClick = onExit,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CardDarkSurface.copy(alpha = 0.9f))
                .border(1.dp, GlassBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Sair do Palco",
                tint = TextPureWhite,
                modifier = Modifier.size(20.dp)
            )
        }

        // Título e Artista centralizados
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist.ifBlank { "KaraoQ Live" },
                style = MaterialTheme.typography.bodySmall,
                color = FlameOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Ações do Topo: Switch de Guia Vocal e Placar ao Vivo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chip de Guia Vocal
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isVocalGuide) SunsetCoral.copy(alpha = 0.25f) else CardDarkSurface.copy(alpha = 0.9f)
                    )
                    .border(
                        1.dp,
                        if (isVocalGuide) SunsetCoral else GlassBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onToggleVocalGuide() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isVocalGuide) Icons.Rounded.Person else Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (isVocalGuide) SunsetCoral else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isVocalGuide) "Guia" else "Playback",
                        color = if (isVocalGuide) TextPureWhite else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Chip do Placar ao Vivo
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardDarkSurface.copy(alpha = 0.9f))
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = AmberGlow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%,d", score),
                        color = TextPureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Indicador de Combo quando ativo
            if (comboMultiplier > 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(FlameOrange, SunsetCoral)))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "x$comboMultiplier",
                        color = TextPureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * Lead-in countdown sutil e flutuante estilo Gemini pill.
 */
@Composable
private fun GeminiCountdownPill(
    secondsRemaining: Int,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "countdown_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pill_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(listOf(CardElevated, CardDarkSurface))
            )
            .border(1.dp, FlameOrange.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(FlameOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$secondsRemaining",
                    color = TextPureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = "Entrada em $secondsRemaining s...",
                color = TextPureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(FlameOrange.copy(alpha = 0.2f))
                    .clickable { onSkip() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "Pular",
                        tint = FlameOrange,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Pular",
                        color = FlameOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Floating Audio Dock inspirado diretamente no design de barra flutuante do Google Gemini (imagens 2 e 3).
 * Abriga o visualizador WaveProgress em tempo real que ganha vida ao cantar.
 */
@Composable
private fun GeminiStageDock(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    micAmplitude: Float,
    pitchNote: String,
    isVoiceDetected: Boolean,
    isMicActive: Boolean,
    hasMicPermission: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onRestart: () -> Unit,
    onFinishShow: () -> Unit,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dockShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = dockShape,
                ambientColor = Color.Black.copy(alpha = 0.8f),
                spotColor = FlameOrange.copy(alpha = 0.3f)
            )
            .clip(dockShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        CardElevated.copy(alpha = 0.98f),
                        CardDarkSurface.copy(alpha = 0.98f)
                    )
                )
            )
            .border(1.dp, GlassBorder, dockShape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Seekbar ultra-fina no topo do dock
            var isUserDragging by remember { mutableStateOf(false) }
            var sliderPos by remember { mutableFloatStateOf(0f) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(if (isUserDragging) sliderPos.toLong() else currentPositionMs),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

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
                        thumbColor = FlameOrange,
                        activeTrackColor = FlameOrange,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .padding(horizontal = 8.dp)
                )

                Text(
                    text = formatTime(durationMs),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Linha principal de controles & WaveProgress Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Ações à esquerda: Reiniciar e Play/Pause
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onRestart,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reiniciar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botão Play/Pause principal com gradiente Flame
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(FlameOrange, SunsetCoral))
                            )
                            .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = TextPureWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Centro / Direita: Gemini WaveProgress Capsule (detector vocal vivo)
                GeminiWaveProgress(
                    isVoiceDetected = isVoiceDetected,
                    amplitude = micAmplitude,
                    pitchNote = pitchNote,
                    isMicActive = isMicActive,
                    hasPermission = hasMicPermission,
                    modifier = Modifier.clickable {
                        if (!hasMicPermission) onRequestMicPermission()
                    }
                )

                // Botão Finalizar Show
                IconButton(
                    onClick = onFinishShow,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, GlassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Finalizar",
                        tint = PitchMint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modal moderno de resultado ao término da performance.
 */
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
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
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
                        text = "FIM DO SHOW",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = FlameOrange
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Fechar", tint = TextMuted)
                    }
                }

                // Símbolo do Rank
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(FlameOrange, SunsetCoral))
                        )
                        .border(2.dp, GoldMedal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = summary.rank.symbol,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPureWhite
                    )
                }

                Text(
                    text = summary.rank.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    textAlign = TextAlign.Center
                )

                // Estrelas
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (i <= summary.stars) GoldMedal else Color.White.copy(alpha = 0.15f),
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
                        color = TextPureWhite
                    )
                    Text(
                        text = "de 10.000 pontos possíveis",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // Estatísticas
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatRow(label = "Precisão Vocal:", value = "${summary.accuracyPercentage}%", color = PitchMint)
                        StatRow(label = "Maior Combo:", value = "${summary.maxCombo}x", color = FlameOrange)
                        StatRow(label = "Afinação Perfeita:", value = "${summary.perfectHits}", color = GoldMedal)
                        StatRow(label = "Muito Bom:", value = "${summary.goodHits}", color = TextPureWhite)
                        StatRow(label = "Quase lá:", value = "${summary.okHits}", color = TextSecondary)
                    }
                }

                // Gravação da Voz
                if (recordedPerformanceFile != null && recordedPerformanceFile.exists()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = null,
                                    tint = FlameOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Sua Gravação Vocal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPureWhite
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onTogglePlayRecorded,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingRecorded) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = TextPureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPlayingRecorded) "Pausar" else "Ouvir",
                                        color = TextPureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = onSharePerformance,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null,
                                        tint = TextPureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Compartilhar",
                                        color = TextPureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Submeter ao Ranking
                if (!isScoreSubmitted) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Registre sua performance no ranking:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = singerNameInput,
                                onValueChange = onSingerNameChange,
                                placeholder = { Text("Seu nome artístico") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FlameOrange,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedTextColor = TextPureWhite,
                                    unfocusedTextColor = TextPureWhite
                                )
                            )
                            Button(
                                onClick = onSubmitScore,
                                enabled = singerNameInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    text = "Salvar",
                                    color = TextPureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PitchMint.copy(alpha = 0.15f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = PitchMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sua pontuação foi registrada no ranking!",
                            color = PitchMint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Ranking Rápido
                if (isLoadingLeaderboard) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = FlameOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (leaderboardEntries.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        leaderboardEntries.take(5).forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardElevated)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    KaraoqMedalBadge(rank = index + 1, size = 18.dp)
                                    Text(
                                        text = entry.singerName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPureWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${entry.score} pts",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AmberGlow
                                )
                            }
                        }
                    }
                }

                // Botões de Ação do Fim
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                    ) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPureWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cantar de Novo", color = TextPureWhite, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onExit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Voltar ao Início", color = TextPureWhite, fontWeight = FontWeight.Bold)
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
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

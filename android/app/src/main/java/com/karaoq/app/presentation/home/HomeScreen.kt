package com.karaoq.app.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karaoq.app.R
import com.karaoq.app.domain.model.NavigationTab
import com.karaoq.app.domain.model.SeparationUiState
import com.karaoq.app.domain.model.SongLyrics
import com.karaoq.app.domain.model.StemType
import com.karaoq.app.presentation.components.GlassCard
import com.karaoq.app.presentation.components.KaraoqBottomDock
import com.karaoq.app.presentation.components.KaraoqPillButton
import com.karaoq.app.presentation.components.LyricsView
import com.karaoq.app.presentation.components.WaveformVisualizer
import com.karaoq.app.presentation.karaoke.KaraokeScreen
import com.karaoq.app.presentation.leaderboard.LeaderboardScreen
import com.karaoq.app.presentation.library.SavedSongsScreen
import com.karaoq.app.presentation.settings.SettingsScreen
import com.karaoq.app.presentation.ui.theme.AlertRed
import com.karaoq.app.presentation.ui.theme.AmberGlow
import com.karaoq.app.presentation.ui.theme.CardDarkSurface
import com.karaoq.app.presentation.ui.theme.CardElevated
import com.karaoq.app.presentation.ui.theme.FlameOrange
import com.karaoq.app.presentation.ui.theme.FlameOrangeLight
import com.karaoq.app.presentation.ui.theme.GlassBorder
import com.karaoq.app.presentation.ui.theme.ObsidianDeep
import com.karaoq.app.presentation.ui.theme.PitchMint
import com.karaoq.app.presentation.ui.theme.SunsetCoral
import com.karaoq.app.presentation.ui.theme.TextMuted
import com.karaoq.app.presentation.ui.theme.TextPureWhite
import com.karaoq.app.presentation.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Se o modo Karaokê estiver ativo, renderiza a tela dedicada do Palco com microfone e pitch
    if (uiState.isKaraokeActive) {
        KaraokeScreen(
            modifier = modifier,
            uiState = uiState,
            viewModel = viewModel,
            onExit = { viewModel.exitKaraoke() }
        )
        return
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianDeep,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_karaoq_logo),
                            contentDescription = "KaraoQ Logo",
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.ic_karaoq_logo_extenso),
                                contentDescription = "KARAOQ",
                                modifier = Modifier.height(20.dp)
                            )
                            Text(
                                text = "AI Stem & Lyrics Sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = FlameOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    // Indicador de status de conexão do Backend
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CardDarkSurface)
                            .border(1.dp, GlassBorder, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PitchMint)
                            )
                            Text(
                                text = "Online",
                                color = PitchMint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianDeep
                )
            )
        },
        bottomBar = {
            KaraoqBottomDock(
                currentTab = uiState.currentTab,
                onTabSelected = { viewModel.switchTab(it) }
            )
        }
    ) { innerPadding ->
        when (uiState.currentTab) {
            NavigationTab.CREATE -> {
                StudioTabContent(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onPickAudio = { audioPickerLauncher.launch("audio/*") },
                    onArtistChange = { viewModel.updateArtistInput(it) },
                    onTitleChange = { viewModel.updateTitleInput(it) },
                    onSearchLyrics = { viewModel.searchLyrics() },
                    onStartSeparation = { viewModel.startSeparation() },
                    onTranscribeLyricsWithAi = { viewModel.transcribeWithAi() },
                    onEnterKaraokeStage = { viewModel.enterStageFromCurrentTrack() },
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onSwitchStem = { viewModel.switchStem(it) },
                    onSeek = { viewModel.seekTo(it) }
                )
            }

            NavigationTab.LIBRARY -> {
                SavedSongsScreen(
                    modifier = Modifier.padding(innerPadding),
                    savedSongs = uiState.savedSongs,
                    isLoading = uiState.isLoadingSavedSongs,
                    onRefresh = { viewModel.loadSavedSongs() },
                    onPlaySong = { viewModel.playSavedSong(it) },
                    onDeleteSong = { viewModel.deleteSavedSong(it) }
                )
            }

            NavigationTab.LEADERBOARD -> {
                LeaderboardScreen(
                    modifier = Modifier.padding(innerPadding),
                    savedSongs = uiState.savedSongs,
                    leaderboardEntries = uiState.leaderboardEntries,
                    isLoading = uiState.isLoadingLeaderboard,
                    onSelectSong = { viewModel.loadLeaderboard(it) },
                    onRefresh = {
                        val songId = uiState.activeKaraokeSong?.id
                            ?: uiState.savedSongs.firstOrNull()?.id
                        if (songId != null) {
                            viewModel.loadLeaderboard(songId)
                        }
                    }
                )
            }

            NavigationTab.SETTINGS -> {
                SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    currentBackendUrl = uiState.backendUrl,
                    onSaveBackendUrl = { viewModel.updateBackendUrl(it) }
                )
            }
        }
    }
}

@Composable
private fun StudioTabContent(
    uiState: HomeUiState,
    innerPadding: PaddingValues,
    onPickAudio: () -> Unit,
    onArtistChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onSearchLyrics: () -> Unit,
    onStartSeparation: () -> Unit,
    onTranscribeLyricsWithAi: () -> Unit,
    onEnterKaraokeStage: () -> Unit,
    onTogglePlay: () -> Unit,
    onSwitchStem: (StemType) -> Unit,
    onSeek: (Long) -> Unit
) {
    val isSeparating = uiState.separationState is SeparationUiState.Uploading ||
            uiState.separationState is SeparationUiState.Processing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Visual com Onda Sonora Animada 2D
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estúdio de Separação",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite
                        )
                        Text(
                            text = if (isSeparating) "Processando faixas com Demucs IA..."
                            else if (uiState.isPlaying) "Reproduzindo áudio isolado"
                            else "Pronto para processar sua faixa",
                            style = MaterialTheme.typography.bodySmall,
                            color = FlameOrange,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FlameOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = FlameOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Visualizador de Ondas inspirado na logo KaraoQ
                WaveformVisualizer(
                    modifier = Modifier.fillMaxWidth(),
                    height = 54.dp,
                    isProcessing = isSeparating || uiState.isPlaying,
                    primaryColor = FlameOrange,
                    secondaryColor = SunsetCoral
                )
            }
        }

        // Card 1: Seleção de Arquivo e Dados da Música
        AudioAndLyricsSelectorCard(
            selectedFileName = uiState.selectedFileName,
            artist = uiState.artistInput,
            title = uiState.titleInput,
            lyrics = uiState.lyrics,
            isSearchingLyrics = uiState.isSearchingLyrics,
            isProcessing = isSeparating,
            onPickAudio = onPickAudio,
            onArtistChange = onArtistChange,
            onTitleChange = onTitleChange,
            onSearchLyrics = onSearchLyrics,
            onStartSeparation = onStartSeparation
        )

        // Card 2: Progresso e Status em Tempo Real
        if (uiState.separationState !is SeparationUiState.Idle) {
            StatusProgressCard(
                separationState = uiState.separationState,
                errorMessage = uiState.errorMessage
            )
        }

        // Card 3: Player de Karaokê com Letra Sincronizada
        if (uiState.separationState is SeparationUiState.Ready) {
            val readyState = uiState.separationState
            KaraokePlayerCard(
                trackTitle = readyState.track.title,
                activeStem = uiState.activeStem,
                isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                lyrics = uiState.lyrics,
                canTranscribeAi = uiState.taskStatus?.taskId != null,
                isTranscribingLyrics = uiState.isTranscribingLyrics,
                transcriptionProgress = uiState.transcriptionProgress,
                partialTranscribedVerse = uiState.partialTranscribedVerse,
                onTranscribeLyricsWithAi = onTranscribeLyricsWithAi,
                onEnterKaraokeStage = onEnterKaraokeStage,
                onTogglePlay = onTogglePlay,
                onSwitchStem = onSwitchStem,
                onSeek = onSeek
            )
        }

        // Dica informativa
        UsbDebuggingHintCard(backendUrl = uiState.backendUrl)
    }
}

@Composable
fun AudioAndLyricsSelectorCard(
    selectedFileName: String?,
    artist: String,
    title: String,
    lyrics: SongLyrics?,
    isSearchingLyrics: Boolean,
    isProcessing: Boolean,
    onPickAudio: () -> Unit,
    onArtistChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onSearchLyrics: () -> Unit,
    onStartSeparation: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = FlameOrange,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "1. Escolha a Música & Letra",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite
                )
            }

            // Botão de seleção de arquivo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardElevated)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(FlameOrange.copy(alpha = 0.5f), SunsetCoral.copy(alpha = 0.3f))),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = !isProcessing) { onPickAudio() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(FlameOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedFileName != null) Icons.Rounded.Audiotrack else Icons.Rounded.UploadFile,
                            contentDescription = null,
                            tint = FlameOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedFileName ?: "Clique para selecionar arquivo de áudio",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedFileName != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFileName != null) TextPureWhite else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Formatos suportados: .mp3, .wav, .m4a",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Campos de Artista e Título da Música
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = artist,
                    onValueChange = onArtistChange,
                    label = { Text("Cantor / Artista") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Person, contentDescription = null, tint = FlameOrange, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlameOrange,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPureWhite,
                        unfocusedTextColor = TextPureWhite,
                        focusedLabelColor = FlameOrange,
                        unfocusedLabelColor = TextMuted
                    )
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Nome da Música") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.MusicNote, contentDescription = null, tint = SunsetCoral, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SunsetCoral,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPureWhite,
                        unfocusedTextColor = TextPureWhite,
                        focusedLabelColor = SunsetCoral,
                        unfocusedLabelColor = TextMuted
                    )
                )
            }

            // Botão de Busca de Letra
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onSearchLyrics,
                    enabled = artist.isNotBlank() && title.isNotBlank() && !isSearchingLyrics,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardElevated,
                        contentColor = FlameOrange,
                        disabledContainerColor = CardDarkSurface,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                ) {
                    if (isSearchingLyrics) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = FlameOrange, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buscando...")
                    } else {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buscar Letra", fontWeight = FontWeight.Bold)
                    }
                }

                // Status da Letra
                if (lyrics != null && lyrics.lines.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = PitchMint, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${lyrics.lines.size} versos sincronizados",
                            style = MaterialTheme.typography.bodySmall,
                            color = PitchMint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Botão Iniciar Separação
            KaraoqPillButton(
                text = if (isProcessing) "Processando Separação..." else "Separar Áudio & Iniciar Karaokê",
                icon = Icons.Rounded.GraphicEq,
                enabled = selectedFileName != null && !isProcessing,
                onClick = onStartSeparation,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StatusProgressCard(
    separationState: SeparationUiState,
    errorMessage: String?
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "2. Status do Processamento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite
                )

                when (separationState) {
                    is SeparationUiState.Processing, is SeparationUiState.Uploading -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(FlameOrange)
                        )
                    }
                    is SeparationUiState.Ready -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = PitchMint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is SeparationUiState.Error -> {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> Unit
                }
            }

            val progressValue = when (separationState) {
                is SeparationUiState.Uploading -> separationState.progress.toFloat() / 100f
                is SeparationUiState.Processing -> separationState.progress.toFloat() / 100f
                is SeparationUiState.Ready -> 1.0f
                else -> 0f
            }

            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when (separationState) {
                    is SeparationUiState.Ready -> PitchMint
                    is SeparationUiState.Error -> AlertRed
                    else -> FlameOrange
                },
                trackColor = CardElevated
            )

            val statusMessage = when (separationState) {
                is SeparationUiState.Uploading -> "Enviando arquivo de áudio para o servidor..."
                is SeparationUiState.Processing -> separationState.message
                is SeparationUiState.Ready -> "Isolamento concluído com sucesso! Stems prontas."
                is SeparationUiState.Error -> errorMessage ?: separationState.message
                else -> ""
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (separationState is SeparationUiState.Error) AlertRed else TextSecondary
            )
        }
    }
}

@Composable
fun KaraokePlayerCard(
    trackTitle: String,
    activeStem: StemType,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    lyrics: SongLyrics?,
    canTranscribeAi: Boolean = false,
    isTranscribingLyrics: Boolean = false,
    transcriptionProgress: Float = 0f,
    partialTranscribedVerse: String = "",
    onTranscribeLyricsWithAi: () -> Unit = {},
    onEnterKaraokeStage: () -> Unit = {},
    onTogglePlay: () -> Unit,
    onSwitchStem: (StemType) -> Unit,
    onSeek: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Bloco de Controle de Reprodução
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Modo Player",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FlameOrange
                    )

                    KaraoqPillButton(
                        text = "Ir para o Palco",
                        icon = Icons.Rounded.MicExternalOn,
                        onClick = onEnterKaraokeStage
                    )
                }

                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Seletor de Faixa (Stem Switcher) - Zero emojis
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StemSelectorButton(
                        modifier = Modifier.weight(1f),
                        title = "Instrumental",
                        subtitle = "Karaokê",
                        icon = Icons.Rounded.GraphicEq,
                        isSelected = activeStem == StemType.INSTRUMENTAL,
                        selectedColor = FlameOrange,
                        onClick = { onSwitchStem(StemType.INSTRUMENTAL) }
                    )

                    StemSelectorButton(
                        modifier = Modifier.weight(1f),
                        title = "Apenas Voz",
                        subtitle = "Acapella",
                        icon = Icons.Rounded.Mic,
                        isSelected = activeStem == StemType.VOCALS,
                        selectedColor = SunsetCoral,
                        onClick = { onSwitchStem(StemType.VOCALS) }
                    )
                }

                // Seekbar
                Column {
                    val sliderValue = if (durationMs > 0) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = sliderValue,
                        onValueChange = { frac ->
                            if (durationMs > 0) {
                                onSeek((frac * durationMs).toLong())
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = FlameOrange,
                            activeTrackColor = FlameOrange,
                            inactiveTrackColor = CardElevated
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = formatTime(durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Controle Play / Pause
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
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
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }

        // Bloco de Letra Sincronizada (Teleprompter em Tempo Real)
        if (lyrics != null) {
            LyricsView(
                lyrics = lyrics,
                currentPositionMs = currentPositionMs,
                isStageMode = false,
                onSeek = onSeek
            )
        } else if (canTranscribeAi) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null, tint = SunsetCoral)
                        Text(
                            text = "Sem letra disponível para sincronizar",
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = "Use o Whisper (IA Local) para transcrever a voz isolada e gerar os versos sincronizados automaticamente com streaming WebSocket.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    if (isTranscribingLyrics) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { (transcriptionProgress / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SunsetCoral,
                                trackColor = CardElevated
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (partialTranscribedVerse.isNotBlank()) partialTranscribedVerse else "Transcrevendo voz com Whisper...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FlameOrange,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${transcriptionProgress.toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SunsetCoral,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    KaraoqPillButton(
                        text = if (isTranscribingLyrics) "Transcrevendo com Whisper (${transcriptionProgress.toInt()}%)..." else "Transcrever Letra com Whisper IA",
                        icon = Icons.Rounded.AutoAwesome,
                        enabled = !isTranscribingLyrics,
                        onClick = onTranscribeLyricsWithAi,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun StemSelectorButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) selectedColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) selectedColor else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) selectedColor else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextPureWhite else TextSecondary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = if (isSelected) selectedColor else TextMuted
                )
            }
        }
    }
}

@Composable
fun UsbDebuggingHintCard(backendUrl: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = FlameOrange,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Dica de KaraoQ",
                    style = MaterialTheme.typography.labelLarge,
                    color = FlameOrange,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Servidor ativo: $backendUrl\n\nAo reproduzir a música, a letra sincronizada rola automaticamente e destaca o verso em tempo real. Toque em qualquer frase para saltar o áudio para aquele instante.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

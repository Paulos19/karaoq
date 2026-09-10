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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karaoq.app.domain.model.NavigationTab
import com.karaoq.app.domain.model.SeparationUiState
import com.karaoq.app.domain.model.SongLyrics
import com.karaoq.app.domain.model.StemType
import com.karaoq.app.presentation.components.LyricsView
import com.karaoq.app.presentation.library.SavedSongsScreen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(NeonCyan, NeonPink)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = DarkBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "KaraoQ",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "AI Stem & Lyrics Sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettingsDialog = !showSettingsDialog }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configurações do Servidor",
                                tint = if (showSettingsDialog) NeonCyan else TextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface
                    )
                )

                // Barra de Abas de Navegação (Criar Karaokê vs Músicas Salvas)
                TabRow(
                    selectedTabIndex = uiState.currentTab.ordinal,
                    containerColor = DarkSurface,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab.ordinal]),
                            color = NeonCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = uiState.currentTab == NavigationTab.CREATE,
                        onClick = { viewModel.switchTab(NavigationTab.CREATE) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Criar Karaokê", fontWeight = FontWeight.Bold)
                            }
                        },
                        selectedContentColor = NeonCyan,
                        unselectedContentColor = TextSecondary
                    )

                    Tab(
                        selected = uiState.currentTab == NavigationTab.LIBRARY,
                        onClick = { viewModel.switchTab(NavigationTab.LIBRARY) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (uiState.savedSongs.isNotEmpty()) "Salvas (${uiState.savedSongs.size})" else "Músicas Salvas",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        selectedContentColor = NeonCyan,
                        unselectedContentColor = TextSecondary
                    )
                }
            }
        }
    ) { innerPadding ->
        if (uiState.currentTab == NavigationTab.LIBRARY) {
            SavedSongsScreen(
                modifier = Modifier.padding(innerPadding),
                savedSongs = uiState.savedSongs,
                isLoading = uiState.isLoadingSavedSongs,
                onRefresh = { viewModel.loadSavedSongs() },
                onPlaySong = { viewModel.playSavedSong(it) },
                onDeleteSong = { viewModel.deleteSavedSong(it) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Seção de Configuração do Backend
                AnimatedVisibility(visible = showSettingsDialog) {
                    BackendSettingsCard(
                        currentUrl = uiState.backendUrl,
                        onSaveUrl = {
                            viewModel.updateBackendUrl(it)
                            showSettingsDialog = false
                        }
                    )
                }

                // Card 1: Seleção de Arquivo e Dados da Música (Artista + Título)
                AudioAndLyricsSelectorCard(
                    selectedFileName = uiState.selectedFileName,
                    artist = uiState.artistInput,
                    title = uiState.titleInput,
                    lyrics = uiState.lyrics,
                    isSearchingLyrics = uiState.isSearchingLyrics,
                    isProcessing = uiState.separationState is SeparationUiState.Uploading ||
                            uiState.separationState is SeparationUiState.Processing,
                    onPickAudio = { audioPickerLauncher.launch("audio/*") },
                    onArtistChange = { viewModel.updateArtistInput(it) },
                    onTitleChange = { viewModel.updateTitleInput(it) },
                    onSearchLyrics = { viewModel.searchLyrics() },
                    onStartSeparation = { viewModel.startSeparation() }
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
                    val readyState = uiState.separationState as SeparationUiState.Ready
                    KaraokePlayerCard(
                        trackTitle = readyState.track.title,
                        activeStem = uiState.activeStem,
                        isPlaying = uiState.isPlaying,
                        currentPositionMs = uiState.currentPositionMs,
                        durationMs = uiState.durationMs,
                        lyrics = uiState.lyrics,
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onSwitchStem = { viewModel.switchStem(it) },
                        onSeek = { viewModel.seekTo(it) }
                    )
                }

                // Dica informativa
                UsbDebuggingHintCard(backendUrl = uiState.backendUrl)
            }
        }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "1. Escolha a Música & Letra",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Botão de seleção de arquivo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.5f), NeonPink.copy(alpha = 0.5f))),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = !isProcessing) { onPickAudio() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (selectedFileName != null) Icons.Default.Audiotrack else Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = if (selectedFileName != null) NeonCyan else TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedFileName ?: "Clique para selecionar arquivo de áudio",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedFileName != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFileName != null) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Formatos suportados: .mp3, .wav, .m4a",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Campos de Artista e Título da Música
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = artist,
                    onValueChange = onArtistChange,
                    label = { Text("Cantor / Artista") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Nome da Música") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = NeonPink, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPink,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
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
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = NeonCyan,
                        disabledContainerColor = DarkSurfaceBorder,
                        disabledContentColor = TextSecondary
                    )
                ) {
                    if (isSearchingLyrics) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buscando...")
                    } else {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
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
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${lyrics.lines.size} versos sincronizados",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Botão Iniciar Separação
            Button(
                onClick = onStartSeparation,
                enabled = selectedFileName != null && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceBorder,
                    disabledContentColor = TextSecondary
                )
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isProcessing) "Processando Separação..." else "Separar Áudio & Iniciar Karaokê",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusProgressCard(
    separationState: SeparationUiState,
    errorMessage: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
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
                    color = TextPrimary
                )

                when (separationState) {
                    is SeparationUiState.Processing, is SeparationUiState.Uploading -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.2f,
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
                                .background(NeonCyan)
                        )
                    }
                    is SeparationUiState.Ready -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is SeparationUiState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Error,
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
                    is SeparationUiState.Ready -> ElectricGreen
                    is SeparationUiState.Error -> AlertRed
                    else -> NeonCyan
                },
                trackColor = DarkSurfaceBorder
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
    onTogglePlay: () -> Unit,
    onSwitchStem: (StemType) -> Unit,
    onSeek: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Bloco de Controle de Reprodução
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan, NeonPink)),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "3. Modo Karaokê Player",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Seletor de Faixa (Stem Switcher)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StemSelectorButton(
                        modifier = Modifier.weight(1f),
                        title = "🎸 Instrumental",
                        subtitle = "Karaokê",
                        isSelected = activeStem == StemType.INSTRUMENTAL,
                        selectedColor = NeonPink,
                        onClick = { onSwitchStem(StemType.INSTRUMENTAL) }
                    )

                    StemSelectorButton(
                        modifier = Modifier.weight(1f),
                        title = "🎤 Apenas Voz",
                        subtitle = "Acapella",
                        isSelected = activeStem == StemType.VOCALS,
                        selectedColor = NeonCyan,
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
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceBorder
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
                                Brush.linearGradient(listOf(NeonCyan, NeonPink))
                            )
                            .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = DarkBackground,
                            modifier = Modifier.size(36.dp)
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
                onSeek = onSeek
            )
        }
    }
}

@Composable
fun StemSelectorButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextSecondary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = if (isSelected) selectedColor else TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun BackendSettingsCard(
    currentUrl: String,
    onSaveUrl: (String) -> Unit
) {
    var textValue by remember(currentUrl) { mutableStateOf(currentUrl) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Endereço da API do Backend",
                style = MaterialTheme.typography.titleSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Button(
                onClick = { onSaveUrl(textValue) },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
            ) {
                Text("Salvar Endereço", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UsbDebuggingHintCard(backendUrl: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "💡 Dica de KaraoQ",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan
            )
            Text(
                text = "Servidor ativo: $backendUrl\n\nAo reproduzir a música, a letra sincronizada rola automaticamente e destaca o verso em tempo real. Você também pode tocar em qualquer frase para pular o áudio para aquele instante!",
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

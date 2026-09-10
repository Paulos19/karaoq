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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
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
import com.karaoq.app.domain.model.SeparationUiState
import com.karaoq.app.domain.model.StemType
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
                                text = "AI Stem Isolation Engine",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seção de Configuração do Backend (Expansível)
            AnimatedVisibility(visible = showSettingsDialog) {
                BackendSettingsCard(
                    currentUrl = uiState.backendUrl,
                    onSaveUrl = {
                        viewModel.updateBackendUrl(it)
                        showSettingsDialog = false
                    }
                )
            }

            // Card 1: Seleção de Arquivo de Áudio
            AudioSelectorCard(
                selectedFileName = uiState.selectedFileName,
                isProcessing = uiState.separationState is SeparationUiState.Uploading ||
                        uiState.separationState is SeparationUiState.Processing,
                onPickAudio = { audioPickerLauncher.launch("audio/*") },
                onStartSeparation = { viewModel.startSeparation() }
            )

            // Card 2: Progresso e Status em Tempo Real
            if (uiState.separationState !is SeparationUiState.Idle) {
                StatusProgressCard(
                    separationState = uiState.separationState,
                    errorMessage = uiState.errorMessage
                )
            }

            // Card 3: Player de Karaokê (Quando pronto)
            if (uiState.separationState is SeparationUiState.Ready) {
                val readyState = uiState.separationState as SeparationUiState.Ready
                KaraokePlayerCard(
                    trackTitle = readyState.track.title,
                    activeStem = uiState.activeStem,
                    isPlaying = uiState.isPlaying,
                    currentPositionMs = uiState.currentPositionMs,
                    durationMs = uiState.durationMs,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onSwitchStem = { viewModel.switchStem(it) },
                    onSeek = { viewModel.seekTo(it) }
                )
            }

            // Dica de Depuração USB
            UsbDebuggingHintCard(backendUrl = uiState.backendUrl)
        }
    }
}

@Composable
fun AudioSelectorCard(
    selectedFileName: String?,
    isProcessing: Boolean,
    onPickAudio: () -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "1. Escolha sua Música",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

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
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (selectedFileName != null) Icons.Default.Audiotrack else Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = if (selectedFileName != null) NeonCyan else TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = selectedFileName ?: "Clique para selecionar música (.mp3, .wav, .m4a)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedFileName != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedFileName != null) TextPrimary else TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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
                    text = if (isProcessing) "Processando com IA..." else "Separar Voz e Playback",
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
    onTogglePlay: () -> Unit,
    onSwitchStem: (StemType) -> Unit,
    onSeek: (Long) -> Unit
) {
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
                // Botão Instrumental (Karaokê)
                StemSelectorButton(
                    modifier = Modifier.weight(1f),
                    title = "🎸 Instrumental",
                    subtitle = "Karaokê",
                    isSelected = activeStem == StemType.INSTRUMENTAL,
                    selectedColor = NeonPink,
                    onClick = { onSwitchStem(StemType.INSTRUMENTAL) }
                )

                // Botão Vocal (Apenas Voz)
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
            Text(
                text = "Para USB Debugging, use http://localhost:8000/ junto com 'adb reverse tcp:8000 tcp:8000'. Para VPS, digite a URL pública.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
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
                text = "💡 Dica de Depuração USB",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan
            )
            Text(
                text = "Servidor configurado: $backendUrl\n\nAo conectar o celular no USB, execute:\nadb reverse tcp:8000 tcp:8000\nIsso permite que o app acesse o backend local direto por 'http://localhost:8000/'!",
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

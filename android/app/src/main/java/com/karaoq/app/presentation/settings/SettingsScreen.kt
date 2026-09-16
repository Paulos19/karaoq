package com.karaoq.app.presentation.settings

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
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoq.app.R
import com.karaoq.app.presentation.components.GlassCard
import com.karaoq.app.presentation.components.KaraoqPillButton
import com.karaoq.app.presentation.ui.theme.CardDarkSurface
import com.karaoq.app.presentation.ui.theme.CardElevated
import com.karaoq.app.presentation.ui.theme.FlameOrange
import com.karaoq.app.presentation.ui.theme.FlameOrangeLight
import com.karaoq.app.presentation.ui.theme.GlassBorder
import com.karaoq.app.presentation.ui.theme.PitchMint
import com.karaoq.app.presentation.ui.theme.SunsetCoral
import com.karaoq.app.presentation.ui.theme.TextMuted
import com.karaoq.app.presentation.ui.theme.TextPureWhite
import com.karaoq.app.presentation.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    currentBackendUrl: String,
    onSaveBackendUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var urlInput by remember(currentBackendUrl) { mutableStateOf(currentBackendUrl) }
    var saveFeedback by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho da Tela
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(listOf(FlameOrange, SunsetCoral))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = TextPureWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Ajustes do Sistema",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPureWhite
                )
                Text(
                    text = "Conectividade, motor de IA e preferências",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlameOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Card 1: Configuração do Backend
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = FlameOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Servidor da API (Backend)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite
                    )
                }

                Text(
                    text = "Informe o endereço do backend FastAPI executando com Demucs e Whisper.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        saveFeedback = false
                    },
                    label = { Text("URL Base da API") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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

                // Atalhos rápidos de IPs comuns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickHostChip(label = "Local (10.0.2.2)", url = "http://10.0.2.2:8000") {
                        urlInput = it
                    }
                    QuickHostChip(label = "Host PC (127.0.0.1)", url = "http://127.0.0.1:8000") {
                        urlInput = it
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (saveFeedback) {
                        Text(
                            text = "Endereço atualizado!",
                            color = PitchMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    KaraoqPillButton(
                        text = "Salvar URL",
                        icon = Icons.Rounded.Save,
                        onClick = {
                            onSaveBackendUrl(urlInput.trim())
                            saveFeedback = true
                        }
                    )
                }
            }
        }

        // Card 2: Status do Motor de Áudio
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = PitchMint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Mecanismo de Áudio & IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite
                    )
                }

                EngineInfoRow(title = "Separação de Stems", value = "Demucs v4 (Hybrid Transformer)")
                EngineInfoRow(title = "Sincronização de Letras", value = "Whisper IA (Streaming WebSocket)")
                EngineInfoRow(title = "Detecção de Pitch", value = "TarsosDSP YIN Pitch Detector")
                EngineInfoRow(title = "Reprodução", value = "Android ExoPlayer 2-Channel")
            }
        }

        // Card 3: Sobre o KaraoQ & Marca
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_karaoq_logo),
                    contentDescription = "KaraoQ Logo",
                    modifier = Modifier.size(60.dp)
                )

                Text(
                    text = "KaraoQ Studio",
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    fontSize = 16.sp
                )

                Text(
                    text = "Versão 1.0.0 • Professional Karaoke Ecosystem",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun QuickHostChip(
    label: String,
    url: String,
    onSelect: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardElevated)
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
            .clickable { onSelect(url) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EngineInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = TextPureWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

package com.karaoq.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoq.app.domain.model.SongLyrics
import com.karaoq.app.presentation.ui.theme.DarkBackground
import com.karaoq.app.presentation.ui.theme.DarkSurface
import com.karaoq.app.presentation.ui.theme.DarkSurfaceBorder
import com.karaoq.app.presentation.ui.theme.ElectricGreen
import com.karaoq.app.presentation.ui.theme.NeonCyan
import com.karaoq.app.presentation.ui.theme.NeonPink
import com.karaoq.app.presentation.ui.theme.TextPrimary
import com.karaoq.app.presentation.ui.theme.TextSecondary

@Composable
fun LyricsView(
    modifier: Modifier = Modifier,
    lyrics: SongLyrics,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState()

    // Encontra o índice da linha ativa no momento
    val activeLineIndex by remember(lyrics.lines, currentPositionMs) {
        derivedStateOf {
            val lines = lyrics.lines
            if (lines.isEmpty()) return@derivedStateOf -1

            // Procura a última linha cujo timeMs <= currentPositionMs
            var activeIdx = -1
            for (i in lines.indices) {
                if (currentPositionMs >= lines[i].timeMs) {
                    activeIdx = i
                } else {
                    break
                }
            }
            activeIdx
        }
    }

    // Auto-scroll suave para manter a frase atual visível e centralizada
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && activeLineIndex < lyrics.lines.size) {
            listState.animateScrollToItem(
                index = activeLineIndex,
                scrollOffset = -200
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(NeonCyan.copy(alpha = 0.6f), NeonPink.copy(alpha = 0.4f))),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabeçalho da Letra com Badge de Sincronização
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Letra de Karaokê",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Badge de status da sincronização
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (lyrics.isSynced) ElectricGreen.copy(alpha = 0.15f)
                            else NeonPink.copy(alpha = 0.15f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (lyrics.isSynced) ElectricGreen else NeonPink,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (lyrics.isSynced) "● Sincronizada (LRC)" else "● Auto-Scroll",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (lyrics.isSynced) ElectricGreen else NeonPink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (lyrics.lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lyrics.plainText.isNotBlank()) lyrics.plainText
                        else "Nenhuma letra encontrada para esta música.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Lista rolável de frases sincronizadas
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 340.dp),
                    contentPadding = PaddingValues(vertical = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        val isActive = index == activeLineIndex

                        val textColor by animateColorAsState(
                            targetValue = if (isActive) NeonCyan else TextSecondary.copy(alpha = 0.45f),
                            animationSpec = tween(300),
                            label = "lyricsColor"
                        )

                        val fontSize = if (isActive) 19.sp else 15.sp
                        val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) NeonCyan.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { onSeek(line.timeMs) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = line.text.ifBlank { "♪ ♪ ♪" },
                                color = textColor,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )

                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonPink)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

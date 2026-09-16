package com.karaoq.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoq.app.domain.model.SongLyrics
import com.karaoq.app.presentation.ui.theme.FlameOrange
import com.karaoq.app.presentation.ui.theme.ObsidianDeep
import com.karaoq.app.presentation.ui.theme.PitchMint
import com.karaoq.app.presentation.ui.theme.SunsetCoral
import com.karaoq.app.presentation.ui.theme.TextMuted
import com.karaoq.app.presentation.ui.theme.TextPureWhite
import com.karaoq.app.presentation.ui.theme.TextSecondary

@Composable
fun LyricsView(
    lyrics: SongLyrics,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
    isStageMode: Boolean = false,
    onSeek: (Long) -> Unit = {}
) {
    val listState = rememberLazyListState()

    // Encontra o índice da linha ativa no momento
    val activeLineIndex by remember(lyrics.lines, currentPositionMs) {
        derivedStateOf {
            val lines = lyrics.lines
            if (lines.isEmpty()) return@derivedStateOf -1

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
            val offset = if (isStageMode) -260 else -180
            listState.animateScrollToItem(
                index = activeLineIndex,
                scrollOffset = offset
            )
        }
    }

    if (isStageMode) {
        // Modo Palco (Inspirado no Gemini: totalmente imersivo, fluido e sem bordas duras)
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            if (lyrics.lines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lyrics.plainText.isNotBlank()) lyrics.plainText else "Solo instrumental...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 70.dp, bottom = 120.dp, start = 20.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        val isActive = index == activeLineIndex

                        val textColor by animateColorAsState(
                            targetValue = if (isActive) TextPureWhite else TextPureWhite.copy(alpha = 0.30f),
                            animationSpec = tween(300),
                            label = "stageTextColor"
                        )

                        val textScale by animateFloatAsState(
                            targetValue = if (isActive) 1.03f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "stageTextScale"
                        )

                        val fontSize = if (isActive) 23.sp else 18.sp
                        val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(textScale)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSeek(line.timeMs) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ponto de pulso ao lado da linha ativa
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(FlameOrange)
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(10.dp))
                            }

                            Text(
                                text = line.text.ifBlank { "(Instrumental)" },
                                color = textColor,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                textAlign = TextAlign.Start,
                                lineHeight = (fontSize.value * 1.35f).sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Gradientes sutis superior e inferior para efeito fade estilo Apple Music / Gemini
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(ObsidianDeep, Color.Transparent)
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianDeep)
                            )
                        )
                )
            }
        }
    } else {
        // Modo Estúdio (Card Glassmorphic contido)
        GlassCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabeçalho da Letra
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
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = FlameOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Letra Sincronizada",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPureWhite
                        )
                    }

                    // Badge de sincronização
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PitchMint.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (lyrics.isSynced) "Sincronizada" else "Auto-Scroll",
                            style = MaterialTheme.typography.labelSmall,
                            color = PitchMint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (lyrics.lines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lyrics.plainText.isNotBlank()) lyrics.plainText
                            else "Nenhuma letra disponível.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(lyrics.lines) { index, line ->
                            val isActive = index == activeLineIndex

                            val textColor by animateColorAsState(
                                targetValue = if (isActive) FlameOrange else TextSecondary.copy(alpha = 0.5f),
                                animationSpec = tween(300),
                                label = "studioTextColor"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSeek(line.timeMs) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = line.text.ifBlank { "(Instrumental)" },
                                    color = textColor,
                                    fontSize = if (isActive) 16.sp else 14.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

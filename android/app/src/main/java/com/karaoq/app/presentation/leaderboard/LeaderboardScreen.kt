package com.karaoq.app.presentation.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoq.app.data.model.LeaderboardEntry
import com.karaoq.app.domain.model.SavedSong
import com.karaoq.app.presentation.components.GlassCard
import com.karaoq.app.presentation.components.KaraoqMedalBadge
import com.karaoq.app.presentation.ui.theme.AmberGlow
import com.karaoq.app.presentation.ui.theme.CardDarkSurface
import com.karaoq.app.presentation.ui.theme.CardElevated
import com.karaoq.app.presentation.ui.theme.FlameOrange
import com.karaoq.app.presentation.ui.theme.FlameOrangeLight
import com.karaoq.app.presentation.ui.theme.GlassBorder
import com.karaoq.app.presentation.ui.theme.GoldMedal
import com.karaoq.app.presentation.ui.theme.PitchMint
import com.karaoq.app.presentation.ui.theme.SunsetCoral
import com.karaoq.app.presentation.ui.theme.TextMuted
import com.karaoq.app.presentation.ui.theme.TextPureWhite
import com.karaoq.app.presentation.ui.theme.TextSecondary

@Composable
fun LeaderboardScreen(
    savedSongs: List<SavedSong>,
    leaderboardEntries: List<LeaderboardEntry>,
    isLoading: Boolean,
    onSelectSong: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSongId by remember(savedSongs) {
        mutableStateOf(savedSongs.firstOrNull()?.id.orEmpty())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho da Tela
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(AmberGlow, FlameOrange))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = TextPureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Ranking Global",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPureWhite
                    )
                    Text(
                        text = "Top performances no KaraoQ",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CardDarkSurface)
                    .border(1.dp, GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Atualizar",
                    tint = AmberGlow,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Seletor horizontal de músicas da biblioteca
        if (savedSongs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedSongs.forEach { song ->
                    val isSelected = song.id == selectedSongId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Brush.horizontalGradient(listOf(FlameOrange, SunsetCoral))
                                else Brush.horizontalGradient(listOf(CardElevated, CardDarkSurface))
                            )
                            .border(
                                1.dp,
                                if (isSelected) FlameOrangeLight else GlassBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedSongId = song.id
                                onSelectSong(song.id)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = if (isSelected) TextPureWhite else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = song.title,
                                color = if (isSelected) TextPureWhite else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmberGlow, strokeWidth = 3.dp)
            }
        } else if (leaderboardEntries.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(AmberGlow.copy(alpha = 0.12f))
                            .border(1.dp, AmberGlow.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = AmberGlow,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "Nenhum show registrado ainda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite
                    )

                    Text(
                        text = "Cante esta música no Palco Karaokê e registre sua pontuação no topo do ranking!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            // Pódio Top 3 e Lista Completa
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Pódio Top 3
                if (leaderboardEntries.size >= 1) {
                    item {
                        PodiumView(entries = leaderboardEntries.take(3))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Lista de classificados a partir do 4º lugar (ou lista completa)
                val remainingEntries = if (leaderboardEntries.size > 3) leaderboardEntries.drop(3) else emptyList()
                if (remainingEntries.isNotEmpty()) {
                    item {
                        Text(
                            text = "Outros Cantores",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                }

                itemsIndexed(remainingEntries) { index, entry ->
                    LeaderboardRow(
                        rank = index + 4,
                        entry = entry
                    )
                }
            }
        }
    }
}

/**
 * Pódio 3D moderno destacando 1º, 2º e 3º lugares com o primeiro colocado elevado ao centro.
 */
@Composable
private fun PodiumView(entries: List<LeaderboardEntry>) {
    val first = entries.getOrNull(0)
    val second = entries.getOrNull(1)
    val third = entries.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2º Lugar (Esquerda)
        if (second != null) {
            PodiumPillar(
                modifier = Modifier.weight(1f),
                rank = 2,
                entry = second,
                pillarHeight = 110.dp
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // 1º Lugar (Centro - Mais alto e destacado)
        if (first != null) {
            PodiumPillar(
                modifier = Modifier.weight(1.15f),
                rank = 1,
                entry = first,
                pillarHeight = 145.dp
            )
        }

        // 3º Lugar (Direita)
        if (third != null) {
            PodiumPillar(
                modifier = Modifier.weight(1f),
                rank = 3,
                entry = third,
                pillarHeight = 90.dp
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumPillar(
    modifier: Modifier = Modifier,
    rank: Int,
    entry: LeaderboardEntry,
    pillarHeight: androidx.compose.ui.unit.Dp
) {
    val highlightColor = when (rank) {
        1 -> GoldMedal
        2 -> Color(0xFFE2E8F0)
        else -> Color(0xFFCD7F32)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        KaraoqMedalBadge(rank = rank, size = if (rank == 1) 32.dp else 26.dp)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = entry.singerName,
            color = TextPureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = if (rank == 1) 13.sp else 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${entry.score} pts",
            color = highlightColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (rank == 1) 12.sp else 10.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Pilar 3D
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pillarHeight)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            highlightColor.copy(alpha = if (rank == 1) 0.35f else 0.2f),
                            CardDarkSurface
                        )
                    )
                )
                .border(
                    1.dp,
                    highlightColor.copy(alpha = if (rank == 1) 0.6f else 0.3f),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${rank}º",
                color = highlightColor,
                fontWeight = FontWeight.Black,
                fontSize = if (rank == 1) 22.sp else 18.sp
            )
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    entry: LeaderboardEntry
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KaraoqMedalBadge(rank = rank, size = 20.dp)

                Column {
                    Text(
                        text = entry.singerName,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${entry.rank} • Combo ${entry.maxCombo}x",
                        color = PitchMint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

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
                    text = "${entry.score} pts",
                    fontWeight = FontWeight.ExtraBold,
                    color = AmberGlow,
                    fontSize = 13.sp
                )
            }
        }
    }
}

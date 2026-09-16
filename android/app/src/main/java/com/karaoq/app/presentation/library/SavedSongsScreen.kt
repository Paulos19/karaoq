package com.karaoq.app.presentation.library

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.karaoq.app.domain.model.SavedSong
import com.karaoq.app.presentation.components.GlassCard
import com.karaoq.app.presentation.ui.theme.AlertRed
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

@Composable
fun SavedSongsScreen(
    modifier: Modifier = Modifier,
    savedSongs: List<SavedSong>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onPlaySong: (SavedSong) -> Unit,
    onDeleteSong: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho da Biblioteca
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
                            Brush.linearGradient(listOf(FlameOrange, SunsetCoral))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = TextPureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Biblioteca",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPureWhite
                    )
                    Text(
                        text = "${savedSongs.size} faixas sincronizadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = FlameOrange,
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
                    contentDescription = "Atualizar lista",
                    tint = FlameOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FlameOrange, strokeWidth = 3.dp)
            }
        } else if (savedSongs.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(FlameOrange.copy(alpha = 0.12f))
                            .border(1.dp, FlameOrange.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = FlameOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Nenhuma música salva ainda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPureWhite
                    )

                    Text(
                        text = "Separe uma música na aba Estúdio para isolar os stems e salvar na sua biblioteca permanente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(savedSongs, key = { it.id }) { song ->
                    SavedSongCard(
                        song = song,
                        onPlay = { onPlaySong(song) },
                        onDelete = { onDeleteSong(song.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedSongCard(
    song: SavedSong,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val hasLyrics = song.lyrics?.lines?.isNotEmpty() == true

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlay() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Capa estilizada com gradiente Flame
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(FlameOrange, SunsetCoral))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = TextPureWhite,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Metadados
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist.ifBlank { "Artista Desconhecido" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Chips de status com ícones vetoriais
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (hasLyrics) PitchMint.copy(alpha = 0.15f) else CardElevated
                            )
                            .border(
                                1.dp,
                                if (hasLyrics) PitchMint.copy(alpha = 0.4f) else GlassBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (hasLyrics) Icons.Rounded.Description else Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = if (hasLyrics) PitchMint else TextMuted,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (hasLyrics) "Com Letra" else "Instrumental",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasLyrics) PitchMint else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Ações: Botão Cantar e Excluir
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(listOf(FlameOrange, FlameOrangeLight))
                        )
                        .clickable { onPlay() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Cantar no Palco",
                            tint = TextPureWhite,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Cantar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPureWhite
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Excluir Música",
                        tint = AlertRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

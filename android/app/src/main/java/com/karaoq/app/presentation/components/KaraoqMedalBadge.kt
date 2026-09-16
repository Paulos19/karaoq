package com.karaoq.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoq.app.presentation.ui.theme.BronzeMedal
import com.karaoq.app.presentation.ui.theme.GoldMedal
import com.karaoq.app.presentation.ui.theme.SilverMedal

/**
 * Distintivo vetorial de classificação e medalhas de alta fidelidade.
 */
@Composable
fun KaraoqMedalBadge(
    rank: Int,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val (badgeColors, iconColor) = when (rank) {
        1 -> listOf(GoldMedal, Color(0xFFE6A800)) to Color(0xFF3D2800)
        2 -> listOf(SilverMedal, Color(0xFF94A3B8)) to Color(0xFF1E293B)
        3 -> listOf(BronzeMedal, Color(0xFF9C5116)) to Color(0xFF2C1300)
        else -> listOf(Color(0xFF2A2B3D), Color(0xFF1B1C28)) to Color(0xFF94A3B8)
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (rank <= 3) 6.dp else 2.dp,
                shape = CircleShape,
                spotColor = badgeColors.first().copy(alpha = 0.5f)
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(badgeColors))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (rank <= 3) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = "Posição $rank",
                tint = iconColor,
                modifier = Modifier.size(size * 0.6f)
            )
        } else {
            Text(
                text = "#$rank",
                color = iconColor,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

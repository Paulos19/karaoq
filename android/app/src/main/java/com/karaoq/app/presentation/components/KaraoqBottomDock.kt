package com.karaoq.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoq.app.domain.model.NavigationTab
import com.karaoq.app.presentation.ui.theme.CardDarkSurface
import com.karaoq.app.presentation.ui.theme.CardElevated
import com.karaoq.app.presentation.ui.theme.FlameOrange
import com.karaoq.app.presentation.ui.theme.FlameOrangeLight
import com.karaoq.app.presentation.ui.theme.GlassBorder
import com.karaoq.app.presentation.ui.theme.TextMuted
import com.karaoq.app.presentation.ui.theme.TextPureWhite

/**
 * Dock de navegação flutuante inferior (Floating Bottom Navigation Bar)
 * inspirado diretamente nas referências 2 e 3 (design moderno em ilha com cantos arredondados).
 */
@Composable
fun KaraoqBottomDock(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val dockShape = CircleShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .shadow(
                elevation = 20.dp,
                shape = dockShape,
                ambientColor = Color.Black.copy(alpha = 0.7f),
                spotColor = FlameOrange.copy(alpha = 0.25f)
            )
            .clip(dockShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CardElevated.copy(alpha = 0.95f),
                        CardDarkSurface.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        GlassBorder
                    )
                ),
                dockShape
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                tab = NavigationTab.CREATE,
                title = "Estúdio",
                icon = Icons.Rounded.GraphicEq,
                isSelected = currentTab == NavigationTab.CREATE,
                onClick = { onTabSelected(NavigationTab.CREATE) }
            )

            DockItem(
                tab = NavigationTab.LIBRARY,
                title = "Biblioteca",
                icon = Icons.Rounded.LibraryMusic,
                isSelected = currentTab == NavigationTab.LIBRARY,
                onClick = { onTabSelected(NavigationTab.LIBRARY) }
            )

            DockItem(
                tab = NavigationTab.LEADERBOARD,
                title = "Ranking",
                icon = Icons.Rounded.EmojiEvents,
                isSelected = currentTab == NavigationTab.LEADERBOARD,
                onClick = { onTabSelected(NavigationTab.LEADERBOARD) }
            )

            DockItem(
                tab = NavigationTab.SETTINGS,
                title = "Ajustes",
                icon = Icons.Rounded.Tune,
                isSelected = currentTab == NavigationTab.SETTINGS,
                onClick = { onTabSelected(NavigationTab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun DockItem(
    tab: NavigationTab,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dock_item_scale"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) TextPureWhite else TextMuted,
        label = "dock_icon_tint"
    )

    val activeBackground = Brush.horizontalGradient(
        colors = listOf(FlameOrange, FlameOrangeLight)
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(activeBackground)
                        .shadow(6.dp, CircleShape, spotColor = FlameOrange)
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )

            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = TextPureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

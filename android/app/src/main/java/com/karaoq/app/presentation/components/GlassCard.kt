package com.karaoq.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.karaoq.app.presentation.ui.theme.CardDarkSurface
import com.karaoq.app.presentation.ui.theme.CardElevated
import com.karaoq.app.presentation.ui.theme.FlameOrange
import com.karaoq.app.presentation.ui.theme.GlassBorder

/**
 * Card 3D Soft com efeito Glassmorphism, bordas translúcidas elegantes
 * e gradiente sutil de profundidade (inspirado nas referências 1 e 3).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = CardDarkSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    isActive: Boolean = false,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val effectiveBorder = if (isActive) {
        BorderStroke(
            borderWidth,
            Brush.linearGradient(
                listOf(
                    FlameOrange.copy(alpha = 0.8f),
                    FlameOrange.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.3f)
                )
            )
        )
    } else {
        BorderStroke(
            borderWidth,
            Brush.verticalGradient(
                listOf(
                    borderColor.copy(alpha = 0.35f),
                    borderColor.copy(alpha = 0.08f)
                )
            )
        )
    }

    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            CardElevated.copy(alpha = 0.6f),
            backgroundColor
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = if (isActive) FlameOrange.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.5f)
            )
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = Color.Transparent,
        border = effectiveBorder
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .padding(20.dp),
            content = content
        )
    }
}

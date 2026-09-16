package com.karaoq.app.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.karaoq.app.presentation.ui.theme.FlameOrange
import com.karaoq.app.presentation.ui.theme.FlameOrangeLight
import com.karaoq.app.presentation.ui.theme.SunsetCoral
import kotlin.math.sin

/**
 * Visualizador de onda sonora 2D animado em Canvas,
 * inspirado diretamente na onda eletroacústica da logo oficial do KaraoQ.
 */
@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    amplitude: Float = 0.5f,
    isProcessing: Boolean = false,
    primaryColor: Color = FlameOrange,
    secondaryColor: Color = SunsetCoral
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProcessing) 1200 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val midY = size.height / 2f
        val currentAmp = (size.height * 0.38f) * amplitude.coerceIn(0.15f, 1.0f) * if (isProcessing) pulseScale else 1f

        val gradient = Brush.horizontalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.2f),
                primaryColor,
                secondaryColor,
                FlameOrangeLight,
                primaryColor.copy(alpha = 0.2f)
            )
        )

        val backgroundGradient = Brush.horizontalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.05f),
                secondaryColor.copy(alpha = 0.3f),
                primaryColor.copy(alpha = 0.05f)
            )
        )

        // Onda 1: Onda Senoidal Principal com Harmônicos (Estilo KaraoQ Waveform)
        val path = Path()
        val bgPath = Path()

        val steps = 80
        val dx = width / steps

        path.moveTo(0f, midY)
        bgPath.moveTo(0f, midY)

        for (i in 0..steps) {
            val x = i * dx
            val normX = i.toFloat() / steps

            // Envoltória gaussiana para suaves extremidades
            val envelope = sin(normX * Math.PI).toFloat()

            // Composição harmônica: onda fundamental + harmônico rápido central
            val wave1 = sin((normX * 4 * Math.PI + phase).toDouble()).toFloat()
            val wave2 = sin((normX * 8 * Math.PI - phase * 1.5f).toDouble()).toFloat() * 0.4f
            val y = midY + (wave1 + wave2) * currentAmp * envelope

            path.lineTo(x, y)
            bgPath.lineTo(x, y)
        }

        // Preenchimento de reflexo sutil
        bgPath.lineTo(width, size.height)
        bgPath.lineTo(0f, size.height)
        bgPath.close()

        drawPath(
            path = bgPath,
            brush = backgroundGradient
        )

        // Linha de traço principal com glow
        drawPath(
            path = path,
            brush = gradient,
            style = Stroke(
                width = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // Pontos de pico dinâmicos (estilo monitor de áudio profissional)
        val samplePoints = listOf(0.25f, 0.45f, 0.55f, 0.75f)
        for (normX in samplePoints) {
            val px = normX * width
            val envelope = sin(normX * Math.PI).toFloat()
            val wave = sin((normX * 4 * Math.PI + phase).toDouble()).toFloat()
            val py = midY + wave * currentAmp * envelope

            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = Offset(px, py)
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.5f),
                radius = 5.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

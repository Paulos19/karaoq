package com.karaoq.app.presentation.karaoke

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoq.app.presentation.ui.theme.CardElevated
import com.karaoq.app.presentation.ui.theme.FlameOrange
import com.karaoq.app.presentation.ui.theme.FlameOrangeLight
import com.karaoq.app.presentation.ui.theme.GlassBorder
import com.karaoq.app.presentation.ui.theme.PitchMint
import com.karaoq.app.presentation.ui.theme.SunsetCoral
import com.karaoq.app.presentation.ui.theme.TextMuted
import com.karaoq.app.presentation.ui.theme.TextPureWhite
import kotlin.math.sin

/**
 * Cápsula WaveProgress inspirada diretamente na interface de voz do Google Gemini (referências 2 e 3).
 * Apresenta barras de áudio verticais animadas que reagem em tempo real à voz do cantor (amplitude e tom).
 */
@Composable
fun GeminiWaveProgress(
    isVoiceDetected: Boolean,
    amplitude: Float,
    pitchNote: String,
    modifier: Modifier = Modifier,
    isMicActive: Boolean = true,
    hasPermission: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_wave_pulse")

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gemini_wave_phase"
    )

    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (isVoiceDetected) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gemini_aura_scale"
    )

    val capsuleBackground = if (!hasPermission || !isMicActive) {
        Brush.horizontalGradient(listOf(Color(0xFF25262B), Color(0xFF1E1F24)))
    } else if (isVoiceDetected) {
        Brush.horizontalGradient(listOf(FlameOrange, SunsetCoral))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF1F222E), Color(0xFF1A1C24)))
    }

    val glowColor by animateColorAsState(
        targetValue = if (isVoiceDetected) FlameOrange.copy(alpha = 0.4f) else Color.Transparent,
        animationSpec = tween(250),
        label = "gemini_glow_color"
    )

    Box(
        modifier = modifier
            .scale(auraScale)
            .shadow(
                elevation = if (isVoiceDetected) 14.dp else 0.dp,
                shape = CircleShape,
                spotColor = glowColor,
                ambientColor = glowColor
            )
            .clip(CircleShape)
            .background(capsuleBackground)
            .border(
                1.dp,
                if (isVoiceDetected) FlameOrangeLight.copy(alpha = 0.8f) else GlassBorder,
                CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ícone do Microfone com status
            Icon(
                imageVector = if (!hasPermission || !isMicActive) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                contentDescription = null,
                tint = if (isVoiceDetected) TextPureWhite else if (isMicActive) PitchMint else TextMuted,
                modifier = Modifier.size(15.dp)
            )

            // Barras de equalização estilo Gemini Voice Pill (5 barras)
            GeminiWaveBars(
                isVoiceDetected = isVoiceDetected,
                amplitude = amplitude,
                pulsePhase = pulsePhase,
                height = 18.dp
            )

            // Nota cantada no momento (se detectada)
            if (isVoiceDetected && pitchNote.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = pitchNote,
                        color = TextPureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * Barras verticais que ondulam suavemente como as ondas de voz do Gemini.
 */
@Composable
fun GeminiWaveBars(
    isVoiceDetected: Boolean,
    amplitude: Float,
    pulsePhase: Float,
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    barCount: Int = 5
) {
    val barColor = if (isVoiceDetected) TextPureWhite else PitchMint

    Row(
        modifier = modifier.height(height),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        for (i in 0 until barCount) {
            val offset = i * 0.8f
            val wave = ((sin(pulsePhase + offset) + 1f) / 2f)

            val targetHeightFraction = if (isVoiceDetected) {
                val ampFactor = amplitude.coerceIn(0.15f, 1.0f)
                (0.35f + (wave * 0.65f * ampFactor)).coerceIn(0.25f, 1.0f)
            } else {
                (0.25f + (wave * 0.25f)).coerceIn(0.2f, 0.5f)
            }

            val animatedHeight by animateFloatAsState(
                targetValue = targetHeightFraction,
                animationSpec = tween(durationMillis = if (isVoiceDetected) 75 else 200),
                label = "bar_h_$i"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height * animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

package com.karaoq.app.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// KaraoQ Official Design System Palette (Derived from Logo & Studio Stage)
// =========================================================================

// Brand Identity Core (Waveform & 'Q' Slash)
val FlameOrange = Color(0xFFFF5E00)       // Laranja Elétrico da Logo
val FlameOrangeLight = Color(0xFFFF7A29)  // Realce do Gradiente
val FlameOrangeDark = Color(0xFFD94B00)   // Sombra do Gradiente
val SunsetCoral = Color(0xFFFF6E40)       // Tom complementar suave

// Canvas & Deep Backgrounds (Inspirado nas Referências 1 e 3)
val ObsidianDeep = Color(0xFF08080C)      // Fundo Base Deep Black
val CardDarkSurface = Color(0xFF13141E)   // Superfície do Card 3D
val CardElevated = Color(0xFF1B1C2A)      // Superfície Elevada / Modais
val CardHover = Color(0xFF232536)         // Estado de Pressionado / Hover

// Borders & Glassmorphism Gloss
val GlassBorder = Color(0x1FFFFFFF)       // Borda translúcida de 1dp (12% Branco)
val GlassBorderActive = Color(0x66FF5E00) // Borda ativa iluminada com Laranja
val GlassHighlight = Color(0x0AFFFFFF)    // Brilho interior sutil

// Stage Accents & Pitch Feedback
val PitchMint = Color(0xFF00F5D4)         // Afinação Perfeita & Playback Ativo
val PitchMintDark = Color(0xFF00BFA5)     // Tom mint secundário
val AmberGlow = Color(0xFFFFB703)         // Pontuação, Combos e Estrelas
val AlertRed = Color(0xFFFF3366)          // Alertas e Desvios Críticos

// Medals & Achievements (Sem Emojis)
val GoldMedal = Color(0xFFFFD166)
val SilverMedal = Color(0xFFE2E8F0)
val BronzeMedal = Color(0xFFCD7F32)

// Typography & Neutrals
val TextPureWhite = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// =========================================================================
// Backwards-Compatibility Aliases (Garante zero quebra no código existente)
// =========================================================================
val DarkBackground = ObsidianDeep
val DarkSurface = CardDarkSurface
val DarkSurfaceVariant = CardElevated
val DarkSurfaceBorder = GlassBorder
val NeonCyan = PitchMint
val NeonPink = FlameOrange
val NeonPurple = FlameOrangeLight
val ElectricGreen = PitchMint
val TextPrimary = TextPureWhite
val TextTertiary = TextMuted


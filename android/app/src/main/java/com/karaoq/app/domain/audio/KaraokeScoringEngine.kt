package com.karaoq.app.domain.audio

import com.karaoq.app.data.audio.PitchResult
import kotlin.math.abs

enum class ScoreRank(val symbol: String, val title: String, val minScore: Int) {
    S("S", "Superstar do KaraoQ! 👑", 8800),
    A("A", "Voz de Ouro! 🌟", 7200),
    B("B", "Belo Show! 🎤", 5000),
    C("C", "Bom Ensaio! Continue praticando 🎶", 0)
}

enum class VocalFeedback(val text: String, val points: Int) {
    PERFECT("Perfeito! ✨", 100),
    GOOD("Muito Bom! 👍", 65),
    OK("Quase lá! 🎵", 35),
    NONE("", 0)
}

data class ScoreFrameResult(
    val score: Int,
    val combo: Int,
    val multiplier: Int,
    val feedback: VocalFeedback,
    val currentNote: String,
    val isSinging: Boolean,
    val accuracyPercentage: Int = 0
)

data class KaraokeScoreSummary(
    val totalScore: Int,
    val maxCombo: Int,
    val perfectHits: Int,
    val goodHits: Int,
    val okHits: Int,
    val accuracyPercentage: Int,
    val rank: ScoreRank,
    val stars: Int,
    val songTitle: String,
    val artist: String
)

class KaraokeScoringEngine {

    private var currentScore: Double = 0.0
    private var comboCount: Int = 0
    private var maxCombo: Int = 0

    private var perfectHits: Int = 0
    private var goodHits: Int = 0
    private var okHits: Int = 0
    private var totalSingingFrames: Int = 0

    private var lastMidiNote: Int = 0
    private var noteSustainedFrames: Int = 0
    private var silentFrames: Int = 0

    fun reset() {
        currentScore = 0.0
        comboCount = 0
        maxCombo = 0
        perfectHits = 0
        goodHits = 0
        okHits = 0
        totalSingingFrames = 0
        lastMidiNote = 0
        noteSustainedFrames = 0
        silentFrames = 0
    }

    /**
     * Processa um frame de áudio avaliando:
     * 1. Presença de voz real (amplitude > limiar de canto e confiança YIN > 0.70)
     * 2. Janela de canto ativa da música (não pontua ruídos durante introduções instrumentais)
     * 3. Estabilidade tonal e acurácia da afinação (assertividade)
     */
    fun processFrame(
        pitch: PitchResult,
        amplitude: Float = 0.1f,
        isSingingSection: Boolean = true
    ): ScoreFrameResult {
        val currentAccuracy = getAccuracyPercentage()

        // 1. Se não estiver em seção cantável, ou sem afinação, ou amplitude for baixa (som ambiente/vazamento do speaker):
        val isVoiceActive = isSingingSection && amplitude >= 0.07f && pitch.isPitched && pitch.confidence >= 0.70f

        if (!isVoiceActive) {
            silentFrames++
            if (silentFrames > 5) {
                comboCount = 0
                noteSustainedFrames = 0
            }
            return ScoreFrameResult(
                score = currentScore.toInt().coerceIn(0, 10000),
                combo = comboCount,
                multiplier = getMultiplier(),
                feedback = VocalFeedback.NONE,
                currentNote = "",
                isSinging = false,
                accuracyPercentage = currentAccuracy
            )
        }

        silentFrames = 0
        totalSingingFrames++

        // 2. Avalia estabilidade e sustentação da nota
        if (pitch.midiNote == lastMidiNote || abs(pitch.centsDeviation) <= 35) {
            noteSustainedFrames++
        } else {
            lastMidiNote = pitch.midiNote
            noteSustainedFrames = 1
        }

        // Requer pelo menos 2 frames consecutivos (~90ms) para confirmar emissão vocal proposital (evita estalos/ruídos breves)
        if (noteSustainedFrames < 2) {
            return ScoreFrameResult(
                score = currentScore.toInt().coerceIn(0, 10000),
                combo = comboCount,
                multiplier = getMultiplier(),
                feedback = VocalFeedback.NONE,
                currentNote = pitch.noteName,
                isSinging = true,
                accuracyPercentage = currentAccuracy
            )
        }

        // 3. Multiplicador baseado em combo
        comboCount++
        if (comboCount > maxCombo) {
            maxCombo = comboCount
        }
        val multiplier = getMultiplier()

        // 4. Determina assertividade da nota (desvio em cents e estabilidade)
        val absCents = abs(pitch.centsDeviation)
        val feedback = when {
            absCents <= 18 && pitch.confidence >= 0.78f -> {
                perfectHits++
                VocalFeedback.PERFECT
            }
            absCents <= 35 && pitch.confidence >= 0.68f -> {
                goodHits++
                VocalFeedback.GOOD
            }
            else -> {
                okHits++
                VocalFeedback.OK
            }
        }

        // 5. Incremento calibrado
        val basePoints = feedback.points * 0.40
        val bonus = (noteSustainedFrames.coerceAtMost(8) * 1.2)
        currentScore += (basePoints + bonus) * multiplier * 0.50

        if (currentScore > 10000.0) {
            currentScore = 10000.0
        }

        return ScoreFrameResult(
            score = currentScore.toInt().coerceIn(0, 10000),
            combo = comboCount,
            multiplier = multiplier,
            feedback = feedback,
            currentNote = pitch.noteName,
            isSinging = true,
            accuracyPercentage = getAccuracyPercentage()
        )
    }

    private fun getAccuracyPercentage(): Int {
        if (totalSingingFrames == 0) return 0
        val weightedScore = (perfectHits * 1.0 + goodHits * 0.75 + okHits * 0.40)
        return ((weightedScore / totalSingingFrames) * 100).toInt().coerceIn(0, 100)
    }

    private fun getMultiplier(): Int {
        return when {
            comboCount >= 30 -> 4
            comboCount >= 18 -> 3
            comboCount >= 8 -> 2
            else -> 1
        }
    }

    fun finish(title: String, artist: String): KaraokeScoreSummary {
        val scoreInt = currentScore.toInt().coerceIn(0, 10000)

        val rank = when {
            scoreInt >= ScoreRank.S.minScore -> ScoreRank.S
            scoreInt >= ScoreRank.A.minScore -> ScoreRank.A
            scoreInt >= ScoreRank.B.minScore -> ScoreRank.B
            else -> ScoreRank.C
        }

        val stars = when {
            scoreInt >= 9200 -> 5
            scoreInt >= 7800 -> 4
            scoreInt >= 5800 -> 3
            scoreInt >= 3500 -> 2
            else -> 1
        }

        return KaraokeScoreSummary(
            totalScore = scoreInt,
            maxCombo = maxCombo,
            perfectHits = perfectHits,
            goodHits = goodHits,
            okHits = okHits,
            accuracyPercentage = getAccuracyPercentage(),
            rank = rank,
            stars = stars,
            songTitle = title,
            artist = artist
        )
    }
}

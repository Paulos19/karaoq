package com.karaoq.app.data.audio

import kotlin.math.log2
import kotlin.math.roundToInt

data class PitchResult(
    val frequencyHz: Float = 0f,
    val noteName: String = "",
    val midiNote: Int = 0,
    val centsDeviation: Int = 0,
    val confidence: Float = 0f,
    val isPitched: Boolean = false
)

class PitchDetector(
    private val sampleRate: Int = 44100,
    private val threshold: Float = 0.15f
) {
    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    // Faixa vocal humana: ~70Hz (D2) até ~1050Hz (C6)
    private val minPeriod = (sampleRate / 1050.0).toInt().coerceAtLeast(20)
    private val maxPeriod = (sampleRate / 70.0).toInt().coerceAtMost(700)

    /**
     * Executa a detecção de pitch pelo algoritmo YIN sobre um buffer PCM 16-bit normalizado.
     */
    fun detectPitch(buffer: ShortArray, length: Int): PitchResult {
        val windowSize = 1024
        if (length < windowSize + maxPeriod) {
            return PitchResult()
        }

        // Converte amostras para float normalizado (-1.0f a 1.0f)
        val audioData = FloatArray(length)
        var sumSquares = 0.0f
        for (i in 0 until length) {
            val sample = buffer[i] / 32768.0f
            audioData[i] = sample
            if (i < windowSize) {
                sumSquares += sample * sample
            }
        }

        // Se o sinal tiver energia baixa (silêncio, ruído ambiente ou vazamento do alto-falante), ignora
        val rms = kotlin.math.sqrt((sumSquares / windowSize).toDouble()).toFloat()
        if (rms < 0.055f) {
            return PitchResult()
        }

        // Etapa 1: Função de Diferença d(tau)
        val diff = FloatArray(maxPeriod)
        for (tau in 0 until maxPeriod) {
            var sum = 0.0f
            for (j in 0 until windowSize) {
                val delta = audioData[j] - audioData[j + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Etapa 2: Diferença Normalizada pela Média Cumulativa d'(tau)
        val cmndf = FloatArray(maxPeriod)
        cmndf[0] = 1.0f
        var runningSum = 0.0f
        for (tau in 1 until maxPeriod) {
            runningSum += diff[tau]
            cmndf[tau] = if (runningSum > 0f) {
                diff[tau] / (runningSum / tau)
            } else {
                1.0f
            }
        }

        // Etapa 3: Threshold Absoluto
        var tauEstimate = -1
        for (tau in minPeriod until maxPeriod) {
            if (cmndf[tau] < threshold) {
                // Encontra o mínimo local logo após cruzar o limiar
                var nextTau = tau
                while (nextTau + 1 < maxPeriod && cmndf[nextTau + 1] < cmndf[nextTau]) {
                    nextTau++
                }
                tauEstimate = nextTau
                break
            }
        }

        // Se nenhum passou pelo limiar estrito, busca o mínimo global dentro da faixa vocal
        if (tauEstimate == -1) {
            var minVal = Float.MAX_VALUE
            for (tau in minPeriod until maxPeriod) {
                if (cmndf[tau] < minVal) {
                    minVal = cmndf[tau]
                    tauEstimate = tau
                }
            }
            // Limiar estrito (0.18f): rejeita ruído ambiente, fala sussurrada ou vazamento do playback
            if (minVal > 0.18f) {
                return PitchResult()
            }
        }

        // Etapa 4: Interpolação Parabólica para precisão subamostral
        val refinedTau: Float = if (tauEstimate > minPeriod && tauEstimate < maxPeriod - 1) {
            val alpha = cmndf[tauEstimate - 1]
            val beta = cmndf[tauEstimate]
            val gamma = cmndf[tauEstimate + 1]
            val denominator = 2.0f * (2.0f * beta - alpha - gamma)
            if (denominator != 0f) {
                tauEstimate + (gamma - alpha) / denominator
            } else {
                tauEstimate.toFloat()
            }
        } else {
            tauEstimate.toFloat()
        }

        if (refinedTau <= 0f) return PitchResult()

        val frequencyHz = sampleRate / refinedTau
        if (frequencyHz < 65f || frequencyHz > 1200f) {
            return PitchResult()
        }

        // Mapeamento para Nota Musical (escala temperada A4 = 440Hz, MIDI 69)
        val midiFloat = 69f + 12f * (log2(frequencyHz / 440f))
        val midiNote = midiFloat.roundToInt()
        val cents = ((midiFloat - midiNote) * 100f).roundToInt()

        val noteIndex = (midiNote % 12 + 12) % 12
        val octave = (midiNote / 12) - 1
        val noteName = "${noteNames[noteIndex]}$octave"

        val confidence = (1.0f - cmndf[tauEstimate]).coerceIn(0f, 1f)
        if (confidence < 0.65f) {
            return PitchResult()
        }

        return PitchResult(
            frequencyHz = frequencyHz,
            noteName = noteName,
            midiNote = midiNote,
            centsDeviation = cents,
            confidence = confidence,
            isPitched = true
        )
    }
}

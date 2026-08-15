package com.example.healthmonitor.camera

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Turns fingertip camera samples (red channel of the flashlight-illuminated
 * finger) into a heart rate using detrending + adaptive peak detection.
 */
class HeartRateAnalyzer {

    data class Result(val bpm: Int, val confidence: Float, val waveform: FloatArray)

    companion object {
        private const val SAMPLE_RATE = 30
        private const val MAX_SAMPLES = 300
        private const val MIN_SAMPLES = 120
    }

    private val raw = ArrayDeque<Double>()

    val size: Int
        get() = raw.size

    fun reset() {
        raw.clear()
    }

    fun addSample(value: Double) {
        raw.addLast(value)
        while (raw.size > MAX_SAMPLES) raw.removeFirst()
    }

    fun analyze(): Result? {
        if (raw.size < MIN_SAMPLES) return null

        val n = raw.size
        val values = DoubleArray(n) { raw.elementAt(it) }

        val detrended = DoubleArray(n)
        val window = SAMPLE_RATE
        var sum = 0.0
        for (i in 0 until n) {
            sum += values[i]
            if (i >= window) sum -= values[i - window]
            val avg = sum / (min(i, window - 1) + 1)
            detrended[i] = values[i] - avg
        }

        val peaks = mutableListOf<Int>()
        var runningMax = 0.0
        var lastPeakIndex = -100
        for (i in 1 until n - 1) {
            val v = detrended[i]
            runningMax = max(runningMax, abs(v))
            val threshold = runningMax * 0.45
            if (v > threshold &&
                detrended[i] > detrended[i - 1] &&
                detrended[i] > detrended[i + 1] &&
                i - lastPeakIndex >= (SAMPLE_RATE / 3)
            ) {
                peaks.add(i)
                lastPeakIndex = i
            }
        }

        if (peaks.size < 3) return null

        val durationSec = n.toDouble() / SAMPLE_RATE
        val bpm = (peaks.size / durationSec * 60.0).roundToInt()
        if (bpm < 35 || bpm > 210) return null

        var diffSum = 0.0
        for (i in 1 until peaks.size) {
            diffSum += (peaks[i] - peaks[i - 1]).toDouble()
        }
        val meanDiff = diffSum / (peaks.size - 1)
        var variance = 0.0
        for (i in 1 until peaks.size) {
            val d = peaks[i] - peaks[i - 1]
            variance += (d - meanDiff) * (d - meanDiff)
        }
        variance /= (peaks.size - 1)
        val cv = sqrt(variance) / meanDiff
        val confidence = (1.0 - min(1.0, cv)).toFloat().coerceIn(0f, 1f)

        return Result(bpm, confidence, detrended.map { it.toFloat() }.toFloatArray())
    }
}

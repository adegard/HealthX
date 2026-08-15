package com.example.healthmonitor.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(224, 54, 90)
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gridPaint = Paint().apply {
        color = Color.argb(40, 255, 255, 255)
        strokeWidth = 1f
    }

    private var data = FloatArray(0)

    fun setWaveform(values: FloatArray) {
        data = values
        invalidate()
    }

    fun clear() {
        data = FloatArray(0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        for (i in 1 until 5) {
            val y = height * i / 5f
            canvas.drawLine(0f, y, width, y, gridPaint)
        }

        if (data.size < 2) return

        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        for (v in data) {
            if (v < min) min = v
            if (v > max) max = v
        }
        val range = (max - min).coerceAtLeast(1f)

        val path = Path()
        val step = width / data.size
        for (i in data.indices) {
            val x = i * step
            val normalized = (data[i] - min) / range
            val y = height - normalized * (height - 8f) - 4f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linePaint)
    }
}

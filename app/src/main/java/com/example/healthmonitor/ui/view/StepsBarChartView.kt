package com.example.healthmonitor.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class StepsBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(224, 54, 90)
        style = Paint.Style.FILL
    }

    private val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(54, 160, 224)
        style = Paint.Style.FILL
    }

    private val baselinePaint = Paint().apply {
        color = Color.argb(60, 255, 255, 255)
        strokeWidth = 2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(154, 163, 178)
        textSize = 11f * resources.displayMetrics.scaledDensity
    }

    private var values: List<Int> = emptyList()

    fun setValues(newValues: List<Int>) {
        values = newValues
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        canvas.drawLine(0f, height - 1f, width, height - 1f, baselinePaint)
        if (values.isEmpty()) return

        val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
        val slot = width / values.size
        val barWidth = slot * 0.6f

        for (i in values.indices) {
            val x = i * slot + slot * 0.2f
            val barHeight = (values[i].toFloat() / max) * (height - 28f)
            val paint = if (i == values.size - 1) todayPaint else barPaint
            canvas.drawRect(x, height - barHeight, x + barWidth, height, paint)
        }
        canvas.drawText("max $max", 8f, height - 8f, labelPaint)
    }
}

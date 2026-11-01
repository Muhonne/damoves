package com.example.soundboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/** One range with two draggable endpoints. Values are in milliseconds. */
class ClipTimeline(context: Context) : View(context) {
    var duration = 1
    var start = 0
    var end = 1
    var onChange: ((Boolean) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var draggingStart = true
    private val padding = 24f * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        val left = padding
        val span = (width - padding * 2).coerceAtLeast(1f)
        val y = height / 2f
        paint.strokeWidth = 8f * resources.displayMetrics.density
        paint.color = Color.DKGRAY
        canvas.drawLine(left, y, left + span, y, paint)
        val a = left + span * start / duration
        val b = left + span * end / duration
        paint.color = Color.CYAN
        canvas.drawLine(a, y, b, y, paint)
        canvas.drawCircle(a, y, 10f * resources.displayMetrics.density, paint)
        canvas.drawCircle(b, y, 10f * resources.displayMetrics.density, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val span = (width - padding * 2).coerceAtLeast(1f)
        val value = (((event.x - padding) / span) * duration).toInt().coerceIn(0, duration)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                draggingStart = abs(value - start) <= abs(value - end)
            }
            MotionEvent.ACTION_MOVE -> Unit
            MotionEvent.ACTION_UP -> performClick()
            MotionEvent.ACTION_CANCEL -> return true
            else -> return false
        }
        if (draggingStart) start = value.coerceAtMost(end - 1).coerceAtLeast(0)
        else end = value.coerceAtLeast(start + 1).coerceAtMost(duration)
        invalidate()
        onChange?.invoke(draggingStart)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

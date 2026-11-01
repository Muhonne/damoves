package com.example.soundboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/** One range with two draggable endpoints. Values are in milliseconds. */
class ClipTimeline(context: Context) : View(context) {
    var duration = 1
    var start = 0
    var end = 1
    var position = 0
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    var onChange: ((Boolean) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var draggingStart = true
    private var lastHapticValue = -1
    private val padding = 24f * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val left = padding
        val span = (width - padding * 2).coerceAtLeast(1f)
        val y = height / 2f
        val a = left + span * start / duration
        val b = left + span * end / duration
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 10f * density
        paint.color = context.getColor(R.color.outline)
        canvas.drawLine(left, y, left + span, y, paint)
        paint.color = context.getColor(R.color.primary)
        canvas.drawLine(a, y, b, y, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(a, y, 12f * density, paint)
        canvas.drawCircle(b, y, 12f * density, paint)
        val playhead = left + span * position.coerceIn(0, duration) / duration
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * density
        paint.color = context.getColor(R.color.text_primary)
        canvas.drawLine(playhead, y - 16f * density, playhead, y + 16f * density, paint)
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
        if (abs(value - lastHapticValue) >= 250) {
            lastHapticValue = value
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        invalidate()
        onChange?.invoke(draggingStart)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

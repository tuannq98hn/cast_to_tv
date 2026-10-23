package com.casttotv.screen.mirroring.ui.activities.new_intro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.casttotv.screen.mirroring.R

/** Rounded track + fill used by the intro pager. */
class DynamicProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.color_main_15)
        style = Paint.Style.FILL
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_main)
        style = Paint.Style.FILL
    }
    private var track = RectF()
    private var fill = RectF()
    private var radius = 32f
    private var initialProgress = 0

    init {
        context.withStyledAttributes(attrs, R.styleable.DynamicProgressBar) {
            initialProgress = getInt(R.styleable.DynamicProgressBar_currentProgress, 0)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        track = RectF(0f, 0f, w.toFloat(), h.toFloat())
        radius = h / 2f
        setCurrentProgress(initialProgress)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawRoundRect(track, radius, radius, trackPaint)
        canvas.drawRoundRect(fill, radius, radius, fillPaint)
    }

    fun setCurrentProgress(value: Int) {
        fill = if (value > 0) RectF(0f, 0f, width * value / 100f, height.toFloat()) else RectF()
        postInvalidate()
    }
}

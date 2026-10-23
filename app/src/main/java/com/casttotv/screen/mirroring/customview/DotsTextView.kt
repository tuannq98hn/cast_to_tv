package com.casttotv.screen.mirroring.customview

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.util.AttributeSet
import androidx.annotation.Keep
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import com.casttotv.screen.mirroring.R
import kotlin.math.max
import kotlin.math.sin

/** Animated "..." loading text (attrs: `WaitingDots`). */
class DotsTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val dots = List(3) { JumpingSpan() }
    private val animators = AnimatorSet()
    private var jumpHeight = 0
    private var period = 6000
    private var autoPlay = true

    init {
        context.withStyledAttributes(attrs, R.styleable.WaitingDots) {
            period = getInt(R.styleable.WaitingDots_period, 6000)
            jumpHeight = getInt(R.styleable.WaitingDots_jumpHeight, (textSize / 4).toInt())
            autoPlay = getBoolean(R.styleable.WaitingDots_autoplay, true)
        }
        text = SpannableString("...​").apply {
            dots.forEachIndexed { i, span -> setSpan(span, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        }
        val first = jump(dots[0], 0).apply { addUpdateListener { invalidate() } }
        animators.playTogether(first, jump(dots[1], period / 6L), jump(dots[2], period * 2 / 6L))
        if (autoPlay && !isInEditMode) animators.start()
    }

    fun setJumpHeight(value: Int) {
        jumpHeight = value
    }

    fun setPeriod(value: Int) {
        period = value
    }

    override fun onDetachedFromWindow() {
        animators.cancel()
        super.onDetachedFromWindow()
    }

    private fun jump(span: JumpingSpan, delay: Long) =
        ObjectAnimator.ofFloat(span, "translationY", 0f, -jumpHeight.toFloat()).apply {
            setEvaluator(SinEvaluator)
            duration = period.toLong()
            startDelay = delay
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }

    private object SinEvaluator : TypeEvaluator<Number> {
        override fun evaluate(fraction: Float, from: Number, to: Number): Number =
            (max(0.0, sin(fraction * Math.PI * 2)) * (to.toFloat() - from.toFloat())).toFloat()
    }

    private class JumpingSpan : ReplacementSpan() {
        @get:Keep @set:Keep
        var translationY = 0f

        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?) =
            paint.measureText(text, start, end).toInt()

        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            canvas.drawText(text, start, end, x, y + translationY, paint)
        }
    }
}

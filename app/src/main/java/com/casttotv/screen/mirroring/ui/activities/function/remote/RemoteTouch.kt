package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

enum class Swipe { UP, DOWN, LEFT, RIGHT }

/** `ht2` + `iq`: touchpad where a fling is a D-pad key and a tap is OK. */
class SwipeTouchListener(context: Context, private val onSwipe: (Swipe) -> Unit, private val onTap: () -> Unit) :
    View.OnTouchListener {

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onTap()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            val start = e1 ?: return false
            val dx = e2.x - start.x
            val dy = e2.y - start.y
            if (abs(dx) > abs(dy)) {
                if (abs(dx) <= 100 || abs(vx) <= 100) return false
                onSwipe(if (dx > 0) Swipe.RIGHT else Swipe.LEFT)
            } else {
                if (abs(dy) <= 100 || abs(vy) <= 100) return false
                onSwipe(if (dy > 0) Swipe.DOWN else Swipe.UP)
            }
            return true
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent) = detector.onTouchEvent(event)
}

/** `ci6`/`fi6`: mouse pad. Drag moves with a 1.1 power curve, tap clicks, a two-finger drag scrolls. */
class PointerTouchListener(
    private val onMove: (Double, Double) -> Unit,
    private val onClick: () -> Unit,
    private val onScroll: (Double, Double) -> Unit,
) : View.OnTouchListener {
    private var down = false
    private var dragging = false
    private var multi = false
    private var startX = 0f
    private var startY = 0f
    private var lastX = Float.NaN
    private var lastY = Float.NaN

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val wasDragging = dragging
        val wasMulti = multi
        multi = multi || event.pointerCount > 1
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = true
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_UP -> {
                down = false
                dragging = false
                multi = false
                lastX = Float.NaN
                lastY = Float.NaN
            }
        }
        val dx = if (lastX.isNaN()) 0f else (event.x - lastX).roundToInt().toFloat()
        val dy = if (lastY.isNaN()) 0f else (event.y - lastY).roundToInt().toFloat()
        if (event.actionMasked != MotionEvent.ACTION_UP) {
            lastX = event.x
            lastY = event.y
        }
        if (down && !dragging && abs(event.x - startX) > 10 && abs(event.y - startY) > 10) dragging = true

        when {
            down && dragging -> if (dx != 0f && dy != 0f && !multi) onMove(accelerate(dx), accelerate(dy))
            !down && !wasDragging -> onClick()
            !down && wasMulti -> onScroll((event.x - startX).toDouble(), (event.y - startY).toDouble())
        }
        if (!down) dragging = false
        return true
    }

    private fun accelerate(delta: Float) = (if (delta >= 0) 1 else -1) * abs(delta).toDouble().pow(1.1).roundToInt().toDouble()
}

/**
 * Swaps [target]'s image (ImageView) or background while one of the D-pad [keys] is held, like the original remotes.
 * [pressed] maps each key view to its pressed drawable; [idle] is restored on release.
 */
@SuppressLint("ClickableViewAccessibility")
fun bindPressedBackground(target: View, idle: Int, pressed: Map<View, Int>) {
    val apply: (Int) -> Unit =
        if (target is android.widget.ImageView) target::setImageResource else target::setBackgroundResource
    pressed.forEach { (view, drawable) ->
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> apply(drawable)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> apply(idle)
            }
            false
        }
    }
}

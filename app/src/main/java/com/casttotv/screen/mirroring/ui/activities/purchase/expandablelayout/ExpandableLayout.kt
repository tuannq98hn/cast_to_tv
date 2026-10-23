package com.casttotv.screen.mirroring.ui.activities.purchase.expandablelayout

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.withStyledAttributes
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.ExpandableLayoutFrameBinding

/** Header + collapsible body with an animated spinner arrow (attrs: `ExpandableLayout`). */
class ExpandableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class Animation { NORMAL, ACCELERATE, BOUNCE, OVERSHOOT }
    enum class SpinnerGravity { START, END }

    lateinit var parentLayout: View
    lateinit var secondLayout: View

    var isExpanded = false
        private set
    private var isExpanding = false
    private var isCollapsing = false
    private var secondLayoutHeight = 0

    private val frame = ExpandableLayoutFrameBinding.inflate(LayoutInflater.from(context))

    var parentLayoutResource = R.layout.expandable_layout_frame
        set(value) { field = value; updateExpandableLayout() }
    var secondLayoutResource = R.layout.expandable_layout_child
        set(value) { field = value; updateExpandableLayout() }
    var spinnerDrawable: Drawable? = null
        set(value) { field = value; updateSpinner() }
    var showSpinner = true
        set(value) { field = value; updateSpinner() }
    var spinnerSize = 12 * resources.displayMetrics.density
        private set
    var spinnerMargin = 14 * resources.displayMetrics.density
        private set
    var spinnerColor = -1
        set(value) { field = value; updateSpinner() }
    var spinnerGravity = SpinnerGravity.END
        set(value) { field = value; updateSpinner() }
    var duration = 250L
    var expandableAnimation = Animation.NORMAL
    var spinnerRotation = -180
    var spinnerAnimate = true
    var onExpandListener: ((Boolean) -> Unit)? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.ExpandableLayout, defStyleAttr, 0) {
            isExpanded = getBoolean(R.styleable.ExpandableLayout_expandable_isExpanded, isExpanded)
            parentLayoutResource = getResourceId(R.styleable.ExpandableLayout_expandable_parentLayout, parentLayoutResource)
            secondLayoutResource = getResourceId(R.styleable.ExpandableLayout_expandable_secondLayout, secondLayoutResource)
            getResourceId(R.styleable.ExpandableLayout_expandable_spinner, -1).takeIf { it != -1 }?.let {
                spinnerDrawable = AppCompatResources.getDrawable(context, it)
            }
            showSpinner = getBoolean(R.styleable.ExpandableLayout_expandable_showSpinner, showSpinner)
            spinnerSize = getDimensionPixelSize(R.styleable.ExpandableLayout_expandable_spinner_size, spinnerSize.toInt()).toFloat()
            spinnerMargin = getDimensionPixelSize(R.styleable.ExpandableLayout_expandable_spinner_margin, spinnerMargin.toInt()).toFloat()
            spinnerColor = getColor(R.styleable.ExpandableLayout_expandable_spinner_color, spinnerColor)
            spinnerGravity = SpinnerGravity.entries[getInteger(R.styleable.ExpandableLayout_expandable_spinner_gravity, spinnerGravity.ordinal)]
            duration = getInteger(R.styleable.ExpandableLayout_expandable_duration, duration.toInt()).toLong()
            expandableAnimation = Animation.entries[getInteger(R.styleable.ExpandableLayout_expandable_animation, expandableAnimation.ordinal)]
            spinnerAnimate = getBoolean(R.styleable.ExpandableLayout_expandable_spinner_animate, spinnerAnimate)
            spinnerRotation = getInt(R.styleable.ExpandableLayout_expandable_spinner_rotation, spinnerRotation)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        updateExpandableLayout()
        if (isExpanded) {
            isExpanded = false
            post { expand() }
        }
    }

    fun setSpinnerSize(dp: Float) {
        spinnerSize = dp * resources.displayMetrics.density
        updateSpinner()
    }

    fun setSpinnerMargin(dp: Float) {
        spinnerMargin = dp * resources.displayMetrics.density
        updateSpinner()
    }

    fun expand() {
        if (isExpanded || isExpanding) return
        secondLayout.visible(true)
        isExpanding = true
        animator(0f, 1f) { value ->
            secondLayout.updateLayoutParams<ViewGroup.LayoutParams> { height = (secondLayoutHeight * value).toInt() }
            if (spinnerAnimate) frame.arrow.rotation = spinnerRotation * value
            if (value >= 1f) {
                onExpandListener?.invoke(isExpanded)
                isExpanding = false
                isExpanded = true
            }
        }
    }

    fun collapse() {
        if (!isExpanded || isCollapsing) return
        secondLayout.visible(false)
        isCollapsing = true
        animator(1f, 0f) { value ->
            secondLayout.updateLayoutParams<ViewGroup.LayoutParams> {
                height = parentLayout.height + ((height - parentLayout.height) * value).toInt()
            }
            if (spinnerAnimate) frame.arrow.rotation = spinnerRotation * value
            if (value <= 0f) {
                onExpandListener?.invoke(isExpanded)
                isCollapsing = false
                isExpanded = false
            }
        }
    }

    private fun animator(from: Float, to: Float, update: (Float) -> Unit) =
        ValueAnimator.ofFloat(from, to).apply {
            duration = this@ExpandableLayout.duration
            interpolator = when (expandableAnimation) {
                Animation.NORMAL -> LinearInterpolator()
                Animation.ACCELERATE -> AccelerateInterpolator()
                Animation.BOUNCE -> BounceInterpolator()
                Animation.OVERSHOOT -> OvershootInterpolator()
            }
            addUpdateListener { update(it.animatedValue as Float) }
            start()
        }

    private fun updateExpandableLayout() {
        visible(false)
        removeAllViews()
        val parent = LayoutInflater.from(context).inflate(parentLayoutResource, this, false)
        parent.measure(0, 0)
        frame.cover.removeAllViews()
        frame.cover.addView(parent)
        frame.cover.updateLayoutParams<ViewGroup.LayoutParams> { height = parent.measuredHeight }
        addView(frame.root)
        parentLayout = parent

        val second = LayoutInflater.from(context).inflate(secondLayoutResource, this, false)
        addView(second)
        second.post {
            secondLayoutHeight = measureHeight(second)
            second.updateLayoutParams<ViewGroup.LayoutParams> { height = 0 }
            second.y = 0f
            visible(true)
        }
        secondLayout = second
        updateSpinner()
    }

    /** Height of [view], growing when nested expandable layouts expand later. */
    private fun measureHeight(view: View): Int {
        var height = view.height
        (view as? ViewGroup)?.children?.filterIsInstance<ExpandableLayout>()?.forEach { child ->
            child.post {
                height += child.secondLayoutHeight
                secondLayoutHeight = height
            }
        }
        return height
    }

    private fun updateSpinner() {
        if (!::parentLayout.isInitialized) return
        val arrow = frame.arrow
        arrow.visible(showSpinner)
        spinnerDrawable?.let(arrow::setImageDrawable)
        arrow.imageTintList = ColorStateList.valueOf(spinnerColor)
        parentLayout.post { arrow.y = parentLayout.height / 2f - spinnerSize / 2 }
        arrow.updateLayoutParams<LayoutParams> {
            width = spinnerSize.toInt()
            height = spinnerSize.toInt()
            leftMargin = spinnerMargin.toInt()
            rightMargin = spinnerMargin.toInt()
            gravity = if (spinnerGravity == SpinnerGravity.START) Gravity.START else Gravity.END
        }
    }

    private fun View.visible(shown: Boolean) {
        visibility = if (shown) VISIBLE else INVISIBLE
    }
}

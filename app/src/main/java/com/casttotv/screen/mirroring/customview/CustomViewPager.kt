package com.casttotv.screen.mirroring.customview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class CustomViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {
    var pagingEnabled = true

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = pagingEnabled && super.onInterceptTouchEvent(ev)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean = pagingEnabled && super.onTouchEvent(ev)
}

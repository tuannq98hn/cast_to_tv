package com.casttotv.screen.mirroring.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

open class ButtonCGT @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.buttonStyle,
) : AppCompatButton(context, attrs, defStyleAttr) {
    init {
        typeface = Fonts.quicksand(context)
    }
}

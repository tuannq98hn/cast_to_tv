package com.casttotv.screen.mirroring.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.SearchView

class SearchViewCGT @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.searchViewStyle,
) : SearchView(context, attrs, defStyleAttr) {
    init {
        findViewById<TextView>(androidx.appcompat.R.id.search_src_text)?.typeface = Fonts.quicksand(context)
    }
}

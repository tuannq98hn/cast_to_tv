package com.casttotv.screen.mirroring.ui.view

import android.content.Context
import android.graphics.Typeface

/** The original views reload this asset on every construction; cache it once instead. */
internal object Fonts {
    private var quicksand: Typeface? = null

    fun quicksand(context: Context): Typeface =
        quicksand ?: Typeface.createFromAsset(context.assets, "fonts/Quicksand_Medium.ttf").also { quicksand = it }
}

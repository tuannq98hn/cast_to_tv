package com.casttotv.screen.mirroring.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Kotlin port of `utils.subs.AppSharePre`; same "app-content" file and keys as the shipped app. */
object AppPrefs {
    private lateinit var prefs: SharedPreferences

    private lateinit var settings: SharedPreferences
    private lateinit var data: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("app-content", Context.MODE_PRIVATE)
        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        data = context.getSharedPreferences("data", Context.MODE_PRIVATE)
    }

    /** `oi1`: the Settings "Vibrate" switch. */
    var hapticFeedback: Boolean
        get() = settings.getBoolean("haptic_feedback", true)
        set(value) = settings.edit { putBoolean("haptic_feedback", value) }

    var language: String
        get() = prefs.getString("key_language", "en")!!
        set(value) = prefs.edit { putString("key_language", value) }

    val languageOrder: String get() = prefs.getString("language_order", "")!!
    val autoSelectLanguage: Boolean get() = prefs.getBoolean("auto_select_language", false)
    val enableIntro: Boolean get() = prefs.getBoolean("enable_intro", false)

    /** "data" file: the user already rated / opened the store. */
    var rated: Boolean
        get() = data.getBoolean("rate", false)
        set(value) = data.edit { putBoolean("rate", value) }

    var rateCount: Int
        get() = data.getInt("count_rate", 3)
        set(value) = data.edit { putInt("count_rate", value) }

    var openCount: Int
        get() = prefs.getInt("KEY_OPEN_APP", 0)
        set(value) = prefs.edit { putInt("KEY_OPEN_APP", value) }

    var dateStartApp: String?
        get() = prefs.getString("KEY_DATE_START_APP", null)
        set(value) = prefs.edit { putString("KEY_DATE_START_APP", value) }

    var firstOpen: Boolean
        get() = prefs.getBoolean("first_open", true)
        set(value) = prefs.edit { putBoolean("first_open", value) }

    val hideNavigationBar: Boolean get() = prefs.getBoolean("hide_bottom_navigation_bar", true)
    val languageFirst: Boolean get() = prefs.getBoolean("language_first", true)
    val newIntro: Boolean get() = prefs.getBoolean("new_intro", false)

    /** `AppSharePre.h()`: tier-2 users must see the purchase screen before premium features. */
    val requirePurchaseForFeatures: Boolean
        get() = prefs.getBoolean("user_tier2", false) &&
            !prefs.getString("list_type_network", "applovin, unity, tiktok, minteral")!!.lowercase()
                .contains(prefs.getString("type_network", "null")!!)
}

package com.casttotv.screen.mirroring.ui.activities.splash

import android.app.Activity
import android.content.Intent
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.data.AppPrefs
import com.casttotv.screen.mirroring.data.Premium
import com.casttotv.screen.mirroring.ui.activities.intro.IntroActivity
import com.casttotv.screen.mirroring.ui.activities.language.LanguageFirstActivity
import com.casttotv.screen.mirroring.ui.activities.main.MainActivity
import com.casttotv.screen.mirroring.ui.activities.new_intro.NewIntroActivity
import com.casttotv.screen.mirroring.ui.activities.purchase.PurchaseActivity

/** Screen order after Splash / Language (`p26` "old flow" with `iap_onboarding_flow` = 0, and `LanguageFirstActivity.k()`). */
object Onboarding {

    fun afterSplash(activity: Activity) {
        val firstRun = AppPrefs.openCount == 0
        val purchase = AppPrefs.requirePurchaseForFeatures && !Premium.isActive
        when {
            firstRun && AppPrefs.languageFirst -> go(activity, LanguageFirstActivity::class.java)
            firstRun && !purchase -> go(activity, MainActivity::class.java)
            firstRun && AppPrefs.newIntro -> go(activity, NewIntroActivity::class.java)
            purchase -> purchase(activity)
            else -> go(activity, MainActivity::class.java)
        }
    }

    fun afterLanguage(activity: Activity) {
        when {
            !AppPrefs.requirePurchaseForFeatures || Premium.isActive ->
                go(activity, if (AppPrefs.enableIntro) IntroActivity::class.java else MainActivity::class.java)
            AppPrefs.newIntro -> go(activity, NewIntroActivity::class.java)
            else -> purchase(activity)
        }
    }

    private fun purchase(activity: Activity) = activity.startActivity(
        Intent(activity, PurchaseActivity::class.java)
            .putExtra(PurchaseActivity.EXTRA_ACTION, "SPLASH_ACTIVITY")
            .putExtra(PurchaseActivity.EXTRA_FROM, "screen_splash"),
    ).also { activity.finish() }

    private fun go(activity: Activity, cls: Class<out Activity>) {
        activity.startActivity(Intent(activity, cls))
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        activity.finish()
    }
}

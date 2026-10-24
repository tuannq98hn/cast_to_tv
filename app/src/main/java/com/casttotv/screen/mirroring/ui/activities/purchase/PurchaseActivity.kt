package com.casttotv.screen.mirroring.ui.activities.purchase

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.view.doOnLayout
import android.widget.Toast
import androidx.activity.addCallback
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.AppPrefs
import com.casttotv.screen.mirroring.data.Premium
import com.casttotv.screen.mirroring.databinding.ActivityPremiumOldBinding
import com.casttotv.screen.mirroring.ui.activities.function.remote.RemoteActivity
import com.casttotv.screen.mirroring.ui.activities.function.screen_mirror.ScreenMirroringActivity
import com.casttotv.screen.mirroring.ui.activities.function.youtube_browser.YoutubeBrowserActivity
import com.casttotv.screen.mirroring.ui.activities.language.LanguageFirstActivity
import com.casttotv.screen.mirroring.ui.activities.main.MainActivity
import com.casttotv.screen.mirroring.ui.activities.webcast.WebCastActivity

/** Port of `PurchaseActivity` (`activity_premium_old`): Dark Cinema "PRO Upgrade" yearly plan sheet. */
class PurchaseActivity : BaseActivity() {

    private lateinit var binding: ActivityPremiumOldBinding
    private var action = ""
    private var from: String? = null
    private val animators = mutableListOf<ValueAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumOldBinding.inflate(layoutInflater)
        setContentView(binding.root)
        action = intent.getStringExtra(EXTRA_ACTION).orEmpty()
        from = intent.getStringExtra(EXTRA_FROM)

        with(binding) {
            tvPolicy.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://begamob.com/cast-policy.html"))) }
            tvTerms.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://begamob.com/ofs-termofuse.html"))) }
            tvRestore.setOnClickListener { Premium.restore() }
            imvClose.setOnClickListener { leave() }
            expandable.parentLayout.setOnClickListener {
                expandable.expand()
                expandable.parentLayout.visibility = View.GONE
            }
            testing1ButtonYear.setOnClickListener {
                Premium.buy(this@PurchaseActivity, Premium.YEARLY) { ok ->
                    if (ok) leave() else Toast.makeText(this@PurchaseActivity, R.string.error_server, Toast.LENGTH_SHORT).show()
                }
            }
            // `b45`: no-trial yearly plan.
            tvDisplayPrice.setText(R.string.enjoy_full_access_only)
            tvTitleButtonOne.setText(R.string.start_with_full_access)
            Premium.price(Premium.YEARLY) { price ->
                runOnUiThread { testing1ButtonYearPrice.text = getString(R.string.txt_price, price, getString(R.string.year)) }
            }
        }
        onBackPressedDispatcher.addCallback(this) { leave() }
        startDecorations()
    }

    override fun onDestroy() {
        animators.forEach { it.cancel() }
        super.onDestroy()
    }

    /** Stitch PRO Upgrade motion: chip dot pulse, ping behind the devices, CTA shimmer sweep; plus the cyan title gradient. */
    private fun startDecorations() = with(binding) {
        animators += ObjectAnimator.ofFloat(premiumDot, View.ALPHA, 1f, 0.35f).apply {
            duration = 1000; repeatMode = ValueAnimator.REVERSE
        }
        animators += ObjectAnimator.ofPropertyValuesHolder(
            heroPing,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 2f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 2f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
        ).apply { duration = 1000; interpolator = DecelerateInterpolator() }
        testing1ButtonYear.clipToOutline = true
        testing1ButtonYear.doOnLayout { button ->
            ctaShimmer.visibility = View.VISIBLE
            animators += ObjectAnimator.ofFloat(ctaShimmer, View.TRANSLATION_X, -ctaShimmer.width.toFloat(), button.width.toFloat()).apply {
                duration = 2500; interpolator = LinearInterpolator(); repeatCount = ValueAnimator.INFINITE; start()
            }
        }
        txtProTitle.doOnLayout {
            txtProTitle.paint.shader = LinearGradient(
                0f, 0f, it.width.toFloat(), it.height.toFloat(),
                intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFA5F3FC.toInt(), 0xFF38BDF8.toInt()),
                floatArrayOf(0f, 0.2f, 0.6f, 1f), Shader.TileMode.CLAMP,
            )
            txtProTitle.invalidate()
        }
        animators.forEach { if (!it.isStarted) { it.repeatCount = ValueAnimator.INFINITE; it.start() } }
    }

    /** `o()`: continue where the paywall was opened from. */
    private fun leave() {
        if (action.equals("SPLASH_ACTIVITY", ignoreCase = true)) {
            open(if (AppPrefs.languageFirst && AppPrefs.openCount == 0) LanguageFirstActivity::class.java else MainActivity::class.java)
            finish()
            return
        }
        when (from) {
            "remote_main" -> open(RemoteActivity::class.java)
            "mirror_main" -> open(ScreenMirroringActivity::class.java)
            "NEW_INTRO_ACTIVITY" -> open(MainActivity::class.java)
            "vimeo_main" -> open(YoutubeBrowserActivity::class.java) { putExtra(YoutubeBrowserActivity.EXTRA_TYPE, "vimeo") }
            "youtube_main" -> open(YoutubeBrowserActivity::class.java) { putExtra(YoutubeBrowserActivity.EXTRA_TYPE, "youtube") }
            "browser_main" -> open(WebCastActivity::class.java)
        }
        finish()
    }

    companion object {
        const val EXTRA_ACTION = "ACTION"
        const val EXTRA_FROM = "name_from"
    }
}

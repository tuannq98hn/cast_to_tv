package com.casttotv.screen.mirroring.ui.activities.purchase

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
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
import com.google.android.material.bottomsheet.BottomSheetBehavior

/** Port of `PurchaseActivity` (`activity_premium_old`): yearly plan sheet over a looping promo video. */
class PurchaseActivity : BaseActivity() {

    private lateinit var binding: ActivityPremiumOldBinding
    private var action = ""
    private var from: String? = null
    private var music: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumOldBinding.inflate(layoutInflater)
        setContentView(binding.root)
        action = intent.getStringExtra(EXTRA_ACTION).orEmpty()
        from = intent.getStringExtra(EXTRA_FROM)

        with(binding) {
            tvPolicy.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://begamob.com/cast-policy.html"))) }
            tvTerms.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://begamob.com/ofs-termofuse.html"))) }
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
        startLooping(binding.shine, R.anim.left_right)
        startLooping(binding.cardViewPremium, R.anim.shake)

        val sheet = BottomSheetBehavior.from(binding.bottomSheet)
        sheet.expandedOffset = 140
        sheet.isFitToContents = false
        val screen = resources.displayMetrics.heightPixels
        binding.bottomSheet.post { sheet.peekHeight = screen - binding.videoView.height }
        sheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    binding.expandable.post { binding.expandable.collapse() }
                    binding.expandable.parentLayout.visibility = View.VISIBLE
                }
            }
        })
        onBackPressedDispatcher.addCallback(this) { leave() }
    }

    override fun onResume() {
        super.onResume()
        playVideo()
        music?.release()
        music = runCatching { MediaPlayer.create(this, R.raw.audio_intro)?.apply { isLooping = true; start() } }.getOrNull()
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoView.isPlaying) binding.videoView.pause()
        music?.pause()
    }

    override fun onDestroy() {
        music?.release()
        super.onDestroy()
    }

    /** `r()`: looping promo video. */
    private fun playVideo() = with(binding.videoView) {
        setVideoURI(Uri.parse("android.resource://$packageName/${R.raw.video_premium_1}"))
        setOnCompletionListener { start() }
        setOnErrorListener { _, _, _ -> true }
        start()
    }

    private fun startLooping(view: View, anim: Int) {
        val animation = AnimationUtils.loadAnimation(this, anim)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(a: Animation) = Unit
            override fun onAnimationRepeat(a: Animation) = Unit
            override fun onAnimationEnd(a: Animation) = view.startAnimation(animation)
        })
        view.startAnimation(animation)
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

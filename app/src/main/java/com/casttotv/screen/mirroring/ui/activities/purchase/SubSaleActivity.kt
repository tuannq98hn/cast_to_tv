package com.casttotv.screen.mirroring.ui.activities.purchase

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.addCallback
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.Premium
import com.casttotv.screen.mirroring.databinding.ActivitySale1Binding
import com.casttotv.screen.mirroring.databinding.DialogSaleBinding
import com.casttotv.screen.mirroring.ui.activities.main.MainActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Port of `SubSaleActivity` (`activity_sale1`, `iap_sale_option` default): the sale paywall opened from the
 * reminder notification. Closing it once offers the lifetime "super sale" (`bh1` type 1, `dialog_sale`).
 */
class SubSaleActivity : BaseActivity() {

    private lateinit var binding: ActivitySale1Binding
    private var music: MediaPlayer? = null
    private var offerShown = false
    private var yearPrice = ""
    private var superSalePrice = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySale1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            tvPolicy.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://begamob.com/cast-policy.html"))) }
            tvTerms.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://begamob.com/ofs-termofuse.html"))) }
            expandable.parentLayout.setOnClickListener {
                expandable.expand()
                expandable.parentLayout.visibility = View.GONE
            }
            imvClose.setOnClickListener { close() }
            rltSubYear.setOnClickListener { buy(Premium.SALE_YEARLY) }
        }
        // `f86` case 3 / case 1.
        Premium.salePrice(Premium.SALE_YEARLY, 50) { price, _ ->
            runOnUiThread {
                yearPrice = price
                binding.testing1ButtonYearPrice.text = getString(R.string.then_year, price)
            }
        }
        Premium.price(Premium.SUPER_SALE) { superSalePrice = it }
        loop(binding.shine, R.anim.left_right)
        loop(binding.cardViewPremium, R.anim.shake)

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
        onBackPressedDispatcher.addCallback(this) { close() }
    }

    override fun onResume() {
        super.onResume()
        with(binding.videoView) {
            setVideoURI(Uri.parse("android.resource://$packageName/${R.raw.video_premium_1}"))
            setOnCompletionListener { start() }
            setOnErrorListener { _, _, _ -> true }
            start()
        }
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

    private fun buy(product: String) = Premium.buy(this, product) { ok -> if (ok) leave() }

    /** `o()`: the first close offers the super sale. */
    private fun close() {
        if (offerShown) return leave()
        offerShown = true
        val b = DialogSaleBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(b.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        b.tvPrice.text = superSalePrice
        b.tvDesPrice.text = getString(R.string.lifetime_regular, yearPrice)
        b.imgClose.setOnClickListener {
            dialog.dismiss()
            leave()
        }
        b.btnContinue.setOnClickListener {
            dialog.dismiss()
            buy(Premium.SUPER_SALE)
        }
        dialog.show()
    }

    /** `q()`: from the notification go to Main, otherwise back. */
    private fun leave() {
        if (intent.getIntExtra(EXTRA_CLICK_NOTIFY, 0) != 0) open(MainActivity::class.java)
        finish()
    }

    private fun loop(view: View, anim: Int) {
        val animation = AnimationUtils.loadAnimation(this, anim)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(a: Animation) = Unit
            override fun onAnimationRepeat(a: Animation) = Unit
            override fun onAnimationEnd(a: Animation) = view.startAnimation(animation)
        })
        view.startAnimation(animation)
    }

    companion object {
        const val EXTRA_CLICK_NOTIFY = "KEY_CLICK_NOTIFY"
    }
}

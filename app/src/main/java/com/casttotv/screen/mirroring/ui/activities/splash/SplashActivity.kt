package com.casttotv.screen.mirroring.ui.activities.splash

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.children
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.AppPrefs
import com.casttotv.screen.mirroring.databinding.AvtivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private val animators = mutableListOf<ValueAnimator>()
    private val createdAt = SystemClock.uptimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = AvtivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startMotion(binding)
        onBackPressedDispatcher.addCallback(this) { /* blocked, as in the original */ }

        val count = AppPrefs.openCount
        // The counter stays 0 until the language screen is confirmed (`KEY_OPEN_APP` = 1).
        if (count == 0) AppPrefs.dateStartApp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH).format(Date())
        else AppPrefs.openCount = count + 1

        lifecycleScope.launch {
            // The original waited here for remote config + the open-app ad.
            delay(SPLASH_MS)
            if (!isFinishing) Onboarding.afterSplash(this@SplashActivity)
        }
    }

    override fun onDestroy() {
        animators.forEach { it.cancel() }
        super.onDestroy()
    }

    /** Stitch splash motion: radar rings, waveform bounce, phone ping, beam flow, drifting lights, loader pills. */
    private fun startMotion(b: AvtivitySplashBinding) {
        fun loop(target: View, duration: Long, delay: Long = 0, reverse: Boolean = false, vararg values: PropertyValuesHolder) {
            animators += ObjectAnimator.ofPropertyValuesHolder(target, *values).apply {
                this.duration = duration
                startDelay = delay
                repeatCount = ValueAnimator.INFINITE
                if (reverse) repeatMode = ValueAnimator.REVERSE
                start()
            }
        }
        fun f(property: android.util.Property<View, Float>, vararg v: Float) = PropertyValuesHolder.ofFloat(property, *v)

        // radar: scale 0.85 -> 2.2, fade out, 2.8 s, second ring 0.9 s later
        listOf(b.ringInner to 0L, b.ringOuter to 900L).forEach { (ring, delay) ->
            ring.alpha = 0f
            loop(ring, 2800, delay, values = arrayOf(f(View.SCALE_X, 0.85f, 2.2f), f(View.SCALE_Y, 0.85f, 2.2f), f(View.ALPHA, 0.9f, 0f)))
            animators.last().interpolator = DecelerateInterpolator()
        }
        // waveform: bounce up by a quarter of each bar, outer bars lead
        b.waveBars.children.forEachIndexed { i, bar ->
            val lead = listOf(300L, 150L, 0L, 150L, 300L)[i]
            bar.post {
                loop(bar, 500, 500 - lead, reverse = true, f(View.TRANSLATION_Y, 0f, -bar.height / 4f))
                animators.last().interpolator = AccelerateDecelerateInterpolator()
            }
        }
        listOf(b.phonePing, b.statusPing).forEach {
            loop(it, 1000, values = arrayOf(f(View.SCALE_X, 1f, 2f), f(View.SCALE_Y, 1f, 2f), f(View.ALPHA, 1f, 0f)))
            animators.last().interpolator = DecelerateInterpolator()
        }
        // beam: rise 12 -> -12 dp while growing, fading in then out
        val dp = resources.displayMetrics.density
        loop(
            b.imgBeam, 2400, values = arrayOf(
                f(View.TRANSLATION_Y, 12 * dp, -12 * dp), f(View.SCALE_X, 0.9f, 1.1f), f(View.SCALE_Y, 0.9f, 1.1f),
                f(View.ALPHA, 0f, 0.85f, 0f),
            ),
        )
        // drifting lights: +15/-15 dp and 1.12x, 7 s back and forth
        listOf(b.blobCyan, b.blobPurple).forEach {
            loop(
                it, 7000, reverse = true, values = arrayOf(
                    f(View.TRANSLATION_X, it.translationX, it.translationX + 15 * dp),
                    f(View.TRANSLATION_Y, it.translationY, it.translationY - 15 * dp),
                    f(View.SCALE_X, 1f, 1.12f), f(View.SCALE_Y, 1f, 1.12f),
                ),
            )
        }
        // loader: spread the four pills over what is left of the splash delay once the first frame is up
        // (a slow cold start can eat most of it before anything is drawn)
        val lit = ColorStateList.valueOf(0xFF22D3EE.toInt())
        b.loaderPills.post {
            val left = (SPLASH_MS - (SystemClock.uptimeMillis() - createdAt)).coerceAtLeast(0)
            b.loaderPills.children.forEachIndexed { i, pill ->
                pill.postDelayed({ pill.backgroundTintList = lit }, left * i / 4)
            }
        }
    }

    private companion object {
        const val SPLASH_MS = 2_000L
    }
}

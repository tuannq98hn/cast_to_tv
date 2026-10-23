package com.casttotv.screen.mirroring.ui.activities.splash

import android.annotation.SuppressLint
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(AvtivitySplashBinding.inflate(layoutInflater).root)
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

    private companion object {
        const val SPLASH_MS = 2_000L
    }
}

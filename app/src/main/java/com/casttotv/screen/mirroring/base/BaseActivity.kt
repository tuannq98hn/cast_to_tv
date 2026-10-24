package com.casttotv.screen.mirroring.base

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.data.AppPrefs
import com.casttotv.screen.mirroring.databinding.ItemHeaderBinding
import com.casttotv.screen.mirroring.databinding.LayoutMediaHeaderBinding
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.function.connect.SearchTVActivity
import com.casttotv.screen.mirroring.ui.dialogs.DisconnectDialog
import kotlinx.coroutines.launch

/** Port of `BaseActivityKt` + `BaseActivityBinding` + the old `BaseActivity` (ads and analytics removed). */
abstract class BaseActivity : AppCompatActivity() {

    private var afterConnect: (() -> Unit)? = null

    /** Request code 821 in the original: connect first, then continue with the pending action. */
    private val connectLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val action = afterConnect
        afterConnect = null
        if (it.resultCode == RESULT_OK && TvConnect.isConnected) action?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        if (AppPrefs.hideNavigationBar) hideNavigationBar()
    }

    /** `ak0.g0`: Dark Cinema status bar with light icons; content kept below it under Android 15 edge-to-edge. */
    private fun setupSystemBars() {
        @Suppress("DEPRECATION")
        window.statusBarColor = ContextCompat.getColor(this, R.color.dc_bg)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            view.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top, 0, 0)
            // Consumed here so `fitsSystemWindows` layouts below don't pad for it a second time.
            WindowInsetsCompat.Builder(insets).setInsets(WindowInsetsCompat.Type.statusBars(), androidx.core.graphics.Insets.NONE).build()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && AppPrefs.hideNavigationBar) hideNavigationBar()
    }

    fun open(cls: Class<out Activity>, extras: Intent.() -> Unit = {}) {
        startActivity(Intent(this, cls).apply(extras))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /** Runs [action] now when a TV is connected, otherwise after a successful connection in SearchTV. */
    fun withConnection(action: () -> Unit) {
        if (TvConnect.isConnected) {
            action()
            return
        }
        afterConnect = action
        connectLauncher.launch(Intent(this, SearchTVActivity::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /** Standard `item_header`: back, title, connection icon (connect / disconnect dialog), premium hidden. */
    fun bindHeader(header: ItemHeaderBinding, title: CharSequence) {
        header.tvTitle.text = title
        header.castPremium.visibility = View.GONE
        header.imvBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        header.castConnect.setOnClickListener {
            if (TvConnect.isConnected) DisconnectDialog(this, TvConnect.deviceName) { renderConnection(header) }.show()
            else SearchTVActivity.open(this)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                renderConnection(header)
                TvConnect.events.collect { renderConnection(header) }
            }
        }
    }

    /** [bindHeader] for the Stitch media header; [showStatus] = false hides the TV line (the screen shows it itself). */
    fun bindMediaHeader(header: LayoutMediaHeaderBinding, title: CharSequence, showStatus: Boolean = true) {
        header.tvTitle.text = title
        header.llStatus.isVisible = showStatus
        header.imvBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        header.castConnect.setOnClickListener {
            if (TvConnect.isConnected) DisconnectDialog(this, TvConnect.deviceName) { renderConnection(header) }.show()
            else SearchTVActivity.open(this)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                renderConnection(header)
                TvConnect.events.collect { renderConnection(header) }
            }
        }
    }

    private fun renderConnection(header: LayoutMediaHeaderBinding) {
        val connected = TvConnect.isConnected
        header.castConnect.setImageResource(if (connected) R.drawable.dc_ic_cast_connected else R.drawable.dc_ic_cast)
        header.viewStatusDot.isVisible = connected
        header.tvStatus.text = if (connected) TvConnect.deviceName else getString(R.string.tab_to_connect)
        header.tvStatus.setTextColor(getColor(if (connected) R.color.dc_primary_soft else R.color.dc_text_muted))
    }

    private fun renderConnection(header: ItemHeaderBinding) = header.castConnect.setImageResource(
        if (TvConnect.isConnected) R.drawable.ic_header_connected else R.drawable.ic_header_not_connect,
    )

    private fun hideNavigationBar() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

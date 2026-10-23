package com.casttotv.screen.mirroring.ui.activities.function.browser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.Premium
import com.casttotv.screen.mirroring.databinding.BottomSheetCodeBinding
import com.casttotv.screen.mirroring.databinding.LayoutFileBrowserBinding
import com.casttotv.screen.mirroring.mirror.MirrorTargets
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.tv.WebReceiver
import com.casttotv.screen.mirroring.ui.activities.function.screen_mirror.QrScanActivity
import com.casttotv.screen.mirroring.ui.activities.purchase.PurchaseActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

/**
 * Port of `FileBrowserActivity` ("Web Cast" from the connect screen): serve the receiver page from the phone, pair
 * a TV browser with the bcastmirror.com code (typed or scanned), and treat the browser as the connected TV.
 */
class FileBrowserActivity : BaseActivity() {

    private lateinit var binding: LayoutFileBrowserBinding
    private var sheet: BottomSheetDialog? = null
    private var pairing = false

    private val scan = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringExtra(QrScanActivity.EXTRA_RESULT)?.takeIf { result.resultCode == RESULT_OK }?.let(::pair)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutFileBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WebReceiver.start(this)
        binding.tvTitle.setText(R.string.title_web_cast)
        binding.castPremium.setImageResource(if (Premium.isActive) R.drawable.imv_member_vip else R.drawable.imv_premium)
        binding.castPremium.setOnClickListener {
            if (!Premium.isActive) open(PurchaseActivity::class.java) { putExtra(PurchaseActivity.EXTRA_FROM, "web_browser") }
        }
        binding.imvBack.setOnClickListener { finish() }
        binding.btnStartScreenMirroring.setOnClickListener { showCodeSheet() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WebReceiver.clients.collect { if (it > 0 && pairing) onBrowserConnected() }
            }
        }
    }

    /** `ti0`: enter the code shown on the TV browser, or scan its QR code. */
    private fun showCodeSheet() {
        val b = BottomSheetCodeBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this).apply { setContentView(b.root) }
        b.pinView.onPinEntered = { code -> pair(code) }
        b.tvScanQr.setOnClickListener {
            dialog.dismiss()
            scan.launch(Intent(this, QrScanActivity::class.java))
        }
        sheet = dialog
        dialog.show()
    }

    /** `u()` + `mk4.i`. */
    private fun pair(code: String) {
        sheet?.dismiss()
        pairing = true
        showState(binding.lottieConnecting, R.string.connecting)
        MirrorTargets.showOnWebPage(code, WebReceiver.address(this)) { ok ->
            runOnUiThread { if (!ok && !isDestroyed) onFailed() }
        }
    }

    /** `KEY_CONNECTED_WEB` → `t()`. */
    private fun onBrowserConnected() {
        pairing = false
        showState(binding.lottieConnected, R.string.connected_successfully) {
            TvConnect.webConnected()
            setResult(RESULT_OK)
            finish()
        }
    }

    /** `s()`. */
    private fun onFailed() {
        pairing = false
        showState(binding.lottieConnectFail, R.string.connect_fail) { binding.llConnected.isVisible = false }
    }

    private fun showState(anim: LottieAnimationView, text: Int, onEnd: (() -> Unit)? = null) = with(binding) {
        llConnected.visibility = View.VISIBLE
        listOf(lottieConnecting, lottieConnected, lottieConnectFail).forEach { it.isVisible = it === anim }
        tvStatusConnect.setText(text)
        if (onEnd == null) return@with
        anim.removeAllAnimatorListeners()
        anim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = onEnd()
        })
        anim.playAnimation()
    }
}

package com.casttotv.screen.mirroring.ui.activities.function.connect

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.Window
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivitySearchTvBinding
import com.casttotv.screen.mirroring.databinding.DialogTurnOnWifiBinding
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.howto.HowToUseActivity
import com.casttotv.screen.mirroring.ui.dialogs.RateDialog
import com.connectsdk.core.Util
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.DiscoveryManagerListener
import com.connectsdk.service.command.ServiceCommandError

/** Port of `SearchTVActivity`: discovers TVs on the LAN and connects through ConnectSDK. */
class SearchTVActivity : BaseActivity(), DiscoveryManagerListener {

    private lateinit var binding: ActivitySearchTvBinding
    private lateinit var adapter: DeviceAdapter
    private lateinit var wifiDialog: Dialog
    private lateinit var connector: TvConnector

    /** `FileBrowserActivity` result: a browser receiver counts as a connected TV. */
    private val webCast = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            renderWifi(info?.state == NetworkInfo.State.CONNECTED)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchTvBinding.inflate(layoutInflater)
        setContentView(binding.root)
        DiscoveryManager.getInstance().addListener(this)
        connector = TvConnector(this, { onConnected() }, ::onConnectionFailed)

        with(binding.include9) {
            tvTitle.text = getText(R.string.connect)
            castConnect.visibility = View.GONE
            castPremium.visibility = View.GONE
            imvHowUse.visibility = View.VISIBLE
            imvBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            imvHowUse.setOnClickListener { open(HowToUseActivity::class.java) { putExtra(HowToUseActivity.EXTRA_TYPE, 1) } }
        }
        binding.llWebCast.setOnClickListener { webCast.launch(Intent(this, com.casttotv.screen.mirroring.ui.activities.function.browser.FileBrowserActivity::class.java)) }

        wifiDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val dialogBinding = DialogTurnOnWifiBinding.inflate(layoutInflater)
            setContentView(dialogBinding.root)
            dialogBinding.llTurnOn.setOnClickListener { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        }

        adapter = DeviceAdapter(::onDeviceClicked)
        binding.rcvListDevice.layoutManager = LinearLayoutManager(this)
        binding.rcvListDevice.adapter = adapter

        @Suppress("DEPRECATION")
        val filter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        ContextCompat.registerReceiver(this, wifiReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onBackPressedDispatcher.addCallback(this) {
            if (TvConnect.isConnected) setResult(RESULT_OK)
            finish()
        }
        refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        DiscoveryManager.getInstance().removeListener(this)
        unregisterReceiver(wifiReceiver)
    }

    private fun refresh() = adapter.submit(group(DiscoveryManager.getInstance().compatibleDevices.values))

    override fun onDeviceAdded(manager: DiscoveryManager, device: ConnectableDevice) = Util.runOnUI(::refresh)
    override fun onDeviceUpdated(manager: DiscoveryManager, device: ConnectableDevice) = Util.runOnUI(::refresh)
    override fun onDeviceRemoved(manager: DiscoveryManager, device: ConnectableDevice) = Util.runOnUI(::refresh)
    override fun onDiscoveryFailed(manager: DiscoveryManager, error: ServiceCommandError) = Unit

    private fun renderWifi(connected: Boolean) = with(binding) {
        svNotDevice.isVisible = !connected
        txtClick.isVisible = connected
        imgAnimLoading.isVisible = connected
        imvNoWifi.isVisible = !connected
        tvNameWifiConnect.isVisible = !connected
        if (connected) {
            runCatching { wifiDialog.dismiss() }
            refresh()
        } else {
            tvNameWifiConnect.setText(R.string.no_internet)
            runCatching { if (!wifiDialog.isShowing) wifiDialog.show() }
        }
    }

    private fun group(devices: Collection<ConnectableDevice>): List<SmartTv> {
        val rows = TvConnector.group(devices)
        binding.svNotDevice.isVisible = rows.isEmpty()
        binding.rlRv.isVisible = rows.isNotEmpty()
        return rows
    }

    private fun onDeviceClicked(tv: SmartTv) = connector.connect(tv)

    /** `yw4` case 9 + `u()`. */
    private fun onConnected() {
        with(binding) {
            llConnected.visibility = View.VISIBLE
            lottieConnecting.visibility = View.GONE
            lottieConnectFail.visibility = View.GONE
            lottieConnected.visibility = View.VISIBLE
            txtStatusConnect.setText(R.string.connected_successfully)
            lottieConnected.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    llConnected.visibility = View.GONE
                    if (!RateDialog.maybeShow(this@SearchTVActivity) { onBackPressedDispatcher.onBackPressed() }) {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
            lottieConnected.playAnimation()
        }
    }

    /** `hq5.onConnectionFailed`. */
    private fun onConnectionFailed() {
        with(binding) {
            llConnected.visibility = View.VISIBLE
            lottieConnecting.visibility = View.GONE
            lottieConnectFail.visibility = View.VISIBLE
            lottieConnected.visibility = View.GONE
            txtStatusConnect.setText(R.string.text_connected_fail)
            lottieConnectFail.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    llConnected.visibility = View.GONE
                }
            })
            lottieConnectFail.playAnimation()
        }
    }

    companion object {
        val fireTvIp get() = TvConnector.fireTvIp

        fun open(activity: Activity) {
            activity.startActivity(Intent(activity, SearchTVActivity::class.java))
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}

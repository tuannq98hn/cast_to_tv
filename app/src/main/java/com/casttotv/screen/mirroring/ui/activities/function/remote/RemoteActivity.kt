package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityRemoteBinding
import com.casttotv.screen.mirroring.tv.DeviceType
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.function.connect.SearchTVActivity
import com.casttotv.screen.mirroring.ui.dialogs.DisconnectDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Port of `RemoteActivity`: brand remote (page 0) + channels (page 1), with remote/touch/number tabs. */
class RemoteActivity : BaseActivity() {

    enum class Mode { REMOTE, TOUCH, NUMBER }

    private lateinit var binding: ActivityRemoteBinding
    private val _mode = MutableStateFlow(Mode.REMOTE)

    /** Replaces the `tab_remote` / `tab_touch` / `tab_number` EventBus messages. */
    val mode: StateFlow<Mode> = _mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding.include) {
            imvBack.setImageResource(R.drawable.ic_back_arrow_white)
            tvTitle.setTextColor(ContextCompat.getColor(this@RemoteActivity, R.color.white))
            castConnect.visibility = View.VISIBLE
            castPremium.visibility = View.GONE
            imvBack.setOnClickListener { finish() }
            castConnect.setOnClickListener {
                if (TvConnect.isConnected) DisconnectDialog(this@RemoteActivity, TvConnect.deviceName).show()
                else SearchTVActivity.open(this@RemoteActivity)
            }
        }
        binding.llRemote.setOnClickListener { select(Tab.REMOTE) }
        binding.llTouchPad.setOnClickListener { select(Tab.TOUCH) }
        binding.llNumber.setOnClickListener { select(Tab.NUMBER) }
        binding.llChannel.setOnClickListener { select(Tab.CHANNEL) }
        buildPages()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TvConnect.events.collect {
                    buildPages()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        renderHeader()
    }

    private enum class Tab { REMOTE, TOUCH, NUMBER, CHANNEL }

    private var tab = Tab.REMOTE

    /** `t()`: page 0 is chosen from the connected TV's brand. */
    private fun buildPages() {
        val remote: Fragment = when (TvConnect.device?.let(TvConnect::typeOf)) {
            DeviceType.LG -> LgRemoteFragment()
            DeviceType.SAMSUNG -> SamsungRemoteFragment()
            DeviceType.SONY -> SonyRemoteFragment()
            DeviceType.FIRE_TV -> FireTvRemoteFragment()
            null, DeviceType.ROKU -> RokuRemoteFragment()
            else -> if (isAndroidTv()) AndroidTvRemoteFragment() else RokuRemoteFragment()
        }
        binding.vpHomeMain.adapter = object : FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getCount() = 2
            override fun getItem(position: Int): Fragment = if (position == 0) remote else ChannelFragment()
        }
        binding.vpHomeMain.offscreenPageLimit = 2
        binding.vpHomeMain.pagingEnabled = false
        select(tab)
    }

    /** `hq4.t()`: an Android TV service is part of the connected device. */
    private fun isAndroidTv() = TvConnect.deviceAndroidTv?.connectedServiceNames?.lowercase()?.contains("androidtv") == true ||
        TvConnect.device?.connectedServiceNames?.lowercase()?.contains("androidtv") == true

    /** `v()`. */
    private fun select(tab: Tab) = with(binding) {
        this@RemoteActivity.tab = tab
        imvRemote.setImageResource(R.drawable.ic_tab_remote_off)
        imvChannel.setImageResource(R.drawable.ic_tab_channel_off)
        imvNumber.setImageResource(R.drawable.ic_tab_number_off)
        imvTouchPad.setImageResource(R.drawable.ic_tab_touch_off)
        listOf(llRemote, llNumber, llTouchPad, llChannel).forEach { it.setBackgroundResource(R.color.transparent) }
        val type = TvConnect.device?.let(TvConnect::typeOf)
        llNumber.isVisible = TvConnect.isConnected && type != DeviceType.ROKU && type != DeviceType.FIRE_TV
        renderHeader()
        when (tab) {
            Tab.REMOTE -> {
                vpHomeMain.currentItem = 0
                imvRemote.setImageResource(R.drawable.ic_tab_remote_on)
                llRemote.setBackgroundResource(R.drawable.bg_item_tab)
                _mode.value = Mode.REMOTE
            }
            Tab.TOUCH -> {
                vpHomeMain.currentItem = 0
                imvTouchPad.setImageResource(R.drawable.ic_tab_touch_on)
                llTouchPad.setBackgroundResource(R.drawable.bg_item_tab)
                _mode.value = Mode.TOUCH
            }
            Tab.NUMBER -> {
                vpHomeMain.currentItem = 0
                imvNumber.setImageResource(R.drawable.ic_tab_number_on)
                llNumber.setBackgroundResource(R.drawable.bg_item_tab)
                _mode.value = Mode.NUMBER
            }
            Tab.CHANNEL -> {
                include.tvTitle.setText(R.string.channel)
                vpHomeMain.currentItem = 1
                imvChannel.setImageResource(R.drawable.ic_tab_channel_on)
                llChannel.setBackgroundResource(R.drawable.bg_item_tab)
            }
        }
    }

    private fun renderHeader() = with(binding.include) {
        if (tab == Tab.CHANNEL) return@with
        tvTitle.text = if (TvConnect.isConnected) TvConnect.deviceName else getString(R.string.remote)
        castConnect.setImageResource(if (TvConnect.isConnected) R.drawable.ic_header_connected else R.drawable.ic_header_not_connect)
    }
}

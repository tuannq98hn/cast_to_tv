package com.casttotv.screen.mirroring.ui.activities.new_intro

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.Premium
import com.casttotv.screen.mirroring.databinding.ActivityNewIntroBinding
import com.casttotv.screen.mirroring.databinding.FragmentNewIntroBinding
import com.casttotv.screen.mirroring.databinding.ItemDeviceIntroBinding
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.function.connect.SmartTv
import com.casttotv.screen.mirroring.ui.activities.function.connect.TvConnector
import com.casttotv.screen.mirroring.ui.activities.function.connect.icon
import com.casttotv.screen.mirroring.ui.activities.main.MainActivity
import com.casttotv.screen.mirroring.ui.activities.purchase.PurchaseActivity
import com.connectsdk.core.Util
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.DiscoveryManagerListener
import com.connectsdk.service.command.ServiceCommandError

/**
 * Port of `NewIntroActivity`: welcome → turn on TV → find & connect a TV → all set (yearly offer).
 * Pages are the `vd4` fragment layout (`fragment_new_intro`), state from `NewIntroViewModel`.
 */
class NewIntroActivity : BaseActivity(), DiscoveryManagerListener {

    private lateinit var binding: ActivityNewIntroBinding
    private val handler = Handler(Looper.getMainLooper())
    private var progress = 6
    private var progressDone = false
    private var devices: List<SmartTv> = emptyList()
    private var selected = 0
    private var skipIap = false
    private var yearlyPrice: String? = null
    private lateinit var connector: TvConnector
    private val pages = PageAdapter()

    /** `xi3` case 14: fake discovery progress, 1 %/50 ms, then one more second. */
    private val tick = object : Runnable {
        override fun run() {
            if (progress > 50) binding.tvProgress.setTextColor(-1)
            if (progress == 101) {
                binding.clProgress.isVisible = false
                binding.tvProgress.isVisible = false
                binding.btnNext.isVisible = true
                if (devices.isEmpty()) {
                    next()
                } else {
                    binding.btnNext.setText(R.string.connect)
                    progressDone = true
                    pages.notifyItemChanged(2)
                }
            }
            if (progress >= 100) {
                progress++
                if (progress <= 101) handler.postDelayed(this, 1000)
                return
            }
            progress++
            binding.tvProgress.text = "$progress%"
            binding.progress.setCurrentProgress(progress)
            handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        skipIap = intent.getBooleanExtra(EXTRA_SKIP_IAP, false)
        connector = TvConnector(this, { showConnected() }, { showConnected() })
        DiscoveryManager.getInstance().addListener(this)
        refreshDevices()
        Premium.price(Premium.YEARLY_INTRO) { price ->
            runOnUiThread {
                yearlyPrice = price
                pages.notifyItemChanged(3)
            }
        }

        binding.viewPager.adapter = pages
        binding.viewPager.isUserInputEnabled = false
        binding.indicatorView.attachTo(binding.viewPager)
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = onPage(position)
        })
        binding.btnNext.setOnClickListener {
            when (binding.viewPager.currentItem) {
                0, 1 -> next()
                2 -> devices.getOrNull(selected)?.let(::connect)
                3 -> Premium.buy(this, Premium.YEARLY_INTRO) { ok -> if (ok) finishIntro() }
            }
        }
        binding.imvClose.setOnClickListener {
            if (skipIap) finishIntro() else {
                open(PurchaseActivity::class.java) { putExtra(PurchaseActivity.EXTRA_FROM, "NEW_INTRO_ACTIVITY") }
            }
        }
        onBackPressedDispatcher.addCallback(this) {
            when (binding.viewPager.currentItem) {
                0, 1 -> next()
                2 -> {
                    next()
                    binding.clProgress.isVisible = false
                    binding.tvProgress.isVisible = false
                    binding.btnNext.isVisible = true
                }
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        DiscoveryManager.getInstance().removeListener(this)
        super.onDestroy()
    }

    private fun next() = binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)

    /** `yw` case 6. */
    private fun onPage(position: Int) = with(binding) {
        when (position) {
            0 -> btnNext.setText(R.string.get_started)
            1 -> btnNext.setText(R.string.continue_s)
            2 -> {
                btnNext.isVisible = false
                clProgress.isVisible = true
                tvProgress.isVisible = true
                handler.post(tick)
            }
            3 -> {
                handler.removeCallbacks(tick)
                btnNext.setText(R.string.continue_s)
                imvClose.isVisible = true
            }
        }
    }

    private fun connect(tv: SmartTv) = with(binding) {
        TvConnect.nameDeviceSave = tv.name
        btnNext.isEnabled = false
        llConnect.isVisible = true
        lottieConnecting.isVisible = true
        lottieConnectFail.isVisible = false
        lottieConnected.isVisible = false
        tvStatusConnect.setText(R.string.connecting)
        connector.connect(tv)
    }

    /** `cc1` case 29 (used for both outcomes in the original), then `fd4`. */
    private fun showConnected() = with(binding) {
        llConnect.isVisible = true
        lottieConnecting.isVisible = false
        lottieConnected.isVisible = true
        tvStatusConnect.setText(R.string.connected_successfully)
        lottieConnected.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                lottieConnected.removeAllAnimatorListeners()
                next()
                llConnect.isVisible = false
                btnNext.isEnabled = true
            }
        })
        lottieConnected.playAnimation()
    }

    private fun finishIntro() {
        open(MainActivity::class.java)
        finish()
    }

    private fun refreshDevices() {
        devices = TvConnector.group(DiscoveryManager.getInstance().compatibleDevices.values)
        if (::binding.isInitialized) pages.notifyItemChanged(2)
    }

    override fun onDeviceAdded(manager: DiscoveryManager, device: ConnectableDevice) = Util.runOnUI(::refreshDevices)
    override fun onDeviceUpdated(manager: DiscoveryManager, device: ConnectableDevice) = Util.runOnUI(::refreshDevices)
    override fun onDeviceRemoved(manager: DiscoveryManager, device: ConnectableDevice) = Util.runOnUI(::refreshDevices)
    override fun onDiscoveryFailed(manager: DiscoveryManager, error: ServiceCommandError) = Util.runOnUI(::refreshDevices)

    /** `vd4` per `TypeIntro`. */
    private inner class PageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = 4

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(FragmentNewIntroBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val page = FragmentNewIntroBinding.bind(holder.itemView)
            fun html(id: Int) = Html.fromHtml(getString(id), Html.FROM_HTML_MODE_LEGACY)
            when (position) {
                0 -> {
                    page.imgBanner.setImageResource(R.drawable.img_welcome_intro)
                    page.tvTitle.setText(R.string.title_welcome_new_intro)
                    page.tvDescription.text = html(R.string.des_welcome_new_intro)
                }
                1 -> {
                    page.imgBanner.setImageResource(R.drawable.img_turn_on_intro)
                    page.tvTitle.setText(R.string.title_turn_on_new_intro)
                    page.tvDescription.text = html(R.string.des_turn_on_new_intro)
                }
                2 -> {
                    val showList = progressDone && devices.isNotEmpty()
                    page.imgBanner.setImageResource(if (showList) R.drawable.img_list_device_intro else R.drawable.img_connect_intro)
                    page.tvTitle.setText(R.string.title_connect_loading_new_intro)
                    page.tvDescription.text = html(R.string.des_connect_loading_new_intro)
                    page.tvTitle.isVisible = !showList
                    page.tvDescription.isVisible = !showList
                    page.llLoading.isVisible = showList
                    page.rcvList.isVisible = showList
                    page.rcvList.layoutManager = LinearLayoutManager(this@NewIntroActivity)
                    page.rcvList.adapter = DeviceAdapter()
                }
                else -> {
                    page.llLoading.isVisible = false
                    page.rcvList.isVisible = false
                    page.tvTitle.isVisible = true
                    page.tvDescription.isVisible = true
                    page.tvDescription1.isVisible = true
                    page.tvTitle.setText(R.string.title_done_new_intro)
                    page.imgBanner.setImageResource(R.drawable.img_done_intro)
                    page.tvDescription1.text = html(R.string.des_done_new_intro_1)
                    yearlyPrice?.let { page.tvDescription.text = getString(R.string.des_done_new_intro, it) }
                }
            }
        }
    }

    /** `gg1`. */
    private inner class DeviceAdapter : RecyclerView.Adapter<DeviceAdapter.Holder>() {
        inner class Holder(val binding: ItemDeviceIntroBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemCount() = devices.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemDeviceIntroBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
            val tv = devices[position]
            tvNameDevice.text = tv.name
            textDeviceType.text = tv.devices.firstOrNull()?.connectedServiceNames.orEmpty()
            imvBranchTV.setImageResource(TvConnect.typeOf(tv.devices[0]).icon)
            if (position == selected) TvConnect.nameTvContain = tv.name
            root.setBackgroundResource(if (position == selected) R.drawable.bg_border12_stroke_primary else R.drawable.bg_border12_secondary)
            root.setOnClickListener {
                selected = holder.bindingAdapterPosition
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        const val EXTRA_SKIP_IAP = "skip_iap"
    }
}

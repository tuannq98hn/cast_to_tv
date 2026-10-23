package com.casttotv.screen.mirroring.ui.activities.channel_roku

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityInstallChannelRokuBinding
import com.casttotv.screen.mirroring.databinding.ItemInstallChannelRokuBinding
import com.casttotv.screen.mirroring.databinding.LayoutNumberAddChannelBinding
import com.casttotv.screen.mirroring.tv.RokuEcp
import com.casttotv.screen.mirroring.tv.TvConnect

/**
 * Port of `InstallChannelRokuActivity`: walks the user through adding the "Cast to TV" Roku channel (714008):
 * open its store page, select "Add channel", type the Roku PIN from the phone, wait until it shows up in `query/apps`.
 */
class InstallChannelRokuActivity : BaseActivity() {

    private lateinit var binding: ActivityInstallChannelRokuBinding
    private lateinit var keypad: LayoutNumberAddChannelBinding
    private val handler = Handler(Looper.getMainLooper())
    private var step = 0
    private var digits = 0
    private val pages = listOf(
        R.drawable.img_channel_1, R.drawable.img_channel_2, R.drawable.img_channel_3,
        R.drawable.img_channel_4, R.drawable.img_channel_4_1, R.drawable.img_channel_5,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallChannelRokuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        keypad = LayoutNumberAddChannelBinding.bind(binding.llKeyBoardNumber.root)
        binding.viewPager.pagingEnabled = false
        binding.viewPager.adapter = object : PagerAdapter() {
            override fun getCount() = pages.size
            override fun isViewFromObject(view: View, obj: Any) = view === obj
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val item = ItemInstallChannelRokuBinding.inflate(layoutInflater, container, false)
                item.image.setImageResource(pages[position])
                container.addView(item.root)
                return item.root
            }

            override fun destroyItem(container: ViewGroup, position: Int, obj: Any) = container.removeView(obj as View)
        }
        binding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) = onPage(position)
        })
        binding.btnAction.setOnClickListener { onAction() }
        binding.tvLater.setOnClickListener { finish() }
        mapOf(
            keypad.rltZero to '0', keypad.rltOne to '1', keypad.rltTwo to '2', keypad.rltThree to '3', keypad.rltFour to '4',
            keypad.rltFive to '5', keypad.rltSix to '6', keypad.rltSeven to '7', keypad.rltEight to '8', keypad.rltNine to '9',
        ).forEach { (view, digit) -> view.setOnClickListener { typeDigit(digit) } }
        keypad.rltDelete.setOnClickListener {
            if (digits == 0) return@setOnClickListener
            digits--
            binding.btnAction.setBackgroundResource(R.drawable.bg_button_all_off)
            RokuEcp.keypress(RokuEcp.Key.BACKSPACE)
        }
        keypad.rltReset.setOnClickListener {
            repeat(digits) { RokuEcp.keypress(RokuEcp.Key.BACKSPACE) }
            digits = 0
            binding.btnAction.setBackgroundResource(R.drawable.bg_button_all_off)
        }
        onPage(0)
        dots(1)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /** `t(String)`: each PIN digit is typed on the TV. */
    private fun typeDigit(digit: Char) {
        if (digits < 4) digits++
        if (digits == 4) binding.btnAction.setBackgroundResource(R.drawable.bg_button_all)
        RokuEcp.literal(digit)
    }

    /** `fz2` case 0: the main button advances the guide. */
    private fun onAction() = with(binding) {
        val label = btnAction.text.toString()
        if (step == 3 && digits != 4) return@with
        if (label == getString(R.string.installing)) return@with
        if (step < 6) step++
        when (step) {
            1 -> {
                dots(2)
                RokuEcp.openStore()
                handler.postDelayed({ RokuEcp.keypress(RokuEcp.Key.SELECT) }, 2500)
            }
            2 -> dots(3)
            4 -> {
                digits = 0
                RokuEcp.keypress(RokuEcp.Key.SELECT)
                llNumber.isVisible = false
                llContentAdd.isVisible = true
                waitForChannel()
            }
            5 -> dots(4)
            6 -> when (label) {
                getString(R.string.try_again) -> {
                    step = 0
                    viewPager.currentItem = 0
                    return@with
                }
                else -> {
                    RokuEcp.keypress(RokuEcp.Key.SELECT)
                    finish()
                    return@with
                }
            }
        }
        viewPager.currentItem = step
    }

    /** `i13.l` + `iz2` case 1: poll `query/apps` until 714008 is installed, then show the success page. */
    private fun waitForChannel(tries: Int = 30) {
        RokuEcp.refreshChannels {
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                if (RokuEcp.CHANNEL_ID in TvConnect.rokuChannels) {
                    binding.btnAction.setText(R.string.continue_s)
                    step = 5
                    binding.viewPager.currentItem = 5
                } else if (tries > 0) {
                    handler.postDelayed({ waitForChannel(tries - 1) }, 2000)
                } else {
                    binding.btnAction.setText(R.string.cancel)
                    step = 5
                    binding.viewPager.currentItem = 5
                }
            }
        }
    }

    /** `ww` case 1: copy per page. */
    private fun onPage(position: Int) = with(binding) {
        when (position) {
            0 -> {
                tvTitle.setText(R.string.title_install_1)
                tvDescription.setText(R.string.des_channel_roku_1)
                btnAction.setText(R.string.tap_to_start)
                tvLater.isVisible = false
            }
            1 -> {
                tvTitle.setText(R.string.title_install_2)
                tvDescription.setText(R.string.des_channel_roku_2)
                btnAction.setText(R.string.add_channel)
                tvLater.isVisible = false
            }
            2 -> {
                tvTitle.setText(R.string.title_install_3_1)
                tvDescription.setText(R.string.des_channel_roku_3_1)
                btnAction.setText(R.string.i_got_it)
            }
            3 -> {
                llContentAdd.visibility = View.INVISIBLE
                llNumber.isVisible = true
                btnAction.setText(R.string.done)
                btnAction.setBackgroundResource(R.drawable.bg_button_all_off)
            }
            4 -> {
                btnAction.setText(R.string.installing)
                llContentAdd.isVisible = true
                llNumber.isVisible = false
                tvDot.isVisible = true
            }
            5 -> {
                if (btnAction.text.toString() == getString(R.string.cancel)) {
                    btnAction.setText(R.string.try_again)
                    tvDescription.setText(R.string.des_channel_roku_4_error)
                    tvTitle.setText(R.string.title_install_4_error)
                    tvLater.isVisible = true
                } else {
                    tvTitle.setText(R.string.title_install_4)
                    tvDescription.setText(R.string.des_channel_roku_4)
                    btnAction.setText(R.string.continue_s)
                    tvLater.isVisible = false
                }
                tvDot.isVisible = false
            }
        }
    }

    /** `u(int)`. */
    private fun dots(selected: Int) = with(binding) {
        listOf(imvDot1, imvDot2, imvDot3, imvDot4).forEachIndexed { i, dot ->
            dot.setImageResource(if (i + 1 == selected) R.drawable.ic_dot_selected else R.drawable.ic_dot_off)
        }
    }

    companion object {
        /** `MainActivity.q()`: on a Roku without the channel, open this guide; otherwise launch the channel. */
        fun checkRoku(activity: BaseActivity): Boolean {
            if (TvConnect.currentType() != com.casttotv.screen.mirroring.tv.DeviceType.ROKU || !TvConnect.isConnected) return false
            val installed = RokuEcp.CHANNEL_ID in TvConnect.rokuChannels
            if (!installed) activity.open(InstallChannelRokuActivity::class.java)
            RokuEcp.launch()
            return !installed
        }
    }
}

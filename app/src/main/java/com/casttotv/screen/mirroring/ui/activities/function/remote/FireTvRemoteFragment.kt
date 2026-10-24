package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.DialogConnectFireTvBinding
import com.casttotv.screen.mirroring.databinding.DialogKeyboardFireTvBinding
import com.casttotv.screen.mirroring.databinding.FragmentRemoteFireTvNBinding
import com.casttotv.screen.mirroring.tv.FireTvAdb
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.function.connect.SearchTVActivity

/** Port of `RemoteFireTVFragment`: ADB shell key events on the Fire TV found through DIAL. */
class FireTvRemoteFragment : BaseRemoteFragment() {

    private var _binding: FragmentRemoteFireTvNBinding? = null
    private val binding get() = _binding!!

    private val host get() = SearchTVActivity.fireTvIp.ifEmpty { TvConnect.device?.ipAddress.orEmpty() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRemoteFireTvNBinding.inflate(inflater, container, false)
        with(binding) {
            bindPressedBackground(
                imvHomeFireTV, R.drawable.bg_control,
                mapOf(
                    rltRemoteUp to R.drawable.bg_control_up, rltRemoteDown to R.drawable.bg_control_down,
                    rltRemoteLeft to R.drawable.bg_control_left, rltRemoteRight to R.drawable.bg_control_right,
                    rltRemoteOk to R.drawable.bg_control_ok,
                ),
            )
            ctTouchpadLG.setOnTouchListener(
                SwipeTouchListener(requireContext(), onSwipe = { key(it.toAndroidKey()) }, onTap = { key(23) }),
            )
            mapOf(
                rltRemoteUp to 19, rltRemoteDown to 20, rltRemoteLeft to 21, rltRemoteRight to 22, rltRemoteOk to 23,
                imvFireTVBack to 4, imvFireTVHome to 3, imvFireTVMenu to 82, imvFireTVNext to 90, imvFireTVPlay to 85,
                imvFireTVPre to 89,
            ).forEach { (view, code) -> view.setOnClickListener { key(code) } }
            imvFireTVSearch.setOnClickListener {
                key(84)
                showKeyboard()
            }
            imvFireTVKeyBoard.setOnClickListener { showKeyboard() }
        }
        firstRunGuide()
        connect()
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun showMode(mode: RemoteActivity.Mode) = with(binding) {
        val touch = mode == RemoteActivity.Mode.TOUCH
        ctTouchpadLG.visibility = if (touch) View.VISIBLE else View.GONE
        ctRemoteLG.visibility = if (touch) View.INVISIBLE else View.VISIBLE
    }

    private fun key(code: Int) = connected { FireTvAdb.shell("input keyevent $code") }

    /** `GuideFireTVActivity` is shown once (`FIRST_FIRETV`). */
    private fun firstRunGuide() {
        val prefs = requireContext().getSharedPreferences("app-content", 0)
        if (!prefs.getBoolean("FIRST_FIRETV", false)) {
            prefs.edit { putBoolean("FIRST_FIRETV", true) }
            startActivity(Intent(requireContext(), GuideFireTVActivity::class.java))
        }
    }

    /** `yg1` mode 0: "Connecting to <ip>" until the TV accepts the ADB key. */
    private fun connect() {
        if (host.isEmpty()) return
        val dialogBinding = DialogConnectFireTvBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialogBinding.tvConnectFireTV.text = getString(R.string.connecting_to) + " " + host
        dialogBinding.tvCancel.setOnClickListener { dialog.dismiss() }
        if (!FireTvAdb.isConnected) dialog.show()
        FireTvAdb.connect(requireContext().applicationContext, host) {
            activity?.runOnUiThread { if (dialog.isShowing) dialog.dismiss() }
        }
    }

    /** `k73`: typed characters become `input text`, delete becomes KEYCODE_DEL. */
    private fun showKeyboard() {
        val dialogBinding = DialogKeyboardFireTvBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialogBinding.tvPinCodeOk.setOnClickListener { dialog.dismiss() }
        dialogBinding.edtPinCodeInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) FireTvAdb.shell("input keyevent 67")
            false
        }
        dialogBinding.edtPinCodeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count <= before) return
                s?.subSequence(start + before, start + count)?.forEach { c ->
                    val text = when (c) {
                        ' ' -> "%s"
                        '#', '&', '(', ')', '?', '*' -> "\"$c\""
                        else -> c.toString()
                    }
                    FireTvAdb.shell("input text $text")
                }
            }
        })
        dialog.show()
    }
}

/** Android KeyEvent codes for the swipe pad (also used by the Android TV remote). */
fun Swipe.toAndroidKey() = when (this) {
    Swipe.UP -> 19
    Swipe.DOWN -> 20
    Swipe.LEFT -> 21
    Swipe.RIGHT -> 22
}

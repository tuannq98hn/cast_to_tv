package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.data.Premium
import com.casttotv.screen.mirroring.databinding.DialogFragmentTextInputBinding
import com.casttotv.screen.mirroring.databinding.FragmentRemoteBinding
import com.casttotv.screen.mirroring.tv.DeviceType
import com.casttotv.screen.mirroring.tv.RokuEcp
import com.casttotv.screen.mirroring.tv.RokuEcp.Key
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.dialogs.SimpleDialog
import com.connectsdk.service.capability.KeyControl

/** Port of `RemoteRokuFragment` (also the fallback remote when nothing is connected). */
class RokuRemoteFragment : BaseRemoteFragment() {

    private var _binding: FragmentRemoteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRemoteBinding.inflate(inflater, container, false)
        with(binding) {
            // VIP badges are decoration for free users; the private-listener badge is off unless remote config enabled it.
            listOf(imvVipPower, imvVipKeyboard, imvVipVoice).forEach { it.isVisible = !Premium.isActive }
            imvVipMute.visibility = View.GONE
            bindPressedBackground(
                ctRokuRemote, R.drawable.bg_control,
                mapOf(
                    llRokuUp to R.drawable.bg_control_up, llRokuDown to R.drawable.bg_control_down,
                    llRokuLeft to R.drawable.bg_control_left, llRokuRight to R.drawable.bg_control_right,
                    llRokuOk to R.drawable.bg_control_ok,
                ),
            )
            ctRokuTouchpad.setOnTouchListener(
                SwipeTouchListener(requireContext(), onSwipe = { key(it.toRoku()) }, onTap = { key(Key.SELECT) }),
            )
            mapOf(
                llRokuUp to Key.UP, llRokuDown to Key.DOWN, llRokuLeft to Key.LEFT, llRokuRight to Key.RIGHT,
                llRokuOk to Key.SELECT, ctRokuBack to Key.BACK, ctRokuHome to Key.HOME, ctRokuInfo to Key.INFO,
                ctRokuPre to Key.REV, ctRokuForward to Key.FWD, ctRokuPause to Key.PLAY, ctRokuMute to Key.VOLUME_MUTE,
                ctRokuVolumeDown to Key.VOLUME_DOWN, ctRokuVolumeUp to Key.VOLUME_UP,
            ).forEach { (view, k) -> view.setOnClickListener { key(k) } }
            ctRokuPower.setOnClickListener { button { TvConnect.device?.getCapability(KeyControl::class.java)?.power(null) } }
            ctRokuKeyBoard.setOnClickListener { button { showKeyboard() } }
            ctRokuVoice.setOnClickListener { button { listen(::typeText) } }
            ctHeadPhone.setOnClickListener {
                button { Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show() }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun showMode(mode: RemoteActivity.Mode) {
        val touch = mode == RemoteActivity.Mode.TOUCH
        binding.ctRokuTouchpad.visibility = if (touch) View.VISIBLE else View.INVISIBLE
        binding.ctRokuRemote.visibility = if (touch) View.INVISIBLE else View.VISIBLE
    }

    private fun key(key: Key) = button { RokuEcp.keypress(key) }

    /**
     * `onclick` preconditions: a web-browser session or a bare Chromecast has no Roku remote, so the user is sent
     * back to pick a TV; an unknown TV shows the "not supported" dialog.
     */
    private fun button(action: () -> Unit) {
        if (TvConnect.isConnectWeb || TvConnect.device?.connectedServiceNames?.lowercase()?.contains("chromecast") == true) {
            if (TvConnect.isConnectWeb) TvConnect.disconnect()
            com.casttotv.screen.mirroring.ui.activities.function.connect.SearchTVActivity.open(requireActivity())
            return
        }
        if (TvConnect.isConnected && TvConnect.device?.let(TvConnect::typeOf) == DeviceType.OTHER) {
            SimpleDialog.show(requireContext(), R.layout.dialog_not_support_tv, closeId = R.id.btnOk)
            return
        }
        connected(action)
    }

    /** Spoken or typed text goes out as `Lit_` key presses. */
    private fun typeText(text: String) = text.forEach(RokuEcp::literal)

    /** `yg1` mode 3: every typed character is sent immediately; delete sends Backspace. */
    private fun showKeyboard() {
        val dialogBinding = DialogFragmentTextInputBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext()).apply { setContentView(dialogBinding.root) }
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.tvTutorial.setOnClickListener { dialog.dismiss() }
        dialogBinding.textBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) RokuEcp.keypress(Key.ENTER)
            false
        }
        dialogBinding.textBox.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) RokuEcp.keypress(Key.BACKSPACE)
            false
        }
        dialogBinding.textBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > before) s?.subSequence(start + before, start + count)?.forEach(RokuEcp::literal)
            }
        })
        dialog.show()
    }
}

fun Swipe.toRoku(): Key = when (this) {
    Swipe.UP -> Key.UP
    Swipe.DOWN -> Key.DOWN
    Swipe.LEFT -> Key.LEFT
    Swipe.RIGHT -> Key.RIGHT
}

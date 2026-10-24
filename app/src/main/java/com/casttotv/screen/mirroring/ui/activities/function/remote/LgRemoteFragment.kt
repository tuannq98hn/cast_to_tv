package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.DialogKeyboardBinding
import com.casttotv.screen.mirroring.databinding.FragmentRemoteLgBinding
import com.casttotv.screen.mirroring.tv.TvConnect
import com.connectsdk.service.capability.KeyControl
import com.connectsdk.service.capability.MouseControl
import com.connectsdk.service.capability.PowerControl
import com.connectsdk.service.capability.TVControl
import com.connectsdk.service.capability.TextInputControl
import com.connectsdk.service.capability.VolumeControl
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Port of `RemoteLGFragment`: webOS through ConnectSDK capabilities. */
class LgRemoteFragment : BaseRemoteFragment() {

    private var _binding: FragmentRemoteLgBinding? = null
    private val binding get() = _binding!!
    private var powerOn = true
    private var muted = false
    private var threeD = false

    private inline fun <reified T : com.connectsdk.service.capability.CapabilityMethods> cap(): T? =
        TvConnect.device?.getCapability(T::class.java)

    private val keys get() = cap<KeyControl>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRemoteLgBinding.inflate(inflater, container, false)
        with(binding) {
            bindPressedBackground(
                imHomeLG, R.drawable.bg_control,
                mapOf(
                    rltRemoteUp to R.drawable.bg_control_up, rltRemoteDown to R.drawable.bg_control_down,
                    rltRemoteLeft to R.drawable.bg_control_left, rltRemoteRight to R.drawable.bg_control_right,
                    rltRemoteOk to R.drawable.bg_control_ok,
                ),
            )
            bindPressedBackground(
                ctVolLG, R.drawable.bg_vol_type0,
                mapOf(rltRemoteVolumeUp to R.drawable.bg_vol_type0_up, rltRemoteVolumeDown to R.drawable.bg_vol_type0_down),
            )
            bindPressedBackground(
                ctChlLG, R.drawable.bg_vol_type0,
                mapOf(rltRemoteChUp to R.drawable.bg_vol_type0_up, rltRemoteChDown to R.drawable.bg_vol_type0_down),
            )
            ctTouchpadLG.setOnTouchListener(
                PointerTouchListener(
                    onMove = { dx, dy -> cap<MouseControl>()?.move(dx, dy) },
                    onClick = { cap<MouseControl>()?.click() },
                    onScroll = { dx, dy -> cap<MouseControl>()?.scroll(dx, dy) },
                ),
            )

            ctPowerLG.setOnClickListener {
                tap {
                    val power = cap<PowerControl>() ?: return@tap
                    if (powerOn) power.powerOff(null) else power.powerOn(null)
                    powerOn = !powerOn
                }
            }
            rltRemoteVolumeUp.setOnClickListener { tap { volume(true) } }
            rltRemoteVolumeDown.setOnClickListener { tap { volume(false) } }
            rltRemoteUp.setOnClickListener { tap { keys?.up(null) } }
            rltRemoteLeft.setOnClickListener { tap { keys?.left(null) } }
            rltRemoteOk.setOnClickListener { tap { keys?.sendKeyCode(KeyControl.KeyCode.ENTER, null) } }
            rltRemoteRight.setOnClickListener { tap { keys?.right(null) } }
            rltRemoteDown.setOnClickListener { tap { keys?.down(null) } }
            rltRemoteChUp.setOnClickListener { tap { channel(true) } }
            rltRemoteChDown.setOnClickListener { tap { channel(false) } }
            rltRemoteVolumeMute.setOnClickListener {
                tap {
                    muted = !muted
                    cap<VolumeControl>()?.setMute(muted, null)
                }
            }
            // The original maps the back buttons to the same action as Home (`n = 14`).
            listOf(ctBackLG, viewReturn, rltRemoteHome).forEach { it.setOnClickListener { tap { keys?.home(null) } } }
            rltRemoteEnter.setOnClickListener { tap { keys?.sendKeyCode(KeyControl.KeyCode.ENTER, null) } }
            llSetting.setOnClickListener { tap { keys?.setting_lg(null) } }
            ctGuide.setOnClickListener { tap { keys?.guide_lg(null) } }
            listOf(ctExitLG, viewExit).forEach { it.setOnClickListener { tap { keys?.exit_lg(null) } } }
            rltRemote3dModel.setOnClickListener {
                tap {
                    threeD = !threeD
                    cap<TVControl>()?.set3DEnabled(threeD, null)
                }
            }
            rltRemoteKeyboard.setOnClickListener { tap { showKeyboard() } }
            rltRemoteProgramList.setOnClickListener { tap { cap<TVControl>()?.getProgramList(null) } }
            ctDot.setOnClickListener { showColorKeys() }
            bindNumbers(com.casttotv.screen.mirroring.databinding.LayoutNumberBinding.bind(ctNumberLG)) { digit -> keys?.sendKeyCode(KeyControl.KeyCode.createFromInteger(digit), null) }
        }
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun showMode(mode: RemoteActivity.Mode) = with(binding) {
        ctRemoteLG.visibility = if (mode == RemoteActivity.Mode.REMOTE) View.VISIBLE else View.INVISIBLE
        ctTouchpadLG.visibility = if (mode == RemoteActivity.Mode.TOUCH) View.VISIBLE else View.GONE
        ctNumberLG.visibility = if (mode == RemoteActivity.Mode.NUMBER) View.VISIBLE else View.GONE
    }

    /** `onclick` returns early when the TV has no KeyControl. */
    private fun tap(action: () -> Unit) {
        if (TvConnect.device != null && keys == null) return
        connected(action)
    }

    /** `p(boolean)`: 1% steps through VolumeControl. */
    private fun volume(up: Boolean) {
        val control = cap<VolumeControl>() ?: return
        TvConnect.volume = if (up) minOf(TvConnect.volume + 0.01f, 1f) else maxOf(TvConnect.volume - 0.01f, 0f)
        control.setVolume(TvConnect.volume, null)
    }

    private fun channel(up: Boolean) {
        val device = TvConnect.device ?: return
        if (up && device.hasCapability(TVControl.Channel_Up)) cap<TVControl>()?.channelUp(null)
        if (!up && device.hasCapability(TVControl.Channel_Down)) cap<TVControl>()?.channelDown(null)
    }

    /** `bottom_sheet_layout_dot`: the four color keys. */
    private fun showColorKeys() {
        val sheet = BottomSheetDialog(requireContext())
        sheet.setContentView(R.layout.bottom_sheet_layout_dot)
        mapOf<Int, (KeyControl) -> Unit>(
            R.id.llRedLG to { it.red(null) }, R.id.llGreenLG to { it.green(null) },
            R.id.llYellowLG to { it.yellow(null) }, R.id.llBlueLG to { it.blue(null) },
        ).forEach { (id, press) ->
            sheet.findViewById<LinearLayout>(id)?.setOnClickListener { tap { keys?.let(press) } }
        }
        sheet.setOnShowListener {
            sheet.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(android.R.color.transparent)
        }
        sheet.show()
    }

    /** `i73`: text typed here is pushed to the focused field on the TV. */
    private fun showKeyboard() {
        val dialogBinding = DialogKeyboardBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val input = { TvConnect.device?.getCapability(TextInputControl::class.java) }
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnSend.setOnClickListener {
            input()?.sendEnter()
            dialog.dismiss()
        }
        dialogBinding.edtTextInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > before) input()?.sendText(s?.subSequence(start + before, start + count).toString())
                else repeat(before - count) { input()?.sendDelete() }
            }
        })
        dialog.show()
    }
}

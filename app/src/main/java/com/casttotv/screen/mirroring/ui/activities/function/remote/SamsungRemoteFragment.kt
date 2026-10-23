package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.FragmentRemoteSsBinding
import com.casttotv.screen.mirroring.tv.SamsungRemote
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.function.connect.SearchTVActivity
import com.connectsdk.service.capability.MouseControl
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

/** Port of `RemoteSamsungFragment`: Tizen remote websocket keys, swipe pad and mouse pad. */
class SamsungRemoteFragment : BaseRemoteFragment() {

    private var _binding: FragmentRemoteSsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRemoteSsBinding.inflate(inflater, container, false)
        with(binding) {
            bindPressedBackground(
                imvHome, R.drawable.bg_btn_home_ss,
                mapOf(
                    llRemoteUpSs to R.drawable.bg_btn_home_ss_up, llRemoteDownSs to R.drawable.bg_btn_home_ss_down,
                    llRemoteLeftSs to R.drawable.bg_btn_home_ss_left, llRemoteRightSs to R.drawable.bg_btn_home_ss_right,
                    llRemoteOkSs to R.drawable.bg_btn_home_ss_ok,
                ),
            )
            bindPressedBackground(
                ctVol, R.drawable.bg_vol_type0,
                mapOf(llRemoteVolUpSs to R.drawable.bg_vol_type0_up, llRemoteVolDowSs to R.drawable.bg_vol_type0_down),
            )
            bindPressedBackground(
                ctCh, R.drawable.bg_vol_type0,
                mapOf(llRemoteChUpSs to R.drawable.bg_vol_type0_up, llRemoteChDownSs to R.drawable.bg_vol_type0_down),
            )
            mapOf(
                ctExitSs to "KEYCODE_ESCAPE", viewExit to "KEYCODE_ESCAPE", ctRokuPower to "KEYCODE_POWER",
                ctToolSs to "KEY_SOURCE", llInputSamsung to "KEY_SOURCE", llRecord to "KEYCODE_MEDIA_RECORD",
                llRemoteChDownSs to "KEYCODE_CHANNEL_DOWN", llRemoteChUpSs to "KEYCODE_CHANNEL_UP",
                llRemoteChannelSs to "KEY_CH_LIST", llRemoteDownSs to "KEYCODE_DPAD_DOWN", llRemoteHomeSs to "KEYCODE_HOME",
                llRemoteInfoSs to "KEY_INFO", llRemoteLeftSs to "KEYCODE_DPAD_LEFT", llRemoteMuteSs to "KEYCODE_VOLUME_MUTE",
                llRemoteOkSs to "KEYCODE_ENTER", llRemoteProgramSs to "KEY_APP_LIST", llRemoteRightSs to "KEYCODE_DPAD_RIGHT",
                llRemoteSettingSs to "KEY_MENU", llRemoteUpSs to "KEYCODE_DPAD_UP", llRemoteVolDowSs to "KEYCODE_VOLUME_DOWN",
                llRemoteVolUpSs to "KEYCODE_VOLUME_UP", llRemoteReturn to "KEYCODE_BACK", viewReturn to "KEYCODE_BACK",
            ).forEach { (view, key) -> view.setOnClickListener { send(key) } }
            llTab.setOnClickListener { touchMode(mouse = false) }
            llMouse.setOnClickListener { touchMode(mouse = true) }
            ctDot.setOnClickListener { showColorKeys() }
            bindNumbers(com.casttotv.screen.mirroring.databinding.LayoutNumberBinding.bind(ctNumberSS)) { digit -> SamsungRemote.sendKey("KEYCODE_$digit") }
            touchMode(mouse = false)
        }
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun showMode(mode: RemoteActivity.Mode) = with(binding) {
        ctRemoteSS.visibility = if (mode == RemoteActivity.Mode.REMOTE) View.VISIBLE else View.INVISIBLE
        ctTouchpadSS.visibility = if (mode == RemoteActivity.Mode.TOUCH) View.VISIBLE else View.GONE
        ctNumberSS.visibility = if (mode == RemoteActivity.Mode.NUMBER) View.VISIBLE else View.GONE
    }

    /** `i(String)`: needs the websocket accepted on the TV, otherwise back to the TV search. */
    private fun send(key: String) {
        if (!TvConnect.isConnected) {
            SearchTVActivity.open(requireActivity())
            return
        }
        vibrate()
        SamsungRemote.sendKey(key)
    }

    /** `k(int)`: swipe pad (arrows) or mouse pad (`ProcessMouseDevice`). */
    private fun touchMode(mouse: Boolean) = with(binding) {
        llMouse.setBackgroundResource(if (mouse) R.drawable.bg_on_select_ss else R.drawable.bg_default_ss)
        llTab.setBackgroundResource(if (mouse) R.drawable.bg_default_ss else R.drawable.bg_on_select_ss)
        imvIconMouse.setImageResource(if (mouse) R.drawable.ic_tab_mouse_on else R.drawable.ic_tab_mouse_off)
        imvIconTouch.setImageResource(if (mouse) R.drawable.ic_touch_off_ss else R.drawable.ic_touch_on_ss)
        ctTouchpadSS.setBackgroundResource(if (mouse) R.drawable.img_touchpad_ss_mouse else R.drawable.img_touchpad_ss)
        ctTouchpadSS.setOnTouchListener(
            if (mouse) {
                PointerTouchListener(
                    onMove = { dx, dy -> SamsungRemote.send(mouseMove(dx, dy)) },
                    onClick = { SamsungRemote.send(JSONObject("""{"method":"ms.remote.control","params":{"Cmd":"LeftClick","TypeOfRemote":"ProcessMouseDevice"}}""")) },
                    onScroll = { dx, dy -> TvConnect.device?.getCapability(MouseControl::class.java)?.scroll(dx, dy) },
                )
            } else {
                SwipeTouchListener(requireContext(), onSwipe = { send(it.toSamsung()) }, onTap = { send("KEYCODE_ENTER") })
            },
        )
    }

    private fun mouseMove(dx: Double, dy: Double): JSONObject {
        val epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2011, 9, 1) }.timeInMillis / 1000
        return JSONObject(
            """{"method":"ms.remote.control","params":{"Cmd":"Move","Position":{"Time":"$epoch","x":$dx,"y":$dy},"TypeOfRemote":"ProcessMouseDevice"}}""",
        )
    }

    private fun showColorKeys() {
        val sheet = BottomSheetDialog(requireContext())
        sheet.setContentView(R.layout.bottom_sheet_layout_dot)
        mapOf(
            R.id.llRedLG to "KEYCODE_PROG_RED", R.id.llGreenLG to "KEYCODE_PROG_GREEN",
            R.id.llYellowLG to "KEYCODE_PROG_YELLOW", R.id.llBlueLG to "KEYCODE_PROG_BLUE",
        ).forEach { (id, key) -> sheet.findViewById<LinearLayout>(id)?.setOnClickListener { send(key) } }
        sheet.setOnShowListener {
            sheet.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(android.R.color.transparent)
        }
        sheet.show()
    }
}

fun Swipe.toSamsung() = when (this) {
    Swipe.UP -> "KEYCODE_DPAD_UP"
    Swipe.DOWN -> "KEYCODE_DPAD_DOWN"
    Swipe.LEFT -> "KEYCODE_DPAD_LEFT"
    Swipe.RIGHT -> "KEYCODE_DPAD_RIGHT"
}

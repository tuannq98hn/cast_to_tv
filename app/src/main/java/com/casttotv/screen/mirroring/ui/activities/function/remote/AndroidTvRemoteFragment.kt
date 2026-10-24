package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.FragmentRemoteAndroidTvNewBinding
import com.casttotv.screen.mirroring.tv.AndroidTvRemote
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.dialogs.PinCodeDialog

/** Port of `RemoteAndroidTVFragment`: Android KeyEvent codes over the Android TV Remote protocol. */
class AndroidTvRemoteFragment : BaseRemoteFragment(), AndroidTvRemote.Listener {

    private var _binding: FragmentRemoteAndroidTvNewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRemoteAndroidTvNewBinding.inflate(inflater, container, false)
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
                SwipeTouchListener(requireContext(), onSwipe = { key(it.toAndroidKey()) }, onTap = { key(23) }),
            )
            mapOf(
                ctBackLG to 4, ctExitLG to 4, viewExit to 4, viewReturn to 4, ctGuide to 172, ctPowerLG to 26,
                llNext to 87, llPause to 127, llPre to 88, llSource to 178, rltMenu to 82, rltRemoteChDown to 167,
                rltRemoteChUp to 166, rltRemoteDown to 20, rltRemoteHome to 3, rltRemoteLeft to 21, rltRemoteOk to 23,
                rltRemoteRight to 22, rltRemoteUp to 19, rltRemoteVolumeDown to 25, rltRemoteVolumeMute to 164,
                rltRemoteVolumeUp to 24,
            ).forEach { (view, code) -> view.setOnClickListener { key(code) } }
            // KEYCODE_0..9 are 7..16.
            bindNumbers(com.casttotv.screen.mirroring.databinding.LayoutNumberBinding.bind(ctNumberLG)) { digit -> AndroidTvRemote.sendKey(7 + digit) }
        }
        connectRemote()
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

    private fun key(code: Int) = connected {
        if (AndroidTvRemote.isConnected) AndroidTvRemote.sendKey(code) else connectRemote()
    }

    private fun connectRemote() {
        val host = (TvConnect.deviceAndroidTv ?: TvConnect.device)?.ipAddress ?: return
        AndroidTvRemote.connect(requireContext().applicationContext, host, this)
    }

    override fun onConnected() = Unit

    override fun onPairingRequired() {
        activity?.runOnUiThread {
            PinCodeDialog(requireActivity(), numeric = false) { code -> AndroidTvRemote.pair(code, this) }.show()
        }
    }

    override fun onFailed() {
        activity?.runOnUiThread {
            context?.let { Toast.makeText(it, R.string.text_connected_fail, Toast.LENGTH_SHORT).show() }
        }
    }
}

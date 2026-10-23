package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.FragmentRemoteSonyBinding
import com.casttotv.screen.mirroring.tv.SonyRemote
import com.casttotv.screen.mirroring.ui.dialogs.PinCodeDialog

/** Port of `RemoteSonyFragment`: Bravia IRCC codes after a PIN registration. */
class SonyRemoteFragment : BaseRemoteFragment() {

    private var _binding: FragmentRemoteSonyBinding? = null
    private val binding get() = _binding!!
    private var playing = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRemoteSonyBinding.inflate(inflater, container, false)
        with(binding) {
            bindPressedBackground(
                imvHomeSony, R.drawable.img_remote_sony,
                mapOf(
                    llRemoteSonyUp to R.drawable.img_remote_sony_up, llRemoteSonyBottom to R.drawable.img_remote_sony_down,
                    llRemoteSonyLeft to R.drawable.img_remote_sony_left, llRemoteSonyRight to R.drawable.img_remote_sony_right,
                    llRemoteSonyOk to R.drawable.img_remote_sony_ok,
                ),
            )
            bindPressedBackground(
                ctVolSony, R.drawable.bg_vol_type0,
                mapOf(llRemoteSonyVolumeUp to R.drawable.bg_vol_type0_up, llRemoteSonyVolumeDown to R.drawable.bg_vol_type0_down),
            )
            bindPressedBackground(
                ctChSony, R.drawable.bg_vol_type0,
                mapOf(llRemoteSonyChannelUp to R.drawable.bg_vol_type0_up, llRemoteSonyChannelDown to R.drawable.bg_vol_type0_down),
            )
            ctTouchpadSony.setOnTouchListener(
                SwipeTouchListener(requireContext(), onSwipe = { send(it.toSony()) }, onTap = { send(OK) }),
            )
            mapOf(
                ctPowerSony to "AAAAAQAAAAEAAAAVAw==", llRemoteSonyAudio to "AAAAAQAAAAEAAAAXAw==",
                llRemoteSonyBlueHelp to "AAAAAgAAAMQAAABNAw==", llRemoteSonyBottom to DOWN, llRemoteSonyCC to "AAAAAgAAAJcAAAAdAw==",
                llRemoteSonyChannelDown to "AAAAAQAAAAEAAAARAw==", llRemoteSonyChannelUp to "AAAAAQAAAAEAAAAQAw==",
                llRemoteSonyHome to "AAAAAQAAAAEAAABgAw==", llRemoteSonyLeft to LEFT, llRemoteSonyMenu to "AAAAAgAAABoAAABYAw==",
                llRemoteSonyMute to "AAAAAQAAAAEAAAAUAw==", llRemoteSonyNext to "AAAAAgAAAJcAAAA9Aw==", llRemoteSonyOk to OK,
                llRemoteSonyOptions to "AAAAAgAAAJcAAAA2Aw==", llRemoteSonyPre to "AAAAAgAAAJcAAAA8Aw==",
                llRemoteSonyReturn to "AAAAAgAAAJcAAAAjAw==", llRemoteSonyRight to RIGHT, llRemoteSonySource to "AAAAAQAAAAEAAAAlAw==",
                llRemoteSonyStop to "AAAAAgAAAJcAAAAYAw==", llRemoteSonyUp to UP, llRemoteSonyVolumeDown to "AAAAAQAAAAEAAAATAw==",
                llRemoteSonyVolumeUp to "AAAAAQAAAAEAAAASAw==",
            ).forEach { (view, code) -> view.setOnClickListener { send(code) } }
            llRemoteSonyPlayPause.setOnClickListener {
                send(if (playing) "AAAAAgAAAJcAAAAZAw==" else "AAAAAgAAAJcAAAAaAw==")
                playing = !playing
            }
            bindNumbers(com.casttotv.screen.mirroring.databinding.LayoutNumberBinding.bind(ctNumberSony)) { digit -> SonyRemote.send(DIGITS[digit]) }
        }
        register(null)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun showMode(mode: RemoteActivity.Mode) = with(binding) {
        ctRemoteSony.visibility = if (mode == RemoteActivity.Mode.REMOTE) View.VISIBLE else View.INVISIBLE
        ctTouchpadSony.visibility = if (mode == RemoteActivity.Mode.TOUCH) View.VISIBLE else View.GONE
        ctNumberSony.visibility = if (mode == RemoteActivity.Mode.NUMBER) View.VISIBLE else View.GONE
    }

    private fun send(code: String) = connected {
        SonyRemote.send(code) { status -> if (status == 403) activity?.runOnUiThread { register(null) } }
    }

    /** `dd5.a`: register, asking for the PIN the TV shows when needed. */
    private fun register(pin: String?) {
        SonyRemote.register(pin) { result ->
            activity?.runOnUiThread {
                when (result) {
                    SonyRemote.PairResult.NEED_PIN -> PinCodeDialog(requireActivity(), numeric = true) { register(it) }.show()
                    SonyRemote.PairResult.FAILED ->
                        Toast.makeText(requireContext(), R.string.fail_connect_sony, Toast.LENGTH_SHORT).show()
                    SonyRemote.PairResult.PAIRED -> Unit
                }
            }
        }
    }

    private companion object {
        const val UP = "AAAAAQAAAAEAAAB0Aw=="
        const val DOWN = "AAAAAQAAAAEAAAB1Aw=="
        const val LEFT = "AAAAAQAAAAEAAAA0Aw=="
        const val RIGHT = "AAAAAQAAAAEAAAAzAw=="
        const val OK = "AAAAAQAAAAEAAABlAw=="
        val DIGITS = listOf(
            "AAAAAQAAAAEAAAAJAw==", "AAAAAQAAAAEAAAAAAw==", "AAAAAQAAAAEAAAABAw==", "AAAAAQAAAAEAAAACAw==",
            "AAAAAQAAAAEAAAADAw==", "AAAAAQAAAAEAAAAEAw==", "AAAAAQAAAAEAAAAFAw==", "AAAAAQAAAAEAAAAGAw==",
            "AAAAAQAAAAEAAAAHAw==", "AAAAAQAAAAEAAAAIAw==",
        )

        fun Swipe.toSony() = when (this) {
            Swipe.UP -> UP
            Swipe.DOWN -> DOWN
            Swipe.LEFT -> LEFT
            Swipe.RIGHT -> RIGHT
        }
    }
}

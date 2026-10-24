package com.casttotv.screen.mirroring.ui.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.DialogDisconnectBinding
import com.casttotv.screen.mirroring.databinding.DialogPairingAlertBinding
import com.casttotv.screen.mirroring.databinding.DialogPinCodeBinding
import com.casttotv.screen.mirroring.tv.TvConnect

/** `sm4`: "accept the prompt on your TV". OK runs [onOk] (the original disconnects there). */
class PairingAlertDialog(context: Context, private val onOk: () -> Unit) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogPairingAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnOk.setOnClickListener {
            onOk()
            dismiss()
        }
    }
}

/** `vm4`: PIN shown on the TV. [onCancel] defaults to nothing; LG needs "000000" (see caller). */
class PinCodeDialog(
    private val activity: Activity,
    private val numeric: Boolean,
    private val onCancel: () -> Unit = {},
    private val onPin: (String) -> Unit,
) : Dialog(activity) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DialogPinCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setCanceledOnTouchOutside(false)
        setCancelable(false)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.AnimationDialog
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        binding.tvPinCodeOk.setOnClickListener {
            onPin(binding.edtPinCodeInput.text.toString().trim())
            dismiss()
        }
        binding.tvPinCodeCancel.setOnClickListener {
            dismiss()
            onCancel()
        }
        if (numeric) {
            binding.edtPinCodeInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            binding.edtPinCodeInput.transformationMethod = PasswordTransformationMethod.getInstance()
        }
        binding.edtPinCodeInput.post {
            binding.edtPinCodeInput.requestFocus()
            activity.getSystemService(InputMethodManager::class.java)
                ?.showSoftInput(binding.edtPinCodeInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }
}

/** Plain transparent full-width dialog for layouts with only a close button (`lf0`). */
object SimpleDialog {
    fun show(context: Context, @LayoutRes layout: Int, @IdRes closeId: Int? = null): Dialog =
        Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(layout)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                attributes.windowAnimations = R.style.DialogAnimation
            }
            closeId?.let { id -> findViewById<View>(id)?.setOnClickListener { dismiss() } }
            show()
        }
}

/** `wi1`: "Disconnect from <tv>?". The left button disconnects, the right one keeps the connection. */
class DisconnectDialog(
    context: Context,
    private val deviceName: String,
    private val onDisconnected: () -> Unit = {},
) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DialogDisconnectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.AnimationDialog
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        binding.tvTitleDisconnect.isSelected = true
        binding.tvTitleDisconnect.text =
            if (TvConnect.isConnectWeb) context.getString(R.string.title_disconnect_web)
            else String.format(context.getString(R.string.title_disconnect), deviceName)
        binding.tvCancel.setOnClickListener {
            TvConnect.disconnect()
            dismiss()
            onDisconnected()
        }
        binding.tvDisconnect.setOnClickListener { dismiss() }
    }
}

/** `bh1` type 0 (`dialog_exit_app`): confirm leaving the app from Main. */
class ExitDialog(context: Context, private val onExit: () -> Unit) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val binding = com.casttotv.screen.mirroring.databinding.DialogExitAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.mainAdsNative.visibility = android.view.View.GONE
        binding.tvTitleExit.setText(com.casttotv.screen.mirroring.R.string.confirm_exit)
        binding.tvDesExit.setText(com.casttotv.screen.mirroring.R.string.are_you_sure_you_want_to_exit)
        binding.tvCancel.setOnClickListener { dismiss() }
        binding.tvExit.setOnClickListener {
            dismiss()
            onExit()
        }
    }
}

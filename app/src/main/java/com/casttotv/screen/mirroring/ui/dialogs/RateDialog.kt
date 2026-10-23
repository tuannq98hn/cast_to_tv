package com.casttotv.screen.mirroring.ui.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.isVisible
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.data.AppPrefs
import com.casttotv.screen.mirroring.databinding.DialogRateBinding

/** Port of `e65`: 5 stars opens the store, fewer stars asks for written feedback. */
class RateDialog(private val activity: Activity, private val onClosed: () -> Unit = {}) : Dialog(activity) {

    private lateinit var binding: DialogRateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        binding = DialogRateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.attributes?.windowAnimations = R.style.AnimationDialog
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.rattingBar.setOnRatingChangeListener { _, rating, _ ->
            val low = rating in 1f..4f
            binding.edtDialogRateInput.isVisible = low
            binding.dialogRateSubmit.isVisible = low
            binding.btnRate.isVisible = !low
        }
        binding.tvExit.setOnClickListener {
            AppPrefs.rated = true
            dismiss()
        }
        // ponytail: Play in-app review API not bundled; the original falls back to the store page the same way.
        binding.btnRate.setOnClickListener {
            AppPrefs.rated = true
            openStore(activity)
            dismiss()
        }
        binding.dialogRateSubmit.setOnClickListener {
            AppPrefs.rated = true
            val text = binding.edtDialogRateInput.text.toString()
            if (text.isEmpty()) {
                Toast.makeText(context, "Feedback is empty", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            sendFeedback(context, text)
            Toast.makeText(context, "Thank you for sharing your feedback", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun dismiss() {
        super.dismiss()
        onClosed()
    }

    companion object {
        /** `ft4.H`: every third call (until rated) shows the rate dialog. */
        fun maybeShow(activity: Activity, onClosed: () -> Unit = {}): Boolean {
            if (AppPrefs.rated) return false
            val count = AppPrefs.rateCount
            if (count % 3 != 0) {
                AppPrefs.rateCount = count + 1
                return false
            }
            AppPrefs.rateCount = 1
            RateDialog(activity, onClosed).show()
            return true
        }

        /** `ft4.w`. */
        fun openStore(context: Context) {
            val id = context.packageName
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=$id")))
            }
        }

        /** `ft4.I`: e-mail feedback with the version code appended. */
        fun sendFeedback(context: Context, text: String) {
            val version = runCatching { PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0)) }.getOrDefault(0L)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/email"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("info@begamob.com"))
                putExtra(Intent.EXTRA_SUBJECT, "[User feedback] Cast to TV")
                putExtra(Intent.EXTRA_TEXT, "$text\n\n ---------------------- \n Version Code:$version")
            }
            context.startActivity(Intent.createChooser(intent, "Send Feedback:").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

package com.casttotv.screen.mirroring.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.DialogPermissionBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Port of `DialogPermission` (`gq4`): explains a runtime permission before the system prompt. */
object PermissionSheet {
    enum class Type { STORAGE, PHOTO, AUDIO, NOTIFICATION }

    fun show(context: Context, type: Type, onAllow: () -> Unit) {
        val b = DialogPermissionBinding.inflate(LayoutInflater.from(context))
        val sheet = BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme).apply { setContentView(b.root) }
        val (image, detail) = when (type) {
            Type.STORAGE -> R.drawable.img_permission_android_13 to R.string.text_permission_android_13_detail
            Type.PHOTO -> R.drawable.img_permission_photo to R.string.text_permission_photo_detail
            Type.AUDIO -> R.drawable.img_permission_audio to R.string.text_permission_audio_detail
            Type.NOTIFICATION -> R.drawable.img_premission_notification to R.string.text_premission_notification_detail
        }
        b.imgMain.setImageResource(image)
        b.txtDetail.setText(detail)
        if (type == Type.NOTIFICATION) b.txtMain.setText(R.string.text_noti_main)
        b.btnAllow.setOnClickListener {
            sheet.dismiss()
            onAllow()
        }
        b.txtDeny.setOnClickListener { sheet.dismiss() }
        sheet.show()
    }
}

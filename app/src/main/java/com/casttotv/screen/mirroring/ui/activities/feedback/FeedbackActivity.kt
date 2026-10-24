package com.casttotv.screen.mirroring.ui.activities.feedback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityFeedbackBinding
import com.casttotv.screen.mirroring.ui.dialogs.RateDialog

/** Port of `FeedbackActivity`: pick a topic, type, e-mail it; the Messenger animation opens a chat. */
class FeedbackActivity : BaseActivity() {

    /** `ln6`. */
    private enum class Topic { CAN_NOT_CONNECT, NOT_WORKING, CRASH, SUGGESTION, OTHER, NONE }

    private lateinit var binding: ActivityFeedbackBinding
    private var topic = Topic.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            imgBack.setOnClickListener { finish() }
            animMessenger.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("fb-messenger://user/61552634416080")))
                } catch (_: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://m.me/61552634416080")))
                }
            }
            topics().forEach { (view, t) -> view.setOnClickListener { select(t) } }
            edtInput.doOnTextChanged { text, _, _, _ -> setSubmitEnabled(!text.isNullOrEmpty()) }
            setSubmitEnabled(false)
            btnSubmit.setOnClickListener {
                val text = edtInput.text?.toString().orEmpty()
                if (text.isEmpty()) return@setOnClickListener
                RateDialog.sendFeedback(this@FeedbackActivity, "[${topic.name}] $text")
                Toast.makeText(this@FeedbackActivity, "Thank you for sharing your feedback", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun topics(): List<Pair<TextView, Topic>> = with(binding) {
        listOf(
            txtCantConnect to Topic.CAN_NOT_CONNECT, txtNotWorking to Topic.NOT_WORKING, txtCrash to Topic.CRASH,
            txtSuggestion to Topic.SUGGESTION, txtOther to Topic.OTHER,
        )
    }

    /** `s(ln6)`. */
    private fun select(selected: Topic) {
        topic = selected
        topics().forEach { (view, t) ->
            val on = t == selected
            view.background = ContextCompat.getDrawable(this, if (on) R.drawable.bg_item_feedback_selected else R.drawable.bg_item_feedback)
            view.setTextColor(ContextCompat.getColor(this, if (on) R.color.white else R.color.dc_text_body))
        }
    }

    /** `c02`. */
    private fun setSubmitEnabled(enabled: Boolean) = with(binding.btnSubmit) {
        setTextColor(ContextCompat.getColor(context, if (enabled) R.color.white else R.color.color_hint))
        background = ContextCompat.getDrawable(context, if (enabled) R.drawable.bg_button_feedback else R.drawable.bg_button_feedback_disable)
        isClickable = enabled
    }
}

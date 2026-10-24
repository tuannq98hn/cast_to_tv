package com.casttotv.screen.mirroring.ui.activities.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.AppPrefs
import com.casttotv.screen.mirroring.data.Premium
import com.casttotv.screen.mirroring.databinding.ActivitySettingBinding
import com.casttotv.screen.mirroring.ui.activities.feedback.FeedbackActivity
import com.casttotv.screen.mirroring.ui.activities.howto.HowToUseActivity
import com.casttotv.screen.mirroring.ui.activities.language.Language
import com.casttotv.screen.mirroring.ui.activities.language.LanguageFirstActivity
import com.casttotv.screen.mirroring.ui.activities.purchase.PurchaseActivity
import com.casttotv.screen.mirroring.ui.dialogs.RateDialog

/** Port of `SettingActivity`. */
class SettingActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            header.tvTitle.setText(R.string.setting)
            header.castConnect.visibility = View.GONE
            header.castPremium.visibility = View.GONE
            header.imvBack.setOnClickListener { finish() }
            tbVibrate.isChecked = AppPrefs.hapticFeedback
            tbVibrate.setOnCheckedChangeListener { _, checked -> AppPrefs.hapticFeedback = checked }
            tvVersion.text = getString(R.string.version) + " " + packageManager.getPackageInfo(packageName, 0).versionName
            clPremium.setOnClickListener {
                open(PurchaseActivity::class.java) { putExtra(PurchaseActivity.EXTRA_FROM, "screen_setting") }
            }
            rlLanguage.setOnClickListener {
                open(LanguageFirstActivity::class.java) { putExtra(LanguageFirstActivity.EXTRA_FROM_SETTING, true) }
            }
            rlHty1.setOnClickListener { howTo(3) }
            rlHty2.setOnClickListener { howTo(2) }
            rlHty3.setOnClickListener { howTo(1) }
            rlRate.setOnClickListener { RateDialog(this@SettingActivity).show() }
            rlFeedback.setOnClickListener { open(FeedbackActivity::class.java) }
            rlShare.setOnClickListener {
                val share = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                    .putExtra(Intent.EXTRA_TEXT, "\nLet me recommend you this application\n\nhttps://play.google.com/store/apps/details?id=$packageName\n\n")
                startActivity(Intent.createChooser(share, "Choose one"))
            }
            rlPrivacy.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://begamob.com/cast-policy.html"))) }
        }
        startShine()
    }

    override fun onResume() {
        super.onResume()
        binding.clPremium.isVisible = !Premium.isActive
        binding.txtLanguage.setText(Language.of(AppPrefs.language).label)
    }

    /** `m()`: the shine sweep over the premium banner, restarted when it ends (`g45`). */
    private fun startShine() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.left_right)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) = Unit
            override fun onAnimationRepeat(animation: Animation) = Unit
            override fun onAnimationEnd(animation: Animation) = binding.shine.startAnimation(anim)
        })
        binding.shine.startAnimation(anim)
    }

    private fun howTo(type: Int) = startActivity(Intent(this, HowToUseActivity::class.java).putExtra(HowToUseActivity.EXTRA_TYPE, type))
}

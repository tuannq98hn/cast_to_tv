package com.casttotv.screen.mirroring.ui.activities.language

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieDrawable
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.AppPrefs
import com.casttotv.screen.mirroring.databinding.ActivityLanguageBinding
import com.casttotv.screen.mirroring.databinding.ItemLanguageLayoutBinding
import com.casttotv.screen.mirroring.ui.activities.splash.Onboarding
import com.casttotv.screen.mirroring.ui.activities.splash.SplashActivity
import java.util.Locale

/** `q83`: languages offered by the picker, in default order. */
enum class Language(val title: String, val key: String, val icon: Int, @StringRes val label: Int) {
    ENGLISH("English", "en", R.drawable.ic_language_en, R.string.lag_english),
    PORTUGUESE("Portuguese", "pt", R.drawable.ic_language_pt, R.string.lag_portuguese),
    FRENCH("French", "fr", R.drawable.ic_language_fr, R.string.lag_french),
    SPANISH("Spanish", "es", R.drawable.ic_language_es, R.string.lag_spanish),
    RUSSIAN("Russian", "ru", R.drawable.ic_language_russia, R.string.lag_russian),
    TURKISH("Turkish", "tr", R.drawable.ic_language_turkey, R.string.lag_turkish),
    INDONESIAN("Indonesian", "in", R.drawable.ic_language_id, R.string.lag_indonesian),
    ITALIAN("Italian", "it", R.drawable.ic_language_it, R.string.lag_italy),
    GERMAN("German", "de", R.drawable.ic_language_de, R.string.lag_german),
    HINDI("Hindi", "hi", R.drawable.ic_language_in, R.string.lag_hindi),
    NORWEGIAN("Norwegian", "no", R.drawable.ic_laguage_norway, R.string.lag_norwegian),
    FINNISH("Finnish", "fi", R.drawable.ic_laguage_finland, R.string.lag_finnish),
    VIETNAMESE("Vietnamese", "vi", R.drawable.ic_laguage_vietnam, R.string.lag_vn);

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: ENGLISH
    }
}

/** Port of `LanguageFirstActivity` (first run, or `from_setting`). */
class LanguageFirstActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var fromSetting = false
    private lateinit var items: List<Language>
    private lateinit var selected: Language

    override fun onCreate(savedInstanceState: Bundle?) {
        fromSetting = intent.getBooleanExtra(EXTRA_FROM_SETTING, false)
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mainAdsNative.isVisible = false

        // Hand tutorial: plays 4 loops, then hides.
        with(binding.animTutorial) {
            isVisible = true
            playAnimation()
            var loops = 4
            addAnimatorListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationRepeat(animation: android.animation.Animator) {
                    if (--loops <= 0) {
                        cancelAnimation()
                        isVisible = false
                    }
                }
            })
            repeatCount = LottieDrawable.INFINITE
        }
        ValueAnimator.ofFloat(0.9f, 1.1f).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                val v = it.animatedValue as Float
                binding.imvOke.scaleX = v
                binding.imvOke.scaleY = v
            }
        }.start()

        binding.imvOke.setOnClickListener {
            AppPrefs.openCount = 1
            apply()
        }
        binding.imgBack.isVisible = fromSetting
        binding.imgBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback(this) { if (fromSetting) finish() else apply() }

        // `language_order` first, then the rest; the default (en, or the device language) is pinned on top.
        val order = AppPrefs.languageOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val ordered = (order.map { Language.of(it) } + Language.entries).distinct()
        val device = Locale.getDefault().language
        val default = if (AppPrefs.autoSelectLanguage && Language.entries.any { it.key == device }) Language.of(device) else Language.ENGLISH
        items = listOf(default) + ordered.filter { it != default }
        select(default)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = Adapter()
    }

    private fun select(language: Language) {
        selected = language
        binding.imvOke.text = localized(R.string.done, language.key)
        binding.tvTitle.text = localized(R.string.language, language.key)
    }

    /** `j(int, String)`: the string in the highlighted language. */
    private fun localized(@StringRes id: Int, key: String): String {
        val config = Configuration(resources.configuration).apply { setLocale(Locale(key)) }
        return createConfigurationContext(config).resources.getString(id)
    }

    /** `l()`: save + apply the locale, then continue onboarding (or restart from Splash when opened from Settings). */
    private fun apply() {
        AppPrefs.language = selected.key
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selected.key))
        if (fromSetting) {
            startActivity(Intent(this, SplashActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        } else {
            Onboarding.afterLanguage(this)
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {
        inner class Holder(val binding: ItemLanguageLayoutBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemLanguageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
            val language = items[position]
            rbChecked.isChecked = language == selected
            imvIcon.setImageResource(language.icon)
            tvName.text = language.title
            root.setOnClickListener {
                select(language)
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        const val EXTRA_FROM_SETTING = "from_setting"
    }
}

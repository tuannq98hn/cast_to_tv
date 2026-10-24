package com.casttotv.screen.mirroring.ui.activities.intro

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityIntroBinding
import com.casttotv.screen.mirroring.databinding.FragmentIntroBinding
import com.casttotv.screen.mirroring.ui.activities.main.MainActivity

/** Port of `IntroActivity`: three onboarding pages (`q13`), then Main. */
class IntroActivity : BaseActivity() {

    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.adsView.isVisible = false

        val pages = listOf(
            Triple(R.drawable.bg_intro_1, R.string.title_1, R.string.guide_1),
            Triple(R.drawable.bg_intro_2, R.string.title_2, R.string.guide_2),
            Triple(R.drawable.bg_intro_3, R.string.title_3, R.string.guide_3),
        )
        binding.vpgIntro.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = pages.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(FragmentIntroBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val page = FragmentIntroBinding.bind(holder.itemView)
                page.imgIntro.setImageResource(pages[position].first)
                page.tvTitle.setText(pages[position].second)
                page.tvIntro.setText(pages[position].third)
            }
        }
        binding.dotsIndicator.attachTo(binding.vpgIntro)
        // `yw` case 4: Skip is hidden on the last page.
        binding.vpgIntro.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tvSkip.isVisible = position != 2
            }
        })
        binding.tvSkip.setOnClickListener { binding.vpgIntro.setCurrentItem(2, true) }
        binding.imgStart.setOnClickListener {
            val page = binding.vpgIntro.currentItem
            if (page < 2) binding.vpgIntro.setCurrentItem(page + 1, true) else finishIntro()
        }
        onBackPressedDispatcher.addCallback(this) {
            if (binding.vpgIntro.currentItem == 1) binding.vpgIntro.setCurrentItem(0, true) else finishIntro()
        }
        ValueAnimator.ofFloat(0.9f, 1.2f).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                binding.imgStart.scaleX = it.animatedValue as Float
                binding.imgStart.scaleY = it.animatedValue as Float
            }
        }.start()
    }

    /** `t()`. */
    private fun finishIntro() {
        open(MainActivity::class.java)
        finish()
    }
}

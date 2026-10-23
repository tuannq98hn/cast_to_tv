package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.os.Bundle
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityGuideFireTvBinding

/** Port of `GuideFireTVActivity`: how to enable ADB debugging on the Fire TV. */
class GuideFireTVActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityGuideFireTvBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imvClose.setOnClickListener { finish() }
    }
}

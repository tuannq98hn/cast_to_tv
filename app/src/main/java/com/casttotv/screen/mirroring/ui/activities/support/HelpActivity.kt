package com.casttotv.screen.mirroring.ui.activities.support

import android.os.Bundle
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityHelpBinding

/** Port of `HelpActivity`: the bundled troubleshooting page. */
class HelpActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.imvBack.setOnClickListener { finish() }
        binding.wvHelp.loadUrl("file:///android_asset/help.html")
    }
}

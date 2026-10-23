package com.casttotv.screen.mirroring.ui.activities.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.casttotv.screen.mirroring.ui.dialogs.PermissionSheet
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.AppPrefs
import com.casttotv.screen.mirroring.data.Premium
import com.casttotv.screen.mirroring.databinding.ActivityMainBinding
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.function.media.MediaActivity
import com.casttotv.screen.mirroring.ui.activities.photo_off.PhotoOfflineActivity
import com.casttotv.screen.mirroring.ui.activities.function.screen_mirror.ScreenMirroringActivity
import com.casttotv.screen.mirroring.ui.activities.function.remote.RemoteActivity
import com.casttotv.screen.mirroring.ui.activities.function.youtube_browser.YoutubeBrowserActivity
import com.casttotv.screen.mirroring.ui.activities.function.connect.SearchTVActivity
import com.casttotv.screen.mirroring.ui.activities.function.connect.icon
import com.casttotv.screen.mirroring.ui.dialogs.DisconnectDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Feature waiting on a runtime permission (original field `h`). */
    private var pendingFeature: Feature? = null

    private val mediaPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val feature = pendingFeature
        if (granted && feature != null) openFeature(feature)
        else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // `p()`: Android 13+ users with notifications off get the explainer first.
        if (Build.VERSION.SDK_INT >= 33 && !androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            PermissionSheet.show(this, PermissionSheet.Type.NOTIFICATION) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        AppPrefs.firstOpen = false
        com.casttotv.screen.mirroring.service.RemindAlarmReceiver.schedule(this)
        // `onBackPressed`: premium users leave directly, others confirm with `bh1`.
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (Premium.isActive) finish()
                else com.casttotv.screen.mirroring.ui.dialogs.ExitDialog(this@MainActivity) { finishAffinity() }.show()
            }
        })

        with(binding) {
            imvSetting.setOnClickListener { open(com.casttotv.screen.mirroring.ui.activities.setting.SettingActivity::class.java) }
            castPremium.setOnClickListener {
                if (!Premium.isActive) open(com.casttotv.screen.mirroring.ui.activities.purchase.PurchaseActivity::class.java) {
                    putExtra(com.casttotv.screen.mirroring.ui.activities.purchase.PurchaseActivity.EXTRA_FROM, "screen_main")
                }
            }
            rlConnect.setOnClickListener { openFeature(Feature.CONNECT) }
            rlMirror.setOnClickListener { openFeature(Feature.MIRROR) }
            rlRemote.setOnClickListener { openFeature(Feature.REMOTE) }
            ctYoutube.setOnClickListener { openFeature(Feature.YOUTUBE) }
            ctVimeo.setOnClickListener { openFeature(Feature.VIMEO) }
            ctDriver.setOnClickListener { openFeature(Feature.WEB) }
            llPhotoOff.setOnClickListener { withMediaPermission(Feature.PHOTO, imagePermission()) }
            llVideoOff.setOnClickListener { withMediaPermission(Feature.VIDEO, videoPermission()) }
            // Ad slots kept in the layout for fidelity, never shown.
            llAds.isVisible = false
            lnlBanner.isVisible = false
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { TvConnect.events.collect { renderState() } }
        }
    }

    override fun onResume() {
        super.onResume()
        renderState()
    }

    /** Port of `w()`: premium badge + connection header. */
    private fun renderState() = with(binding) {
        castPremium.setAnimation(if (Premium.isActive) R.raw.anim_member else R.raw.anim_premium)
        castPremium.repeatCount = -1
        castPremium.playAnimation()
        if (TvConnect.isConnected) {
            imvConnect.setImageResource(TvConnect.currentType().icon)
            tvTitleConnect.text = TvConnect.deviceName
            tvStatusConnect.setText(R.string.text_connected)
        } else {
            imvConnect.setImageResource(R.drawable.ic_no_connect_home)
            tvTitleConnect.setText(R.string.text_connect_to_tv)
            tvStatusConnect.setText(R.string.tab_to_connect)
        }
    }

    private fun withMediaPermission(feature: Feature, permission: String) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openFeature(feature)
        } else {
            pendingFeature = feature
            // `l(int)`: storage explainer below Android 13, the photo one otherwise.
            val type = if (Build.VERSION.SDK_INT < 33) PermissionSheet.Type.STORAGE else PermissionSheet.Type.PHOTO
            PermissionSheet.show(this, type) { mediaPermission.launch(permission) }
        }
    }

    private fun imagePermission() =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun videoPermission() =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE

    /** Port of `n(boolean)`; targets get wired as each screen is ported. */
    private fun openFeature(feature: Feature) {
        val needsRokuChannel = feature == Feature.YOUTUBE || feature == Feature.VIMEO || feature == Feature.WEB
        if (needsRokuChannel && com.casttotv.screen.mirroring.ui.activities.channel_roku.InstallChannelRokuActivity.checkRoku(this)) return
        when (feature) {
            Feature.CONNECT ->
                if (TvConnect.isConnected) DisconnectDialog(this, TvConnect.deviceName) { renderState() }.show()
                else SearchTVActivity.open(this)
            Feature.PHOTO -> open(PhotoOfflineActivity::class.java)
            Feature.MIRROR -> open(ScreenMirroringActivity::class.java)
            Feature.REMOTE -> open(RemoteActivity::class.java)
            Feature.YOUTUBE -> open(YoutubeBrowserActivity::class.java) { putExtra(YoutubeBrowserActivity.EXTRA_TYPE, "youtube") }
            Feature.VIMEO -> open(YoutubeBrowserActivity::class.java) { putExtra(YoutubeBrowserActivity.EXTRA_TYPE, "vimeo") }
            Feature.VIDEO -> open(MediaActivity::class.java) { putExtra(MediaActivity.EXTRA_MEDIA_TYPE, "Video") }
            Feature.WEB -> open(com.casttotv.screen.mirroring.ui.activities.webcast.WebCastActivity::class.java)
        }
    }

    private enum class Feature { PHOTO, VIDEO, WEB, CONNECT, REMOTE, YOUTUBE, MIRROR, VIMEO }
}

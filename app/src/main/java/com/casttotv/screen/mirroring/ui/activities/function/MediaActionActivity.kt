package com.casttotv.screen.mirroring.ui.activities.function

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.MediaController
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityMediaActionBinding
import com.casttotv.screen.mirroring.models.CastSession
import com.casttotv.screen.mirroring.models.CastType
import com.casttotv.screen.mirroring.models.MediaItem
import com.casttotv.screen.mirroring.ui.activities.function.play.CastMediaActivity
import com.casttotv.screen.mirroring.ui.activities.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Port of `MediaActionActivity`: a photo or video shared from another app, previewed and cast in one tap. */
class MediaActionActivity : BaseActivity() {

    private lateinit var binding: ActivityMediaActionBinding
    private var photo = true
    private var item: MediaItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaActionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mainAdsNative.isVisible = false
        val uri = intent.data ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        photo = intent.type?.contains("image") != false
        bindHeader(binding.include3, getString(if (photo) R.string.cast_photo else R.string.cast_videos))
        binding.imvPhotoAction.isVisible = photo
        binding.linearVideoView.isVisible = !photo
        onBackPressedDispatcher.addCallback(this) {
            open(MainActivity::class.java)
            finish()
        }
        binding.btnStartCast.setOnClickListener { cast() }
        if (uri == null) return
        if (photo) Glide.with(this).load(uri).into(binding.imvPhotoAction)
        binding.progressBar.isVisible = true
        // `gm3` / `im3`: the receiver needs a real file to serve.
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) { runCatching { copyToCache(uri) }.getOrNull() }
            binding.progressBar.isVisible = false
            file ?: return@launch
            item = MediaItem(file.absolutePath, file.name)
            if (!photo) with(binding.myvideoview) {
                setVideoPath(file.absolutePath)
                setMediaController(MediaController(this@MediaActionActivity))
                start()
            }
        }
    }

    private fun cast() {
        val media = item ?: return
        withConnection {
            CastSession.start(if (photo) CastType.PHOTO_OFFLINE else CastType.VIDEO_OFFLINE, listOf(media), 0)
            open(CastMediaActivity::class.java)
        }
    }

    private fun copyToCache(uri: Uri): File {
        val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: uri.lastPathSegment ?: "shared"
        val dir = File(cacheDir, "shared").apply { mkdirs() }
        val out = File(dir, name.replace('/', '_'))
        contentResolver.openInputStream(uri)!!.use { input -> out.outputStream().use(input::copyTo) }
        return out
    }
}

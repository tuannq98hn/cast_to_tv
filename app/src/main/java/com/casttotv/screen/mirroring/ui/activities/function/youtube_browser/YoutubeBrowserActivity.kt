package com.casttotv.screen.mirroring.ui.activities.function.youtube_browser

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.bumptech.glide.Glide
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityYoutubeBrowserBinding
import com.casttotv.screen.mirroring.databinding.DialogBsfYoutubeBinding
import com.casttotv.screen.mirroring.databinding.DialogNoResourceFoundBinding
import com.casttotv.screen.mirroring.databinding.ItemYoutubeBinding
import com.casttotv.screen.mirroring.models.CastSession
import com.casttotv.screen.mirroring.models.CastType
import com.casttotv.screen.mirroring.models.MediaItem
import com.casttotv.screen.mirroring.ui.activities.function.play.CastMediaActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Port of `YoutubeBrowserActivity` (`browser_type` = youtube | vimeo): browse, extract stream URLs, cast. */
class YoutubeBrowserActivity : BaseActivity() {

    /** `ge7`: one playable stream of the current page. */
    private data class Stream(val title: String, val thumb: String, val url: String, val quality: String, val lengthSec: Long, val audioBitrate: Int)

    private lateinit var binding: ActivityYoutubeBrowserBinding
    private lateinit var home: String
    private val streams = mutableListOf<Stream>()
    private var lastUrl: String? = null
    private var forceRescan = false
    private var sheet: BottomSheetDialog? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubeBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val youtube = intent.getStringExtra(EXTRA_TYPE) == "youtube"
        home = if (youtube) "https://www.youtube.com/" else "https://vimeo.com/watch"
        bindHeader(binding.viewHeader, if (youtube) "Cast Youtube" else "Cast Vimeo")
        binding.linearProgress.max = 100
        AdBlocker.init(this)

        with(binding.page.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            displayZoomControls = false
            builtInZoomControls = true
            setSupportZoom(true)
        }
        binding.page.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                binding.linearProgress.isVisible = progress != 100
                binding.linearProgress.progress = progress
            }
        }
        binding.page.webViewClient = Client()
        lastUrl = binding.page.url
        binding.page.loadUrl(home)

        binding.imvYoutubeBrowserBack.setOnClickListener { if (binding.page.canGoBack()) binding.page.goBack() }
        binding.imvYoutubeBrowserForward.setOnClickListener { if (binding.page.canGoForward()) binding.page.goForward() }
        binding.imvYoutubeBrowserHome.setOnClickListener { binding.page.loadUrl(home) }
        binding.imvYoutubeBrowserReload.setOnClickListener {
            forceRescan = true
            binding.page.reload()
        }
        binding.imvYoutubeBrowserList.setOnClickListener { if (streams.isEmpty()) showNoResource() else showStreams() }
    }

    override fun onDestroy() {
        sheet?.dismiss()
        super.onDestroy()
    }

    /** `ee7`: on navigation to a watch page, extract its streams; ad hosts are blocked. */
    private inner class Client : WebViewClient() {
        override fun onLoadResource(view: WebView, url: String) {
            val page = view.url ?: return
            if ((page == lastUrl || page.contains("#searching")) && !forceRescan) return
            forceRescan = false
            lastUrl = page
            streams.clear()
            binding.imvYoutubeBrowserBack.setImageResource(if (view.canGoBack()) R.drawable.ic_yt_back_black else R.drawable.ic_yt_back)
            binding.imvYoutubeBrowserForward.setImageResource(if (view.canGoBack()) R.drawable.ic_yt_next_black else R.drawable.ic_yt_next)
            if (page.contains(WATCH)) extract("http://youtube.com/watch?v=" + page.replace(WATCH, ""))
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            AdBlocker.intercept(request.url.toString()) ?: super.shouldInterceptRequest(view, request)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = request.url.toString().startsWith("intent")
    }

    /** `de7`: keep mp4/HLS streams at 240/360/720/1080p, dropping video-only duplicates. */
    @SuppressLint("StaticFieldLeak")
    private fun extract(url: String) {
        object : YouTubeExtractor(this) {
            override fun onExtractionComplete(files: SparseArray<YtFile>?, meta: VideoMeta?) {
                if (files == null || meta == null) return
                for (i in 0 until files.size()) {
                    val file = files.valueAt(i)
                    val format = file.format
                    if (format.ext in listOf("mp4", "m3u", "m3u8") && format.height in listOf(240, 360, 720, 1080)) {
                        streams += Stream(meta.title, meta.hqImageUrl, file.url, "${format.height}p", meta.videoLength, format.audioBitrate)
                    }
                }
                listOf("360p", "720p").forEach { q ->
                    if (streams.any { it.quality == q && it.audioBitrate != -1 }) streams.removeAll { it.quality == q && it.audioBitrate == -1 }
                }
                runOnUiThread { onStreamsFound() }
            }
        }.extract(url)
    }

    /** `fe7` case 0. */
    private fun onStreamsFound() {
        binding.imvYoutubeBrowserList.setImageResource(if (streams.isEmpty()) R.drawable.ic_yt_list_empty else R.drawable.ic_yt_list)
        binding.imvData.isVisible = streams.isNotEmpty()
        if (streams.isNotEmpty() && sheet?.isShowing != true) showStreams()
    }

    /** `xg1`: list of streams; tapping one casts it. */
    private fun showStreams() {
        val dialogBinding = DialogBsfYoutubeBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme).apply { setContentView(dialogBinding.root) }
        dialogBinding.imvDialogBsfYoutubeClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.imvDialogBsfYoutubeHelp.setOnClickListener { dialogBinding.tvHelp.isVisible = !dialogBinding.tvHelp.isVisible }
        dialogBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        dialogBinding.recyclerView.adapter = StreamAdapter(streams.toList()) { stream ->
            dialog.dismiss()
            cast(stream)
        }
        sheet = dialog
        dialog.show()
    }

    /** `s()`. */
    private fun cast(stream: Stream) = withConnection {
        CastSession.start(CastType.YOUTUBE_ONLINE, listOf(MediaItem(stream.url, stream.title, stream.thumb, stream.lengthSec * 1000)), 0)
        open(CastMediaActivity::class.java)
    }

    /** `kh1`. */
    private fun showNoResource() {
        val dialogBinding = DialogNoResourceFoundBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialogBinding.imvClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.rlRefresh.setOnClickListener {
            dialog.dismiss()
            forceRescan = true
            binding.page.reload()
        }
        dialog.show()
    }

    private class StreamAdapter(private val items: List<Stream>, private val onClick: (Stream) -> Unit) :
        RecyclerView.Adapter<StreamAdapter.Holder>() {
        class Holder(val binding: ItemYoutubeBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemYoutubeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
            val s = items[position]
            Glide.with(root).load(s.thumb).placeholder(R.drawable.ic_video_default).into(imvItemYoutubeThumbnail)
            tvItemYoutubeFormat.text = "mp4"
            tvItemYoutubeName.text = s.title
            tvItemYoutubeQuality.text = s.quality
            imvNoAudio.visibility = if (s.audioBitrate == -1) View.VISIBLE else View.GONE
            root.setOnClickListener { onClick(s) }
        }
    }

    companion object {
        const val EXTRA_TYPE = "browser_type"
        private const val WATCH = "https://m.youtube.com/watch?v="
    }
}

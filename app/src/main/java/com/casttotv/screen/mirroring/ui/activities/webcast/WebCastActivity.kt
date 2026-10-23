package com.casttotv.screen.mirroring.ui.activities.webcast

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.BrowserHistory
import com.casttotv.screen.mirroring.data.HistoryBrowser
import com.casttotv.screen.mirroring.databinding.ActivityWebCastBinding
import com.casttotv.screen.mirroring.databinding.AvailableFilesDialogBinding
import com.casttotv.screen.mirroring.databinding.DialogShowImageBinding
import com.casttotv.screen.mirroring.databinding.DownloadResultListBinding
import com.casttotv.screen.mirroring.databinding.ItemContentHistoryBinding
import com.casttotv.screen.mirroring.databinding.VideoPlayerBinding
import com.casttotv.screen.mirroring.models.CastSession
import com.casttotv.screen.mirroring.models.CastType
import com.casttotv.screen.mirroring.models.MediaItem
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.function.connect.SearchTVActivity
import com.casttotv.screen.mirroring.ui.activities.function.play.CastMediaActivity
import com.casttotv.screen.mirroring.ui.activities.function.youtube_browser.AdBlocker
import com.casttotv.screen.mirroring.ui.dialogs.DisconnectDialog
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/** Port of `WebCastActivity`: a browser that sniffs video / photo / audio URLs from page resources and casts them. */
class WebCastActivity : BaseActivity() {

    /** `rm7`: a media resource found on the page; [kind] 1 = video, 2 = audio, 3 = photo. */
    class Found(var name: String, val url: String, val kind: Int, val size: String?)

    private lateinit var binding: ActivityWebCastBinding
    private val sniffer = Executors.newFixedThreadPool(4)
    private val videos = mutableListOf<Found>()
    private val audios = mutableListOf<Found>()
    private val photos = mutableListOf<Found>()
    private var pageTitle: String? = null
    private var progressTimer: CountDownTimer? = null
    private var lastTitle = ""
    private var lastUrl = ""
    private lateinit var videoFilters: Array<String>
    private lateinit var imageFilters: Array<String>
    private lateinit var audioFilters: Array<String>
    private lateinit var adFilters: Array<String>
    private lateinit var restricted: Array<String>
    private val recent = RecentAdapter()

    private val historyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val url = it.data?.getStringExtra(HistoryBrowserActivity.EXTRA_RESULT)
        if (it.resultCode == RESULT_OK && url != null) {
            binding.etSearchBar.setText(url)
            go()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebCastBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(resources) {
            videoFilters = getStringArray(R.array.videourl_filters)
            imageFilters = getStringArray(R.array.imageurl_filters)
            audioFilters = getStringArray(R.array.audiourl_filters)
            adFilters = getStringArray(R.array.ad_site_filters)
            restricted = getStringArray(R.array.ristricted_sites)
        }
        AdBlocker.init(this)
        binding.adsWebBottom.isVisible = false

        with(binding.simpleWebView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            displayZoomControls = false
            builtInZoomControls = true
            setSupportZoom(true)
        }
        binding.simpleWebView.webViewClient = Client()
        binding.simpleWebView.isVisible = false
        binding.etSearchBar.setAdapter(SuggestAdapter(this))

        binding.rcvHistory.layoutManager = LinearLayoutManager(this)
        binding.rcvHistory.adapter = recent
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { BrowserHistory.items.collect(::showRecent) }
                launch { TvConnect.events.collect { renderConnection() } }
            }
        }
        bindClicks()
        resetFound()

        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let(::openSite)
        }
        intent.data?.let {
            binding.etSearchBar.setText(it.toString())
            go()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = back()
        })
    }

    override fun onResume() {
        super.onResume()
        renderConnection()
    }

    override fun onDestroy() {
        progressTimer?.cancel()
        sniffer.shutdownNow()
        super.onDestroy()
    }

    private fun renderConnection() = binding.imvYoutubeBrowserConnect.setImageResource(
        if (TvConnect.isConnected) R.drawable.ic_header_connected else R.drawable.ic_header_not_connect,
    )

    private fun bindClicks() = with(binding) {
        imvDelete.setOnClickListener { BrowserHistory.clear() }
        imvToHistory.setOnClickListener {
            historyLauncher.launch(Intent(this@WebCastActivity, HistoryBrowserActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        imvYoutubeBack.setOnClickListener { finish() }
        imvYoutubeBrowserBack.setOnClickListener { back() }
        imvYoutubeBrowserConnect.setOnClickListener {
            if (TvConnect.isConnected) DisconnectDialog(this@WebCastActivity, TvConnect.deviceName) { renderConnection() }.show()
            else SearchTVActivity.open(this@WebCastActivity)
        }
        imvYoutubeBrowserForward.setOnClickListener {
            if (simpleWebView.canGoForward()) simpleWebView.goForward()
            if (!simpleWebView.isVisible) {
                llContent.isVisible = false
                simpleWebView.isVisible = true
            }
        }
        imvYoutubeBrowserHome.setOnClickListener { showHome() }
        imvYoutubeBrowserList.setOnClickListener { rlData.isVisible = true }
        imvYoutubeBrowserReload.setOnClickListener { simpleWebView.reload() }
        rlData.setOnClickListener { rlData.isVisible = false }
        rlVideo.setOnClickListener { showFound(1) }
        rlAudio.setOnClickListener { showFound(2) }
        rlPhoto.setOnClickListener { showFound(3) }
        mapOf(
            llYoutube to "https://m.facebook.com/", llVimeo to "https://www.tiktok.com/", llBuzzVideo to "https://www.buzzvideos.com/",
            llGoogle to "https://www.google.com/", llSounCloud to "https://mobile.twitter.com/", llYaHoo to "https://www.instagram.com/",
            llIMDB to "https://www.imdb.com/", llTwitch to "https://www.twitch.tv/", llFox to "https://www.foxnews.com/",
            llLiveStream to "https://livestream.com/", llESPN to "https://www.espn.com/", llYTBGaming to "https://www.dailymotion.com",
        ).forEach { (view, url) -> view.setOnClickListener { openSite(url) } }

        btnSearch.setOnClickListener { go() }
        btnSearchCancel.setOnClickListener { setAddress("") }
        etSearchBar.setOnFocusChangeListener { _, focused ->
            if (focused && etSearchBar.text.toString() == getString(R.string.home)) setAddress("")
        }
        etSearchBar.setOnKeyListener { _, keyCode, event ->
            (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER).also { if (it) go() }
        }
        etSearchBar.setOnItemClickListener { _, _, _, _ -> go() }
    }

    /** `onBackPressed`: leaving the first page returns to the site grid. */
    private fun back() {
        val web = binding.simpleWebView
        val index = web.copyBackForwardList().currentIndex
        if (index <= 0) return finish()
        if (index == 1) {
            setAddress("")
            web.isVisible = false
            binding.llContent.isVisible = true
        }
        web.goBack()
    }

    private fun showHome() = with(binding) {
        simpleWebView.isVisible = false
        llContent.isVisible = true
        setAddress("")
    }

    /** `c86`: the two most recent history entries on the start page. */
    private fun showRecent(list: List<HistoryBrowser>) {
        binding.rlHistory.isVisible = list.isNotEmpty()
        recent.submit(list.takeLast(2).reversed())
    }

    /** `s(String)`: open a typed / shared text if it contains a URL. */
    private fun openSite(text: String) {
        val trimmed = text.trim()
        val url = if (URLUtil.isValidUrl(trimmed)) trimmed else URL_PATTERN.toRegex().find(trimmed)?.value
        if (url == null) {
            AlertDialog.Builder(this).setTitle(R.string.Wait).setMessage(R.string.NoUrlFound).show()
            return
        }
        binding.etSearchBar.setText(url)
        go()
    }

    /** `v()`: load the address bar, searching Google for anything that is not a URL. */
    private fun go() = with(binding) {
        simpleWebView.isVisible = true
        llContent.isVisible = false
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(etSearchBar.windowToken, 0)
        if (!Patterns.WEB_URL.matcher(etSearchBar.text).matches()) {
            etSearchBar.setText("https://www.google.com/search?q=" + URLEncoder.encode(etSearchBar.text.toString(), "UTF-8"))
        }
        etSearchBar.dismissDropDown()
        simpleWebView.loadUrl(etSearchBar.text.toString())
    }

    /** `w(String)`. */
    private fun setAddress(text: String) = with(binding) {
        if (text.isEmpty()) {
            btnSearchCancel.visibility = View.INVISIBLE
            etSearchBar.requestFocus()
        } else {
            btnSearchCancel.visibility = View.VISIBLE
        }
        etSearchBar.setText(text)
        etSearchBar.dismissDropDown()
    }

    /** `t()`: clear the found-media counters for a new page. */
    private fun resetFound() = with(binding) {
        synchronized(videos) {
            videos.clear()
            audios.clear()
            photos.clear()
        }
        pageTitle = null
        imvYoutubeBrowserList.setImageResource(R.drawable.ic_yt_list_empty)
        tvSizeAudio.text = "0"
        tvSizeVideo.text = "0"
        tvSizePhoto.text = "0"
    }

    private fun foundOf(kind: Int) = when (kind) {
        1 -> videos
        2 -> audios
        else -> photos
    }

    /** `gb0` / `yw4` / `yr0` case 12: store a sniffed resource and refresh its counter. */
    private fun addFound(found: Found) = runOnUiThread {
        if (isDestroyed) return@runOnUiThread
        val list = foundOf(found.kind)
        synchronized(videos) {
            if (list.any { it.url == found.url }) return@runOnUiThread
            list += found
        }
        val counter = when (found.kind) {
            1 -> binding.tvSizeVideo
            2 -> binding.tvSizeAudio
            else -> binding.tvSizePhoto
        }
        counter.text = list.size.toString()
        binding.imvYoutubeBrowserList.setImageResource(R.drawable.ic_yt_list)
        tada()
    }

    /** `YoYo Techniques.Tada` on the list button. */
    private fun tada() = ObjectAnimator.ofPropertyValuesHolder(
        binding.imvYoutubeBrowserList,
        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.9f, 1.1f, 1.1f, 1f),
        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.9f, 1.1f, 1.1f, 1f),
        PropertyValuesHolder.ofFloat(View.ROTATION, 0f, -3f, 3f, -3f, 0f),
    ).apply {
        duration = 300
        repeatCount = 5
    }.start()

    private inner class Client : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            resetFound()
            binding.loadingPageProgress.progress = 0
            binding.loadingPageProgress.isVisible = true
            progressTimer?.cancel()
            progressTimer = object : CountDownTimer(1100, 1000) {
                override fun onTick(left: Long) {
                    if (binding.loadingPageProgress.progress < 80) binding.loadingPageProgress.progress += 8
                }

                override fun onFinish() = Unit
            }.start()
            super.onPageStarted(view, url, favicon)
            setAddress(url)
        }

        /** `g67`. */
        override fun onPageFinished(view: WebView, url: String) {
            pageTitle = view.title
            progressTimer?.cancel()
            binding.loadingPageProgress.progress = 100
            Handler(Looper.getMainLooper()).postDelayed({ if (!isDestroyed) binding.loadingPageProgress.isVisible = false }, 500)
            if (videos.isNotEmpty() || audios.isNotEmpty() || photos.isNotEmpty()) binding.imvYoutubeBrowserList.setImageResource(R.drawable.ic_yt_list)
            tada()
        }

        /** `yr0` case 13: record history, then sniff the resource unless the site is restricted. */
        override fun onLoadResource(view: WebView, resource: String) {
            binding.imvYoutubeBrowserBack.setImageResource(if (view.canGoBack()) R.drawable.ic_yt_back else R.drawable.ic_yt_back_black)
            binding.imvYoutubeBrowserForward.setImageResource(if (view.canGoForward()) R.drawable.ic_yt_next_black else R.drawable.ic_yt_next)
            val url = view.url.orEmpty()
            val title = view.title.orEmpty()
            if ((title != lastTitle || url != lastUrl) && url.isNotEmpty() && !url.startsWith("about:") && title.isNotEmpty() && !title.contains("about:blank")) {
                lastTitle = title
                lastUrl = url
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                BrowserHistory.add(HistoryBrowser(0, title, url, BrowserHistory.siteOf(url), date))
            }
            val address = binding.etSearchBar.text.toString().lowercase()
            if (restricted.any { address.contains(it) }) return
            val page = view.url
            val pageName = view.title
            runCatching { sniffer.execute { sniff(resource, page, pageName) } }
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            AdBlocker.intercept(request.url.toString()) ?: super.shouldInterceptRequest(view, request)

        /** `intent://` links open the target app or their browser fallback. */
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (!url.startsWith("intent://")) return false
            return runCatching {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                val fallback = intent.getStringExtra("browser_fallback_url")
                if (intent.scheme == "http" || intent.scheme == "https") {
                    fallback?.let(view::loadUrl)
                } else if (packageManager.resolveActivity(intent, 0) != null) {
                    startActivity(intent)
                } else if (fallback != null) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
                }
                true
            }.getOrDefault(false)
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            AlertDialog.Builder(this@WebCastActivity)
                .setTitle("Security Warning")
                .setMessage("Unverified website. There may be some security risks. Do you want to continue?")
                .setPositiveButton("continue") { _, _ -> handler.proceed() }
                .setNegativeButton("cancel") { _, _ -> handler.cancel() }
                .show()
        }
    }

    /** `h67.run`: classify a loaded resource URL. */
    private fun sniff(resource: String, page: String?, title: String?) {
        val lower = resource.lowercase()
        if (imageFilters.any { lower.contains(it) }) addFound(Found(title.orEmpty(), resource, 3, null))
        if (audioFilters.any { lower.contains(it) }) addFound(Found("image", resource, 2, null))
        val isVideo = videoFilters.any { lower.contains(it) } && runCatching {
            val u = URI(lower)
            val path = URI(u.scheme, u.authority, u.path, null, u.fragment).toString()
            listOf(".js", ".css", ".svg", ".ts").none { path.endsWith(it) }
        }.getOrDefault(false) && adFilters.none { lower.contains(it) }
        if (!isVideo || page == null) return
        runCatching {
            val connection = URL(resource).openConnection() as HttpURLConnection
            try {
                connection.connect()
                val type = connection.getHeaderField("content-type")?.lowercase() ?: return
                if (!(type.contains("video") || type.contains("audio") || type == "application/octet-stream" || type == "application/mp4")) return
                if (URL(page).host.contains("twitter.com") && type == "video/mp2t") return
                // `h67.a`: drop the `&range` part so the whole file is fetched; only files of 1 MB or more are listed (`pk6.k`).
                val location = connection.getHeaderField("Location") ?: connection.url.toString()
                val full = location.substringBefore("&range")
                val length = if (full == location) connection.getHeaderField("content-length")
                else (URL(full).openConnection() as HttpURLConnection).run { connect(); getHeaderField("content-length").also { disconnect() } }
                val size = length?.toLongOrNull() ?: return
                if (size < 1024L * 1024 || full.startsWith("blob")) return
                val name = if (type.contains("audio")) "[AUDIO ONLY]" + title.orEmpty() else title.orEmpty()
                addFound(Found(name, full, 1, formatSize(size)))
            } finally {
                connection.disconnect()
            }
        }
    }

    /** `nt`: list of found resources of one kind. */
    private fun showFound(kind: Int) {
        val items = synchronized(videos) { foundOf(kind).toList() }
        // `pk6.E()`: videos take the page title once it is known.
        if (kind == 1) pageTitle?.let { t -> items.forEach { it.name = t } }
        val dialogBinding = AvailableFilesDialogBinding.inflate(layoutInflater)
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        dialogBinding.imvNoFile.isVisible = items.isEmpty()
        dialogBinding.imvClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.resultRecyclerView.layoutManager = LinearLayoutManager(this)
        dialogBinding.resultRecyclerView.adapter = FoundAdapter(items) { found, position, preview ->
            if (preview) preview(found) else cast(found, items, position)
        }
        dialog.show()
    }

    /** `wm1` case 2. */
    private fun cast(found: Found, items: List<Found>, position: Int) = withConnection {
        when (found.kind) {
            3 -> CastSession.start(CastType.PHOTO_BROWSER, items.map { MediaItem(it.url, it.name) }, position)
            2 -> CastSession.start(CastType.AUDIO_OFFLINE, items.map { MediaItem(it.url, it.name) }, position)
            else -> CastSession.start(CastType.VIDEO_BROWSER, listOf(MediaItem(found.url, found.name)), 0)
        }
        open(CastMediaActivity::class.java)
    }

    /** `bh5`: photos open `yg1` (zoomable image), videos / audio play in `hx7`. */
    private fun preview(found: Found) {
        if (found.kind == 3) {
            val b = DialogShowImageBinding.inflate(layoutInflater)
            val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply { setContentView(b.root) }
            Glide.with(this).load(found.url).placeholder(R.drawable.ic_video_default).into(b.myZoomageView)
            b.imvExit.setOnClickListener { dialog.dismiss() }
            dialog.show()
            return
        }
        val b = VideoPlayerBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            setContentView(b.root)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        b.videoview.setVideoURI(Uri.parse(found.url))
        b.videoview.setOnPreparedListener {
            b.bufferingTextview.isVisible = false
            it.start()
        }
        dialog.setOnDismissListener { b.videoview.stopPlayback() }
        dialog.show()
    }

    private class FoundAdapter(private val items: List<Found>, private val onClick: (Found, Int, Boolean) -> Unit) :
        RecyclerView.Adapter<FoundAdapter.Holder>() {
        class Holder(val binding: DownloadResultListBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(DownloadResultListBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
            val found = items[position]
            if (!found.size.isNullOrEmpty()) txtVidSize.text = found.size
            tvFilmName.text = found.name
            Glide.with(root).load(found.url).placeholder(R.drawable.ic_video_default).error(R.drawable.ic_video_default).into(ivPoster)
            ivPoster.setOnClickListener { if (found.kind == 3) onClick(found, position, true) }
            btnPreview.setOnClickListener { onClick(found, position, true) }
            btnDownload.setOnClickListener { onClick(found, position, false) }
        }
    }

    /** `rt0` type 0 (`item_content_history`). */
    private inner class RecentAdapter : RecyclerView.Adapter<RecentAdapter.Holder>() {
        inner class Holder(val binding: ItemContentHistoryBinding) : RecyclerView.ViewHolder(binding.root)

        private var items: List<HistoryBrowser> = emptyList()

        @SuppressLint("NotifyDataSetChanged")
        fun submit(list: List<HistoryBrowser>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemContentHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
            val item = items[position]
            tvTitle.text = item.title
            tvLink.text = item.linkUrl
            imvThumb.setImageResource(BrowserHistory.iconOf(item.linkUrl))
            imvRemove.setOnClickListener { BrowserHistory.remove(item) }
            root.setOnClickListener {
                binding.etSearchBar.setText(item.linkUrl)
                go()
            }
        }
    }

    /** `at` / `zs`: Google search suggestions for the address bar. */
    private class SuggestAdapter(private val context: Context) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1) {
        private var items: List<String> = emptyList()

        override fun getCount() = items.size
        override fun getItem(position: Int) = items.getOrNull(position).orEmpty()

        override fun getFilter() = object : Filter() {
            override fun performFiltering(text: CharSequence?): FilterResults {
                val results = FilterResults()
                val q = text?.toString() ?: return results
                if (q.startsWith("http://") || q.startsWith("https://") || q == context.getString(R.string.home)) return results
                runCatching {
                    val body = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=" + URLEncoder.encode(q, "UTF-8"))
                        .readText()
                    val array = JSONArray(body).getJSONArray(1)
                    List(array.length()) { array.getString(it) }
                }.getOrNull()?.let {
                    results.values = it
                    results.count = it.size
                }
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(text: CharSequence?, results: FilterResults?) {
                items = (results?.values as? List<String>) ?: emptyList()
                if (items.isNotEmpty()) notifyDataSetChanged() else notifyDataSetInvalidated()
            }
        }
    }

    companion object {
        private const val URL_PATTERN =
            "\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)(\\w+:)?(([-\\w]+\\.)+(com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|museum|travel|[a-z]{2}))(:[\\d]{1,5})?(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b"

        /** `fl4.v`. */
        fun formatSize(bytes: Long): String {
            val f = DecimalFormat("0.00")
            var v = bytes.toDouble()
            for (unit in listOf("Bytes", "KB", "MB", "GB")) {
                if (v / 1024 <= 1.0) return f.format(v) + " " + unit
                v /= 1024
            }
            return f.format(v) + " TB"
        }
    }
}

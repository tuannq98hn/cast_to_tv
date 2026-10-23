package com.casttotv.screen.mirroring.ui.activities.function.youtube_browser

import android.content.Context
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** `a7`/`b7`: blocks requests to hosts listed in `assets/hosts` (and their sub-domains) inside the in-app browsers. */
object AdBlocker {
    private val hosts = HashSet<String>()
    private val cache = ConcurrentHashMap<String, Boolean>()
    @Volatile
    private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        thread {
            context.assets.open("hosts").bufferedReader().useLines { lines -> synchronized(hosts) { lines.forEach { hosts += it.trim() } } }
            loaded = true
        }
    }

    /** An empty response for blocked URLs, `null` to let the WebView load it. */
    fun intercept(url: String): WebResourceResponse? {
        val blocked = cache.getOrPut(url) { runCatching { isAd(URL(url).host) }.getOrDefault(false) }
        return if (blocked) WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0))) else null
    }

    private fun isAd(host: String?): Boolean {
        if (host.isNullOrEmpty()) return false
        val dot = host.indexOf('.')
        if (dot < 0) return false
        return synchronized(hosts) { host in hosts } || (dot + 1 < host.length && isAd(host.substring(dot + 1)))
    }
}

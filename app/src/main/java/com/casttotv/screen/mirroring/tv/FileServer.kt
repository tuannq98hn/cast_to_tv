package com.casttotv.screen.mirroring.tv

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.URLConnection
import kotlin.random.Random

/**
 * Port of `mt5`: serves local files to the TV as `http://<phone-ip>:<port><absolute path>`, with Range support so
 * TVs can seek in videos.
 */
class FileServer(port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val file = File(session.uri.substringBefore('?'))
        if (!file.isFile) return text(Response.Status.NOT_FOUND, "Not found")
        val mime = URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
        val etag = Integer.toHexString((file.absolutePath + file.lastModified() + file.length()).hashCode())
        val length = file.length()
        val range = session.headers["range"]?.takeIf { it.startsWith("bytes=") }?.removePrefix("bytes=")

        return try {
            if (range != null) {
                val start = range.substringBefore('-').toLongOrNull() ?: 0
                if (start >= length) {
                    return text(Response.Status.RANGE_NOT_SATISFIABLE, "").apply {
                        addHeader("Content-Range", "bytes 0-0/$length")
                        addHeader("ETag", etag)
                    }
                }
                val end = range.substringAfter('-').toLongOrNull()?.coerceAtMost(length - 1) ?: (length - 1)
                val size = (end - start + 1).coerceAtLeast(0)
                val input = FileInputStream(file).apply { skip(start) }
                newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, input, size).apply {
                    addHeader("Accept-Ranges", "bytes")
                    addHeader("Content-Range", "bytes $start-$end/$length")
                    addHeader("ETag", etag)
                }
            } else if (etag == session.headers["if-none-match"]) {
                newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "")
            } else {
                newFixedLengthResponse(Response.Status.OK, mime, FileInputStream(file), length).apply {
                    addHeader("Accept-Ranges", "bytes")
                    addHeader("ETag", etag)
                }
            }
        } catch (e: IOException) {
            text(Response.Status.FORBIDDEN, "Forbidden: Reading file failed")
        }
    }

    private fun text(status: Response.Status, body: String) =
        newFixedLengthResponse(status, MIME_PLAINTEXT, body).apply { addHeader("Accept-Ranges", "bytes") }

    companion object {
        private var server: FileServer? = null

        /** The original picked 8093 and fell back to a free port in 8000..8998. */
        fun ensureRunning(): Int {
            server?.takeIf { it.isAlive }?.let { return it.listeningPort }
            val port = if (isFree(8093)) 8093 else generateSequence { 8000 + Random.nextInt(999) }.first(::isFree)
            return FileServer(port).also { it.start(SOCKET_READ_TIMEOUT, false); server = it }.listeningPort
        }

        fun stop() {
            server?.stop()
            server = null
        }

        /** URL the TV can fetch for [pathOrUrl]; remote URLs pass through unchanged. */
        fun urlFor(context: Context, pathOrUrl: String): String {
            if (pathOrUrl.contains("http")) return pathOrUrl
            val port = ensureRunning()
            return "http://${phoneIp(context)}:$port$pathOrUrl"
        }

        @Suppress("DEPRECATION")
        fun phoneIp(context: Context): String {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return Formatter.formatIpAddress(wifi.connectionInfo.ipAddress)
        }

        private fun isFree(port: Int) = runCatching { ServerSocket(port).close() }.isSuccess
    }
}

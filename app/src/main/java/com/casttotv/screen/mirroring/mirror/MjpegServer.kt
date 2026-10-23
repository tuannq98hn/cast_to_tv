package com.casttotv.screen.mirroring.mirror

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/** HTTP side of the mirror: `/` is the bundled viewer page, `/stream.mjpeg` the multipart JPEG stream. */
class MjpegServer(private val context: Context, port: Int) : NanoHTTPD(port) {

    private val clients = CopyOnWriteArrayList<PipedOutputStream>()

    @Volatile
    private var lastFrame: ByteArray? = null

    fun startServer() = start(SOCKET_READ_TIMEOUT, false)

    /** Pushes a JPEG frame to every connected viewer; slow or closed viewers are dropped. */
    fun publish(jpeg: ByteArray) {
        lastFrame = jpeg
        val part = "--$BOUNDARY\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.size}\r\n\r\n".toByteArray()
        for (client in clients) {
            runCatching {
                client.write(part)
                client.write(jpeg)
                client.write("\r\n".toByteArray())
                client.flush()
            }.onFailure {
                clients.remove(client)
                runCatching { client.close() }
            }
        }
    }

    override fun stop() {
        clients.forEach { runCatching { it.close() } }
        clients.clear()
        super.stop()
    }

    override fun serve(session: IHTTPSession): Response = when (session.uri) {
        "/", "/index.html" -> newFixedLengthResponse(Response.Status.OK, "text/html", page())
        "/stream.mjpeg" -> stream()
        "/favicon.ico" -> asset("mirror/favicon.png", "image/png")
        else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
    }

    private fun page(): String = context.assets.open("mirror/index_mirror.html").bufferedReader().use { it.readText() }
        .replace("BACK_COLOR", "#000000")
        .replace("SCREEN_STREAM_ADDRESS", "/stream.mjpeg")
        .replace("MSG_NO_MJPEG_SUPPORT", "This browser cannot show the MJPEG stream")

    private fun stream(): Response {
        val input = PipedInputStream(4 * 1024 * 1024)
        val output = PipedOutputStream(input)
        clients += output
        lastFrame?.let { frame -> thread { publishTo(output, frame) } }
        return newChunkedResponse(Response.Status.OK, "multipart/x-mixed-replace; boundary=$BOUNDARY", input).apply {
            addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            addHeader("Pragma", "no-cache")
            addHeader("Connection", "close")
        }
    }

    private fun publishTo(client: PipedOutputStream, jpeg: ByteArray) = runCatching {
        client.write("--$BOUNDARY\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.size}\r\n\r\n".toByteArray())
        client.write(jpeg)
        client.write("\r\n".toByteArray())
        client.flush()
    }

    private fun asset(path: String, mime: String): Response {
        val stream: InputStream = context.assets.open(path)
        return newChunkedResponse(Response.Status.OK, mime, stream)
    }

    private companion object {
        const val BOUNDARY = "CastToTVFrame"
    }
}

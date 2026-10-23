package com.casttotv.screen.mirroring.tv

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.InetSocketAddress

/**
 * Port of `WebServer` (service) + its RxJava bus: a TV web browser opens `http://<phone>:8686/` (`assets/web`),
 * fetches `/wsinfo` and connects to the WebSocket on 8268; the app then drives its `<video>` with JSON commands
 * (`{"command":9,"type":1,"url":...}` load, 1 play, 2 pause, 3 stop, 4 volume, 5/6 ff/rew, 7 seek, 12/13 volume ±).
 */
object WebReceiver {
    const val HTTP_PORT = 8686
    const val WS_PORT = 8268

    private var http: NanoHTTPD? = null
    private var ws: WebSocketServer? = null
    private val _clients = MutableStateFlow(0)
    /** Number of connected browser pages (`KEY_CONNECTED_WEB` when it becomes > 0). */
    val clients: StateFlow<Int> = _clients

    fun address(context: Context) = "http://${FileServer.phoneIp(context)}:$HTTP_PORT"

    @Synchronized
    fun start(context: Context) {
        if (http != null) return
        val assets = context.applicationContext.assets
        val ip = FileServer.phoneIp(context)
        http = object : NanoHTTPD(HTTP_PORT) {
            override fun serve(session: IHTTPSession): Response {
                val uri = session.uri.trimStart('/').ifEmpty { "index.html" }
                if (uri == "wsinfo") return newFixedLengthResponse("$ip:$WS_PORT")
                return runCatching {
                    val bytes = assets.open("web/$uri").use { it.readBytes() }
                    newFixedLengthResponse(Response.Status.OK, getMimeTypeForFile(uri), bytes.inputStream(), bytes.size.toLong())
                }.getOrElse { newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found") }
            }
        }.apply { runCatching { start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) } }
        ws = object : WebSocketServer(InetSocketAddress("0.0.0.0", WS_PORT)) {
            override fun onOpen(conn: WebSocket, handshake: ClientHandshake) { _clients.value = connections.size }
            override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
                _clients.value = connections.size
                if (connections.isEmpty()) TvConnect.webDisconnected()
            }
            override fun onMessage(conn: WebSocket, message: String) = Unit
            override fun onError(conn: WebSocket?, ex: Exception) = Unit
            override fun onStart() = Unit
        }.apply {
            isReuseAddr = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        http?.stop()
        http = null
        runCatching { ws?.stop(500) }
        ws = null
        _clients.value = 0
    }

    fun send(command: Int, extra: JSONObject.() -> Unit = {}) {
        val json = JSONObject().put("command", command).put("type", 1).apply(extra).toString()
        runCatching { ws?.broadcast(json) }
    }

    fun load(url: String) = send(9) { put("url", url) }
}

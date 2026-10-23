package com.casttotv.screen.mirroring.tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/** Port of `rm5`/`gn5`/`fn5`: Samsung Tizen remote over the `samsung.remote.control` websocket channel. */
object SamsungRemote {
    interface Callback {
        fun onSuccess()
        fun onFailure(reason: String)
    }

    private const val NAME = "VW5pdmVyc2FsUmVtb3Rl" // base64("UniversalRemote")
    private lateinit var prefs: SharedPreferences
    private var socket: WebSocket? = null
    private var port = 8002
    private var connected = false

    fun init(context: Context) {
        prefs = context.getSharedPreferences("SamsungATV", Context.MODE_PRIVATE)
    }

    /** Opens the channel; 8002 is TLS and returns a token that is reused on the next connection. */
    fun connect(ip: String, port: Int, freshToken: Boolean, callback: Callback?) {
        prefs.edit { putString("IP_SAMSUNG_REMOTE", ip) }
        this.port = port
        connected = false
        socket?.cancel()
        val base = "/api/v2/channels/samsung.remote.control?name=$NAME"
        val url = when {
            port != 8002 -> "ws://$ip:$port$base"
            freshToken -> "wss://$ip:$port$base&token="
            else -> "wss://$ip:$port$base&token=" + prefs.getString("TOKEN_SS_REMOTE", "")
        }
        socket = trustAllClient().newWebSocket(Request.Builder().url(url).build(), Listener(callback))
    }

    /** Sends an Android-style key name (e.g. `KEYCODE_DPAD_UP`) mapped to the Samsung key. */
    fun sendKey(androidKey: String) {
        val key = KEYS[androidKey] ?: return
        send(JSONObject().put("method", "ms.remote.control").put("params", JSONObject()
            .put("Cmd", "Click").put("DataOfCmd", key).put("Option", "false").put("TypeOfRemote", "SendRemoteKey")))
    }

    fun send(json: JSONObject) {
        if (connected) socket?.send(json.toString())
    }

    private class Listener(private val callback: Callback?) : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val event = runCatching { JSONObject(text).getString("event") }.getOrNull() ?: return
            when {
                event == "ms.channel.clientDisconnect" -> {
                    connected = false
                    webSocket.close(1000, "disconnected")
                }
                event == "ms.channel.unauthorized" && !connected -> callback?.onFailure("data_null")
                event == "ms.channel.connect" && !connected -> {
                    if (port == 8002) {
                        runCatching { JSONObject(text).getJSONObject("data").getString("token") }
                            .getOrNull()?.let { prefs.edit { putString("TOKEN_SS_REMOTE", it) } }
                    }
                    connected = true
                    callback?.onSuccess()
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (t.message?.lowercase()?.contains("failed to connect to") == true) callback?.onFailure("error")
        }
    }

    // ponytail: trust-all TLS mirrors the original (TVs use self-signed certs); only used for the LAN TV socket.
    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private fun trustAllClient(): OkHttpClient {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustAll), SecureRandom()) }
        return OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trustAll)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private val KEYS = mapOf(
        "KEYCODE_POWER" to "KEY_POWER", "KEYCODE_DPAD_UP" to "KEY_UP", "KEYCODE_DPAD_DOWN" to "KEY_DOWN",
        "KEYCODE_DPAD_LEFT" to "KEY_LEFT", "KEYCODE_DPAD_RIGHT" to "KEY_RIGHT", "KEYCODE_CHANNEL_UP" to "KEY_CHUP",
        "KEYCODE_CHANNEL_DOWN" to "KEY_CHDOWN", "KEYCODE_ENTER" to "KEY_ENTER", "KEYCODE_BACK" to "KEY_RETURN",
        "KEYCODE_TV_CONTENTS_MENU" to "KEY_CH_LIST", "KEYCODE_MENU" to "KEY_MENU", "KEYCODE_TV" to "KEY_SOURCE",
        "KEYCODE_GUIDE" to "KEY_GUIDE", "KEYCODE_SETTINGS" to "KEY_TOOLS", "KEYCODE_INFO" to "KEY_INFO",
        "KEYCODE_PROG_RED" to "KEY_RED", "KEYCODE_PROG_GREEN" to "KEY_GREEN", "KEYCODE_PROG_YELLOW" to "KEY_YELLOW",
        "KEYCODE_PROG_BLUE" to "KEY_BLUE", "KEYCODE_MEDIA_NEXT" to "KEY_PANNEL_CHDOWN", "KEYCODE_VOLUME_UP" to "KEY_VOLUP",
        "KEYCODE_VOLUME_DOWN" to "KEY_VOLDOWN", "KEYCODE_VOLUME_MUTE" to "KEY_MUTE",
        "KEYCODE_0" to "KEY_0", "KEYCODE_1" to "KEY_1", "KEYCODE_2" to "KEY_2", "KEYCODE_3" to "KEY_3", "KEYCODE_4" to "KEY_4",
        "KEYCODE_5" to "KEY_5", "KEYCODE_6" to "KEY_6", "KEYCODE_7" to "KEY_7", "KEYCODE_8" to "KEY_8", "KEYCODE_9" to "KEY_9",
        "KEYCODE_LAST_CHANNEL" to "KEY_PRECH", "KEYCODE_MEDIA_REWIND" to "KEY_REWIND", "KEYCODE_MEDIA_RECORD" to "KEY_REC",
        "KEYCODE_HOME" to "KEY_HOME", "KEY_APP_LIST" to "KEYCODE_ALL_APPS", "KEYCODE_CAPTIONS" to "KEY_CAPTION",
        "KEYCODE_CLEAR" to "KEY_CLEAR", "KEYCODE_ESCAPE" to "KEY_EXIT", "KEYCODE_MEDIA_FAST_FORWARD" to "KEY_FF",
        "KEYCODE_MEDIA_PLAY" to "KEY_PLAY", "KEYCODE_MEDIA_PLAY_PAUSE" to "KEY_PAUSE", "KEYCODE_SLEEP" to "KEY_SLEEP",
        "KEYCODE_MUSIC" to "KEY_SOUND_MODE", "KEYCODE_MEDIA_STOP" to "KEY_STOP", "KEYCODE_AVR_INPUT" to "KEY_VCR_MODE",
        "KEYCODE_DISPLAY" to "KEYCODE_DISPLAY", "KEYCODE_3D_MODE" to "KEYCODE_3D_MODE", "KEYCODE_NETFLIX" to "KEYCODE_NETFLIX",
        "KEYCODE_YOUTUBE" to "KEYCODE_YOUTUBE", "KEY_SOURCE" to "KEY_SOURCE", "KEY_ID_SETUP" to "KEY_ID_SETUP",
        "KEY_INFO" to "KEY_INFO", "KEY_SETTINGS" to "KEY_SETTINGS", "KEY_MENU" to "KEY_MENU", "KEY_CH_LIST" to "KEY_CH_LIST",
        "APPLE_TV" to "APPLE_TV",
    )
}

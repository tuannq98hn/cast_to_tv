package com.casttotv.screen.mirroring.mirror

import com.casttotv.screen.mirroring.tv.DeviceType
import com.casttotv.screen.mirroring.tv.SamsungRemote
import com.casttotv.screen.mirroring.tv.SonyRemote
import com.casttotv.screen.mirroring.tv.TvConnect
import com.connectsdk.core.MediaInfo
import com.connectsdk.service.capability.MediaPlayer
import com.connectsdk.service.command.ServiceCommandError
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.concurrent.thread

/** How the running MJPEG stream is put on a screen (`my3.o()` for the connected TV, `mk4.i` for the web relay). */
object MirrorTargets {

    private val http = OkHttpClient()
    private var relay: Socket? = null

    /** Direct mirror: open the stream on the connected TV in the way each brand supports. */
    fun showOnTv(stream: MirrorService.State.Streaming) {
        val device = TvConnect.device ?: return
        when (TvConnect.typeOf(device)) {
            DeviceType.SAMSUNG -> SamsungRemote.send(
                JSONObject(
                    """{"method":"ms.channel.emit","params":{"event": "ed.apps.launch", "to":"host", "data":""" +
                        """{"appId":"org.tizen.browser","action_type":"NATIVE_LAUNCH","metaTag":"${stream.address}"}}}""",
                ),
            )
            DeviceType.ROKU -> post(
                "http://${device.ipAddress}:8060/input/714008?uri=${enc(stream.address)}&typeapi=start" +
                    "&title=${enc("Screen Mirroring")}&typecast=mirror&height=720&width=1280",
            )
            DeviceType.SONY -> SonyRemote.setActiveApp("localapp://webappruntime?url=${enc(stream.streamUrl)}")
            else -> TvConnect.castDevice?.getCapability(MediaPlayer::class.java)?.displayImage(
                MediaInfo.Builder(stream.streamUrl, "image/jpeg").setTitle("Screen Mirroring").build(),
                object : MediaPlayer.LaunchListener {
                    override fun onSuccess(result: MediaPlayer.MediaLaunchObject) = Unit
                    override fun onError(error: ServiceCommandError) = Unit
                },
            )
        }
    }

    /**
     * Web mirror: tell the bcastmirror.com page whose QR code was scanned ([key]) where the stream is.
     * [onStatus] receives the page's `msgStatusConnect` answer (false on socket errors).
     */
    fun showOnWebPage(key: String, address: String, onStatus: (Boolean) -> Unit) {
        relay?.disconnect()
        val socket = IO.socket("https://bcastmirror.com/")
        relay = socket
        socket.on(Socket.EVENT_CONNECT) {
            val payload = JSONObject()
                .put("socketIdDevice", socket.id())
                .put("key", key)
                .put("ipShareFromDevice", address)
                .put("feat", "mirror")
                .put("appName", "roku_universal")
            socket.emit("msgSocket", payload)
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) { onStatus(false) }
        socket.on("msgStatusConnect") { args ->
            onStatus(runCatching { JSONArray(args).getJSONObject(0).getBoolean("status") }.getOrDefault(false))
        }
        socket.connect()
    }

    fun disconnectRelay() {
        relay?.disconnect()
        relay = null
    }

    private fun post(url: String) = thread {
        runCatching {
            http.newCall(
                Request.Builder().url(url).post("".toRequestBody("text/plain".toMediaType()))
                    .header("User-Agent", "CastApp/3 CFNetwork/1331.0.7 Darwin/21.4.0").build(),
            ).execute().close()
        }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}

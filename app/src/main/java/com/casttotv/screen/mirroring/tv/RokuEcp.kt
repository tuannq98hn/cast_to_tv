package com.casttotv.screen.mirroring.tv

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Roku External Control Protocol on port 8060. Replaces the obfuscated jaku client (`v23`/`RequestTask`) and the
 * raw OkHttp calls the original made for `input/15985`.
 */
object RokuEcp {
    /** Key names indexed like the original `l82.b` table (1 = Home ... 30 = Lit_). */
    enum class Key(val ecp: String) {
        HOME("Home"), REV("Rev"), FWD("Fwd"), PLAY("Play"), SELECT("Select"), LEFT("Left"), RIGHT("Right"),
        DOWN("Down"), UP("Up"), BACK("Back"), INSTANT_REPLAY("InstantReplay"), INFO("Info"), BACKSPACE("Backspace"),
        SEARCH("Search"), ENTER("Enter"), FIND_REMOTE("FindRemote"), VOLUME_DOWN("VolumeDown"), VOLUME_MUTE("VolumeMute"),
        VOLUME_UP("VolumeUp"), POWER_OFF("PowerOff"), POWER_ON("PowerOn"), CHANNEL_UP("ChannelUp"),
        CHANNEL_DOWN("ChannelDown"), INPUT_TUNER("InputTuner"), INPUT_HDMI1("InputHDMI1"), INPUT_HDMI2("InputHDMI2"),
        INPUT_HDMI3("InputHDMI3"), INPUT_HDMI4("InputHDMI4"), INPUT_AV1("InputAV1"),
    }

    /** The app's own Roku channel ("Cast to TV"), installed on demand and used to show cast media. */
    const val CHANNEL_ID = "714008"

    private val client = OkHttpClient()
    private val io = Executors.newSingleThreadExecutor()

    private fun base(): String? = TvConnect.castDevice?.ipAddress?.let { "http://$it:8060" }

    fun keypress(key: Key) = post("/keypress/${key.ecp}")

    /** Literal text entry (`Lit_<char>`). */
    fun literal(char: Char) = post("/keypress/Lit_" + URLEncoder.encode(char.toString(), "UTF-8"))

    fun launch(appId: String = CHANNEL_ID) = post("/launch/$appId")

    /** Channel Store page of [appId] (`oz2` type 0: `launch/11?contentId=`). */
    fun openStore(appId: String = CHANNEL_ID) = post("/launch/11?contentId=$appId")

    /**
     * `input/15985?t=p|v&...`: hands a media URL to the cast receiver channel. [onResult] gets the HTTP code
     * (on a background thread).
     */
    fun castMedia(url: String, video: Boolean, onResult: (Int) -> Unit = {}) {
        val query = if (video) "t=v&h=a&k=a&u=" else "t=p&h=a&u="
        post("/input/15985?$query" + URLEncoder.encode(url, "UTF-8"), onResult)
    }

    /** `query/apps`: installed channel ids, cached in [TvConnect.rokuChannels]. */
    fun refreshChannels(onDone: () -> Unit = {}) {
        val base = base() ?: return
        io.execute {
            runCatching {
                client.newCall(Request.Builder().url("$base/query/apps").build()).execute().use { r ->
                    val ids = Regex("""<app id="([^"]+)"""").findAll(r.body?.string().orEmpty()).map { it.groupValues[1] }.toList()
                    TvConnect.rokuChannels.clear()
                    TvConnect.rokuChannels += ids
                }
            }
            onDone()
        }
    }

    private fun post(path: String, onResult: (Int) -> Unit = {}) {
        val base = base() ?: return
        io.execute {
            val code = runCatching {
                val request = Request.Builder().url(base + path)
                    .post("".toRequestBody("text/plain".toMediaType()))
                    .header("User-Agent", "CastApp/3 CFNetwork/1331.0.7 Darwin/21.4.0")
                    .build()
                client.newCall(request).execute().use { it.code }
            }.getOrDefault(-1)
            onResult(code)
        }
    }
}

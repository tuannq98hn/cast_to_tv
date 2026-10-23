package com.casttotv.screen.mirroring.tv

import android.os.Build
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.Executors

/** Port of `dd5`: Sony Bravia registration (`/sony/accessControl`) and IRCC key codes (`/sony/IRCC`). */
object SonyRemote {
    enum class PairResult { PAIRED, NEED_PIN, FAILED }

    /** Auth cookie returned by a successful registration (`dd5.f1310a`). */
    @Volatile
    var cookie = ""
        private set

    private val http = OkHttpClient()
    private val io = Executors.newSingleThreadExecutor()
    private val nickname = "Sony Remote App (WiFi) - " + Build.MODEL
    private val clientId = "$nickname-2021"

    private fun host() = TvConnect.device?.ipAddress

    /**
     * `actRegister`. Without [pin] the TV answers with an error and shows a PIN on screen; calling again with that
     * PIN returns the auth cookie.
     */
    fun register(pin: String?, onResult: (PairResult) -> Unit) {
        val host = host() ?: return onResult(PairResult.FAILED)
        io.execute {
            val body = """{"id":13,"method":"actRegister","version":"1.0","params":[{"clientid":"$clientId","nickname":"$nickname"},""" +
                """[{"clientid":"$clientId","value":"yes","nickname":"$nickname","function":"WOL"}]]}"""
            val auth = "Basic " + Base64.encodeToString((pin?.let { ":$it" } ?: "").toByteArray(), Base64.NO_WRAP)
            val result = runCatching {
                http.newCall(
                    Request.Builder().url("http://$host/sony/accessControl")
                        .header("Authorization", auth)
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .build(),
                ).execute().use { response ->
                    response.header("Set-Cookie")?.takeIf { it.isNotEmpty() }?.let { cookie = it }
                    if (JSONObject(response.body?.string().orEmpty()).has("error")) PairResult.NEED_PIN else PairResult.PAIRED
                }
            }.getOrDefault(PairResult.FAILED)
            onResult(result)
        }
    }

    /** Sends one base64 IRCC code; [onResult] gets the HTTP status (403 means the registration expired). */
    fun send(ircc: String, onResult: (Int) -> Unit = {}) {
        val host = host() ?: return
        io.execute {
            val envelope = """<s:Envelope
    xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
    s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
    <s:Body>
        <u:X_SendIRCC xmlns:u="urn:schemas-sony-com:service:IRCC:1">
            <IRCCCode>$ircc</IRCCCode>
        </u:X_SendIRCC>
    </s:Body>
</s:Envelope>"""
            val code = runCatching {
                http.newCall(
                    Request.Builder().url("http://$host/sony/IRCC")
                        .header("SOAPACTION", "\"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC\"")
                        .header("X-Auth-PSK", "1234")
                        .header("Cookie", cookie)
                        .post(envelope.toRequestBody("application/xml".toMediaType()))
                        .build(),
                ).execute().use { it.code }
            }.getOrDefault(-1)
            onResult(code)
        }
    }

    /** `setActiveApp` on `/sony/appControl` (apps list and web-runtime launches). */
    fun setActiveApp(uri: String) {
        val host = host() ?: return
        io.execute {
            runCatching {
                http.newCall(
                    Request.Builder().url("http://$host/sony/appControl")
                        .header("X-Auth-PSK", "1234")
                        .header("Cookie", cookie)
                        .post("""{"method":"setActiveApp","id":601,"params":[{"uri":"$uri"}],"version":"1.0"}""".toRequestBody("application/json".toMediaType()))
                        .build(),
                ).execute().close()
            }
        }
    }
}

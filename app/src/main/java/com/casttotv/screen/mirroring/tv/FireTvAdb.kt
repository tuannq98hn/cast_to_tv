package com.casttotv.screen.mirroring.tv

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File
import java.util.concurrent.Executors

/**
 * Replaces the obfuscated ADB client (`p22`): Fire TV is driven through `adb shell` over Wi-Fi (port 5555),
 * e.g. `input keyevent 19` or `am start -n <component>`. The TV must have ADB debugging enabled and accept the key.
 */
object FireTvAdb {
    private val io = Executors.newSingleThreadExecutor()
    private var dadb: Dadb? = null
    private var host: String? = null

    val isConnected get() = dadb != null

    /** Connects once per host; [onResult] runs on a background thread. */
    fun connect(context: Context, host: String, onResult: (Boolean) -> Unit) {
        io.execute {
            if (dadb != null && this.host == host) return@execute onResult(true)
            close()
            val result = runCatching {
                val dir = context.filesDir
                val private = File(dir, "adbkey")
                val public = File(dir, "adbkey.pub")
                if (!private.exists()) AdbKeyPair.generate(private, public)
                Dadb.create(host, 5555, AdbKeyPair.read(private, public)).also {
                    it.shell("echo ok")
                    dadb = it
                    this.host = host
                }
            }
            onResult(result.isSuccess)
        }
    }

    fun shell(command: String) {
        io.execute { runCatching { dadb?.shell(command) }.onFailure { close() } }
    }

    /** Installed package names (`pm list packages`); blocking, call off the main thread. */
    fun listPackages(): Set<String> =
        dadb?.shell("pm list packages")?.output?.lineSequence()?.map { it.removePrefix("package:").trim() }?.filter { it.isNotEmpty() }?.toSet()
            ?: emptySet()

    fun close() {
        runCatching { dadb?.close() }
        dadb = null
        host = null
    }
}

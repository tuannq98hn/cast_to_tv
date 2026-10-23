package com.casttotv.screen.mirroring.tv

import com.connectsdk.device.ConnectableDevice
import com.connectsdk.service.AndroidService
import com.connectsdk.service.capability.MediaControl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.Locale

/** Port of `com.connectsdk.TVConnectController` (app-side connection state) + `hq4` device classification. */
object TvConnect {
    enum class Event { CONNECT, DISCONNECT }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)

    /** Replaces the EventBus `MessageEvent("connect"/"disconnect")` broadcasts. */
    val events: SharedFlow<Event> = _events

    var device: ConnectableDevice? = null
    var deviceAndroidTv: ConnectableDevice? = null
    var deviceChromecast: ConnectableDevice? = null
    var deviceClicked: ConnectableDevice? = null
    var mediaControl: MediaControl? = null
    var isConnectWeb = false
    var nameDeviceSave = ""
    var nameService = ""
    var nameTvContain = ""
    var openNotify = false
    var volume = 0.1f

    /** Roku channel ids reported by the TV (`query/apps`); filled by the Roku helpers. */
    val rokuChannels = mutableListOf<String>()

    val isConnected: Boolean get() = isConnectWeb || device?.isConnected == true

    /** The device to use for media casting: Chromecast twin of an Android TV when present. */
    val castDevice: ConnectableDevice?
        get() = deviceChromecast?.takeIf { device?.serviceId == AndroidService.ID } ?: device

    val deviceName: String
        get() {
            if (isConnectWeb) nameDeviceSave = "Connected WebBrowser"
            if (nameDeviceSave.isNotEmpty()) return nameDeviceSave
            return device?.let { it.friendlyName ?: it.modelName } ?: "No TV connection"
        }

    fun connected(connected: ConnectableDevice) {
        device = connected
        _events.tryEmit(Event.CONNECT)
        // `TVConnectController.setRokuChanelList`: installed channels decide whether the cast channel must be added.
        if (typeOf(connected) == DeviceType.ROKU) RokuEcp.refreshChannels()
    }

    /** Browser receiver connected (`FileBrowserActivity.t`). */
    fun webConnected() {
        isConnectWeb = true
        _events.tryEmit(Event.CONNECT)
    }

    /** `KEY_DISCONNECTED_WEB`. */
    fun webDisconnected() {
        if (!isConnectWeb) return
        isConnectWeb = false
        nameDeviceSave = ""
        _events.tryEmit(Event.DISCONNECT)
    }

    fun disconnect() {
        if (isConnectWeb) WebReceiver.stop()
        isConnectWeb = false
        device?.disconnect()
        device = null
        deviceAndroidTv = null
        _events.tryEmit(Event.DISCONNECT)
    }

    /** `hq4.q()`: type of the connected (or last clicked) device. */
    fun currentType(): DeviceType = (device ?: deviceClicked)?.let(::typeOf) ?: DeviceType.OTHER

    /** `hq4.r()`: match service names / manufacturer / model first, then the friendly name. */
    fun typeOf(device: ConnectableDevice): DeviceType =
        DeviceType.entries.firstOrNull { it.keys.isNotEmpty() && matchesService(device, it.keys) }
            ?: DeviceType.entries.firstOrNull { it.keys.isNotEmpty() && matchesName(device, it.keys) }
            ?: DeviceType.OTHER

    private fun matchesService(device: ConnectableDevice, keys: List<String>): Boolean {
        val services = device.connectedServiceNames?.lowercase(Locale.getDefault()).orEmpty()
        val manufacturer = device.serviceDescription?.manufacturer?.lowercase().orEmpty()
        val model = device.serviceDescription?.modelName?.lowercase().orEmpty()
        val serviceId = device.serviceId?.lowercase().orEmpty()
        return keys.any { k ->
            services.contains(k.lowercase()) || manufacturer.contains(k) || model.contains(k) || serviceId.contains(k)
        }
    }

    private fun matchesName(device: ConnectableDevice, keys: List<String>): Boolean {
        val name = if (device.connectedServiceNames != null) device.friendlyName?.lowercase(Locale.getDefault()).orEmpty() else ""
        return keys.any { name.contains(it.lowercase()) }
    }
}

/** Device families as named by the original app (the `id` strings are used in prefs/analytics). */
enum class DeviceType(val id: String, val keys: List<String>) {
    ROKU("RokuTV", listOf("roku")),
    LG("LG", listOf("lg", "webos", "web os")),
    SAMSUNG("SamsungTV", listOf("samsung", "[tv]")),
    SONY("SonyTV", listOf("sony", "bravia")),
    ANDROID_TV("AndroidTV", listOf("AndroidTV", "AndroidTV2")),
    CHROMECAST("ChromeCastTV", listOf("chromecast")),
    TCL("TclTV", listOf("tcl")),
    FIRE_TV("FireTV", listOf("firetv", "fire tv")),
    PANASONIC("PanasonicTV", listOf("panasonic", "veria", "_series")),
    OTHER("Other", emptyList()),
}

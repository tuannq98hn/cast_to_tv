package com.casttotv.screen.mirroring.ui.activities.function.connect

import android.app.Activity
import android.app.Dialog
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.tv.DeviceType
import com.casttotv.screen.mirroring.tv.SamsungRemote
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.dialogs.PairingAlertDialog
import com.casttotv.screen.mirroring.ui.dialogs.PinCodeDialog
import com.casttotv.screen.mirroring.ui.dialogs.SimpleDialog
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.device.ConnectableDeviceListener
import com.connectsdk.service.DIALService
import com.connectsdk.service.DeviceService
import com.connectsdk.service.FireTVService
import com.connectsdk.service.command.ServiceCommandError

/** `tc6` (`utils.TVConnect`) + the connect half of `SearchTVActivity`: choose a service, run pairing, report the outcome. */
class TvConnector(
    private val activity: Activity,
    private val onConnected: (ConnectableDevice) -> Unit,
    private val onFailed: () -> Unit,
) {
    private var selectedName = ""
    private var handled = false
    private var pairingAlert: PairingAlertDialog? = null
    private var pinDialog: PinCodeDialog? = null
    private var samsungDialog: Dialog? = null

    private val listener = object : ConnectableDeviceListener {
        override fun onDeviceReady(device: ConnectableDevice) {
            if (handled) return
            handled = true
            activity.runOnUiThread {
                TvConnect.nameDeviceSave = selectedName
                dismissSamsung()
                TvConnect.connected(device)
                onConnected(device)
            }
        }

        override fun onDeviceDisconnected(device: ConnectableDevice) = Unit

        override fun onPairingRequired(device: ConnectableDevice, service: DeviceService, type: DeviceService.PairingType) {
            activity.runOnUiThread {
                when (type) {
                    DeviceService.PairingType.FIRST_SCREEN -> pairingAlert?.show()
                    DeviceService.PairingType.PIN_CODE, DeviceService.PairingType.MIXED -> pinDialog?.show()
                    else -> Unit
                }
            }
        }

        override fun onCapabilityUpdated(device: ConnectableDevice, added: MutableList<String>, removed: MutableList<String>) = Unit

        override fun onConnectionFailed(device: ConnectableDevice, error: ServiceCommandError) {
            activity.runOnUiThread {
                device.removeListener(this)
                device.disconnect()
                dismissSamsung()
                onFailed()
            }
        }
    }

    /** `nq5`: pick which of the grouped services to connect with. */
    fun connect(tv: SmartTv) {
        TvConnect.nameService = tv.services
        selectedName = tv.name
        TvConnect.nameDeviceSave = selectedName
        val devices = tv.devices
        if (devices.size == 1) {
            TvConnect.nameTvContain = tv.name
            connect(devices[0])
            return
        }
        val androidTv = devices.firstOrNull { it.serviceId.contains("AndroidTV") }
        val chromecast = devices.firstOrNull { it.serviceId.contains("Chromecast") }
        if (androidTv != null && chromecast != null) {
            TvConnect.deviceAndroidTv = androidTv
            TvConnect.deviceChromecast = chromecast
        }
        val preferred = chromecast ?: androidTv
            ?: devices.firstOrNull { it.connectedServiceNames?.let { s -> s.equals("webOS TV", true) || s.equals("FireTV", true) || s.equals("Roku", true) } == true }
            ?: devices[0]
        connect(preferred)
    }

    /** `fq5.a`: Samsung needs the remote websocket accepted on the TV first. */
    private fun connect(device: ConnectableDevice) {
        TvConnect.deviceClicked = device
        if (TvConnect.typeOf(device) != DeviceType.SAMSUNG) {
            startConnect(device)
            return
        }
        samsungDialog = SimpleDialog.show(activity, R.layout.dialog_check_samsung, closeId = R.id.imvClose)
        val callback = object : SamsungRemote.Callback {
            override fun onSuccess() = activity.runOnUiThread { startConnect(device) }
            override fun onFailure(reason: String) {
                SamsungRemote.connect(device.ipAddress, 8001, true, object : SamsungRemote.Callback {
                    override fun onSuccess() = activity.runOnUiThread { startConnect(device) }
                    override fun onFailure(reason: String) = activity.runOnUiThread { startConnect(device) }
                })
            }
        }
        SamsungRemote.connect(device.ipAddress, 8002, false, callback)
    }

    /** `kq5`: hook the listener and let ConnectSDK drive pairing. */
    private fun startConnect(device: ConnectableDevice) {
        handled = false
        device.addListener(listener)
        device.setPairingType(DeviceService.PairingType.PIN_CODE)
        device.connect()
        pairingAlert = PairingAlertDialog(activity) {
            TvConnect.device?.let {
                it.removeListener(listener)
                it.disconnect()
            }
        }
        pinDialog = PinCodeDialog(
            activity,
            numeric = true,
            onCancel = { if (TvConnect.typeOf(device) == DeviceType.LG) device.sendPairingKey("000000") },
        ) { pin -> device.sendPairingKey(pin) }
    }

    private fun dismissSamsung() {
        samsungDialog?.takeIf { it.isShowing }?.dismiss()
    }

    companion object {
        /** IP of a Fire TV seen through DIAL (`SearchTVActivity.N`), used by the Fire TV remote. */
        var fireTvIp = ""

        /** `v()`: one row per friendly name, grouping the services that expose the same TV. */
        fun group(devices: Collection<ConnectableDevice>): List<SmartTv> {
            val rows = LinkedHashMap<String, SmartTv>()
            for (device in devices) {
                if (device.connectedServiceNames == null) continue
                if (device.serviceId.equals(DIALService.ID, ignoreCase = true)) {
                    if (device.friendlyName?.contains(FireTVService.ID) == true) fireTvIp = device.ipAddress
                    continue
                }
                val name = device.friendlyName ?: device.modelName
                val key = rows.keys.firstOrNull { it.equals(name, ignoreCase = true) }
                if (key != null) rows.getValue(key).devices += device else rows[name] = SmartTv(name, mutableListOf(device))
            }
            return rows.values.toList()
        }
    }
}

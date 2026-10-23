package com.casttotv.screen.mirroring.ui.activities.function.connect

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.LayoutItemDeviceBinding
import com.casttotv.screen.mirroring.tv.DeviceType
import com.casttotv.screen.mirroring.tv.TvConnect
import com.connectsdk.device.ConnectableDevice

/** One TV (by friendly name) and every ConnectSDK service found for it (`SmartTVObject`). */
class SmartTv(val name: String, val devices: MutableList<ConnectableDevice>) {
    val services: String get() = devices.joinToString(", ") { it.connectedServiceNames.orEmpty() }
}

val DeviceType.icon: Int
    get() = when (this) {
        DeviceType.ROKU -> R.drawable.ic_roku
        DeviceType.LG -> R.drawable.ic_lg
        DeviceType.SAMSUNG -> R.drawable.ic_samsung
        DeviceType.SONY -> R.drawable.ic_sony
        DeviceType.ANDROID_TV -> R.drawable.ic_android
        DeviceType.CHROMECAST -> R.drawable.ic_chromecast
        DeviceType.TCL -> R.drawable.ic_tcl
        DeviceType.FIRE_TV -> R.drawable.ic_firetv
        else -> R.drawable.ic_default
    }

/** `ud0` mode 7. */
class DeviceAdapter(private val onClick: (SmartTv) -> Unit) : RecyclerView.Adapter<DeviceAdapter.Holder>() {
    private val items = mutableListOf<SmartTv>()

    class Holder(val binding: LayoutItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<SmartTv>) {
        items.clear()
        items += list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val tv = items[position]
        with(holder.binding) {
            textDeviceName.text = tv.name
            textDeviceType.text = tv.services
            imgDevice.setImageResource(TvConnect.typeOf(tv.devices[0]).icon)
            root.setOnClickListener { onClick(tv) }
        }
    }
}

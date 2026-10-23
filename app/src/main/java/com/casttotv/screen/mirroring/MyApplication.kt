package com.casttotv.screen.mirroring

import android.app.Application
import android.content.Intent
import com.casttotv.screen.mirroring.data.AppPrefs
import com.casttotv.screen.mirroring.tv.SamsungRemote
import com.connectsdk.DeviceConnectService
import com.connectsdk.discovery.DiscoveryManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)
        com.casttotv.screen.mirroring.data.BrowserHistory.init(this)
        com.casttotv.screen.mirroring.data.Premium.init(this)
        SamsungRemote.init(this)
        DiscoveryManager.init(applicationContext)
        DiscoveryManager.getInstance().start()
        DeviceConnectService.enqueueWork(this, Intent())
    }
}

package com.connectsdk;

import java.util.HashMap;

/* JADX INFO: loaded from: classes3.dex */
public class DefaultPlatform {
    public static HashMap<String, String> getDeviceServiceMap() {
        HashMap<String, String> mapS = new java.util.HashMap<>(java.util.Map.of("com.connectsdk.service.CastService", "com.connectsdk.discovery.provider.SSDPDiscoveryProvider", "com.connectsdk.service.WebOSTVService", "com.connectsdk.discovery.provider.SSDPDiscoveryProvider"));
        mapS.put("com.connectsdk.service.DLNAService", "com.connectsdk.discovery.provider.SSDPDiscoveryProvider");
        mapS.put("com.connectsdk.service.RokuService", "com.connectsdk.discovery.provider.SSDPDiscoveryProvider");
        mapS.put("com.connectsdk.service.FireTVService", "com.connectsdk.discovery.provider.FireTVDiscoveryProvider");
        mapS.put("com.connectsdk.service.AndroidService", "com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider");
        mapS.put("com.connectsdk.service.NewAndroidService", "com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider");
        mapS.put("com.connectsdk.service.VizioService", "com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider");
        return mapS;
    }
}

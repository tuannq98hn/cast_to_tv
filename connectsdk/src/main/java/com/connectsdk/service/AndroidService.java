package com.connectsdk.service;

import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;

/* JADX INFO: loaded from: classes3.dex */
public class AndroidService extends DeviceService {
    public static final String ID = "AndroidTV";

    public AndroidService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter("AndroidTV", "_androidtvremote._tcp.local.");
    }

    @Override // com.connectsdk.service.DeviceService
    public void connect() {
        this.connected = true;
        reportConnected(true);
    }

    @Override // com.connectsdk.service.DeviceService
    public void disconnect() {
        this.connected = false;
    }

    @Override // com.connectsdk.service.DeviceService
    public boolean isConnectable() {
        return true;
    }

    @Override // com.connectsdk.service.DeviceService
    public boolean isConnected() {
        return this.connected;
    }
}

package com.connectsdk.discovery.provider;

import android.content.Context;
import com.amazon.whisperplay.fling.media.controller.DiscoveryController;
import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: loaded from: classes3.dex */
public class FireTVDiscoveryProvider implements DiscoveryProvider {
    private DiscoveryController discoveryController;
    DiscoveryController.IDiscoveryListener fireTVListener;
    ConcurrentHashMap<String, ServiceDescription> foundServices;
    private boolean isRunning;
    CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;

    public class FireTVDiscoveryListener implements DiscoveryController.IDiscoveryListener {
        public FireTVDiscoveryListener() {
        }

        private void updateServiceDescription(ServiceDescription serviceDescription, RemoteMediaPlayer remoteMediaPlayer) {
            String uniqueIdentifier = remoteMediaPlayer.getUniqueIdentifier();
            serviceDescription.setDevice(remoteMediaPlayer);
            serviceDescription.setFriendlyName(remoteMediaPlayer.getName());
            serviceDescription.setIpAddress(uniqueIdentifier);
            serviceDescription.setServiceID("FireTV");
            serviceDescription.setUUID(uniqueIdentifier);
        }

        @Override // com.amazon.whisperplay.fling.media.controller.DiscoveryController.IDiscoveryListener
        public void discoveryFailure() {
            FireTVDiscoveryProvider.this.notifyListenersThatDiscoveryFailed(new ServiceCommandError("FireTV discovery failure"));
        }

        @Override // com.amazon.whisperplay.fling.media.controller.DiscoveryController.IDiscoveryListener
        public void playerDiscovered(RemoteMediaPlayer remoteMediaPlayer) {
            if (remoteMediaPlayer == null) {
                return;
            }
            String uniqueIdentifier = remoteMediaPlayer.getUniqueIdentifier();
            ServiceDescription serviceDescription = FireTVDiscoveryProvider.this.foundServices.get(uniqueIdentifier);
            if (serviceDescription != null) {
                updateServiceDescription(serviceDescription, remoteMediaPlayer);
                return;
            }
            ServiceDescription serviceDescription2 = new ServiceDescription();
            updateServiceDescription(serviceDescription2, remoteMediaPlayer);
            FireTVDiscoveryProvider.this.foundServices.put(uniqueIdentifier, serviceDescription2);
            FireTVDiscoveryProvider.this.notifyListenersThatServiceAdded(serviceDescription2);
        }

        @Override // com.amazon.whisperplay.fling.media.controller.DiscoveryController.IDiscoveryListener
        public void playerLost(RemoteMediaPlayer remoteMediaPlayer) {
            ServiceDescription serviceDescription;
            if (remoteMediaPlayer == null || (serviceDescription = FireTVDiscoveryProvider.this.foundServices.get(remoteMediaPlayer.getUniqueIdentifier())) == null) {
                return;
            }
            FireTVDiscoveryProvider.this.notifyListenersThatServiceLost(serviceDescription);
            FireTVDiscoveryProvider.this.foundServices.remove(remoteMediaPlayer.getUniqueIdentifier());
        }
    }

    public FireTVDiscoveryProvider(Context context) {
        this(new DiscoveryController(context));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyListenersThatDiscoveryFailed(final ServiceCommandError serviceCommandError) {
        Util.runOnUI(new Runnable() { // from class: com.connectsdk.discovery.provider.FireTVDiscoveryProvider.3
            @Override // java.lang.Runnable
            public void run() {
                Iterator<DiscoveryProviderListener> it = FireTVDiscoveryProvider.this.serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceDiscoveryFailed(FireTVDiscoveryProvider.this, serviceCommandError);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyListenersThatServiceAdded(final ServiceDescription serviceDescription) {
        Util.runOnUI(new Runnable() { // from class: com.connectsdk.discovery.provider.FireTVDiscoveryProvider.1
            @Override // java.lang.Runnable
            public void run() {
                Iterator<DiscoveryProviderListener> it = FireTVDiscoveryProvider.this.serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceAdded(FireTVDiscoveryProvider.this, serviceDescription);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyListenersThatServiceLost(final ServiceDescription serviceDescription) {
        Util.runOnUI(new Runnable() { // from class: com.connectsdk.discovery.provider.FireTVDiscoveryProvider.2
            @Override // java.lang.Runnable
            public void run() {
                Iterator<DiscoveryProviderListener> it = FireTVDiscoveryProvider.this.serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceRemoved(FireTVDiscoveryProvider.this, serviceDescription);
                }
            }
        });
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void addDeviceFilter(DiscoveryFilter discoveryFilter) {
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void addListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.add(discoveryProviderListener);
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public boolean isEmpty() {
        return this.foundServices.isEmpty();
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void removeDeviceFilter(DiscoveryFilter discoveryFilter) {
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void removeListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.remove(discoveryProviderListener);
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void rescan() {
        restart();
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void reset() {
        this.foundServices.clear();
        stop();
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void restart() {
        stop();
        start();
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void setFilters(List<DiscoveryFilter> list) {
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void start() {
        if (this.isRunning) {
            return;
        }
        this.discoveryController.start(this.fireTVListener);
        this.isRunning = true;
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void stop() {
        if (this.isRunning) {
            this.discoveryController.stop();
            this.isRunning = false;
        }
        Iterator<ServiceDescription> it = this.foundServices.values().iterator();
        while (it.hasNext()) {
            notifyListenersThatServiceLost(it.next());
        }
        this.foundServices.clear();
    }

    public FireTVDiscoveryProvider(DiscoveryController discoveryController) {
        this.foundServices = new ConcurrentHashMap<>();
        this.serviceListeners = new CopyOnWriteArrayList<>();
        this.discoveryController = discoveryController;
        this.fireTVListener = new FireTVDiscoveryListener();
    }
}

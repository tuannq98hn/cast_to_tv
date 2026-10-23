package com.connectsdk.discovery.provider;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.content.Context;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.config.ServiceDescription;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ZeroconfDiscoveryProvider implements DiscoveryProvider {
    private static final String HOSTNAME = "connectsdk";
    JmDNS jmdns;
    private Timer scanTimer;
    InetAddress srcAddress;
    boolean isRunning = false;
    ServiceListener jmdnsListener = new ServiceListener() { // from class: com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider.1
        @Override
        public void serviceAdded(ServiceEvent event) {
            // Ask jmdns to resolve now; serviceResolved() fires once host/port are known.
            jmdns.requestServiceInfo(event.getType(), event.getName(), false, 6000);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            final ServiceDescription serviceDescription = ZeroconfDiscoveryProvider.this.foundServices.get(hostAddress(event.getInfo()));
            if (serviceDescription != null) {
                Util.runOnUI(new Runnable() { // from class: com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider.1.1
                    @Override // java.lang.Runnable
                    public void run() {
                        Iterator<DiscoveryProviderListener> it = ZeroconfDiscoveryProvider.this.serviceListeners.iterator();
                        while (it.hasNext()) {
                            it.next().onServiceRemoved(ZeroconfDiscoveryProvider.this, serviceDescription);
                        }
                    }
                });
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            ServiceInfo st5Var = event.getInfo();
            String strA = hostAddress(st5Var);
            if (Util.isIPv4Address(strA)) {
                String strF = st5Var.getName();
                int i = st5Var.getPort();
                ServiceDescription serviceDescription = ZeroconfDiscoveryProvider.this.foundServices.get(strA);
                boolean z = true;
                if (serviceDescription == null) {
                    serviceDescription = new ServiceDescription();
                    serviceDescription.setUUID(strA);
                    serviceDescription.setServiceFilter(st5Var.getType());
                    serviceDescription.setIpAddress(strA);
                    serviceDescription.setServiceID(ZeroconfDiscoveryProvider.this.serviceIdForFilter(st5Var.getType()));
                    serviceDescription.setPort(i);
                    serviceDescription.setFriendlyName(strF);
                } else if (serviceDescription.getFriendlyName().equals(strF)) {
                    z = false;
                } else {
                    serviceDescription.setFriendlyName(strF);
                }
                serviceDescription.setLastDetection(new Date().getTime());
                ZeroconfDiscoveryProvider.this.foundServices.put(strA, serviceDescription);
                if (z) {
                    Iterator<DiscoveryProviderListener> it = ZeroconfDiscoveryProvider.this.serviceListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onServiceAdded(ZeroconfDiscoveryProvider.this, serviceDescription);
                    }
                }
            }
        }
    };
    ConcurrentHashMap<String, ServiceDescription> foundServices = new ConcurrentHashMap<>(8, 0.75f, 2);
    CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners = new CopyOnWriteArrayList<>();
    List<DiscoveryFilter> serviceFilters = new CopyOnWriteArrayList();

    public class MDNSSearchTask extends TimerTask {
        public /* synthetic */ MDNSSearchTask(ZeroconfDiscoveryProvider zeroconfDiscoveryProvider, int i) {
            this();
        }

        @Override // java.util.TimerTask, java.lang.Runnable
        public void run() {
            ArrayList<String> arrayList = new ArrayList();
            long time = new Date().getTime() - 60000;
            for (String str : ZeroconfDiscoveryProvider.this.foundServices.keySet()) {
                ServiceDescription serviceDescription = ZeroconfDiscoveryProvider.this.foundServices.get(str);
                if (serviceDescription == null || serviceDescription.getLastDetection() < time) {
                    arrayList.add(str);
                }
            }
            for (String str2 : arrayList) {
                final ServiceDescription serviceDescription2 = ZeroconfDiscoveryProvider.this.foundServices.get(str2);
                if (serviceDescription2 != null) {
                    Util.runOnUI(new Runnable() { // from class: com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider.MDNSSearchTask.1
                        @Override // java.lang.Runnable
                        public void run() {
                            Iterator<DiscoveryProviderListener> it = ZeroconfDiscoveryProvider.this.serviceListeners.iterator();
                            while (it.hasNext()) {
                                it.next().onServiceRemoved(ZeroconfDiscoveryProvider.this, serviceDescription2);
                            }
                        }
                    });
                }
                if (ZeroconfDiscoveryProvider.this.foundServices.containsKey(str2)) {
                    ZeroconfDiscoveryProvider.this.foundServices.remove(str2);
                }
            }
            ZeroconfDiscoveryProvider.this.rescan();
        }

        private MDNSSearchTask() {
        }
    }

    public ZeroconfDiscoveryProvider(Context context) {
        try {
            this.srcAddress = Util.getIpAddress(context);
        } catch (UnknownHostException e2) {
            e2.printStackTrace();
        }
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void addDeviceFilter(DiscoveryFilter discoveryFilter) {
        if (discoveryFilter.getServiceFilter() == null) {
            String str = Util.T;
        } else {
            this.serviceFilters.add(discoveryFilter);
        }
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void addListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.add(discoveryProviderListener);
    }

    public JmDNS createJmDNS() throws IOException {
        InetAddress inetAddress = this.srcAddress;
        if (inetAddress != null) {
            return JmDNS.create(inetAddress, HOSTNAME);
        }
        return null;
    }

    private static String hostAddress(ServiceInfo info) {
        String[] addresses = info.getHostAddresses();
        return addresses.length > 0 ? addresses[0] : "";
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public boolean isEmpty() {
        return this.serviceFilters.size() == 0;
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void removeDeviceFilter(DiscoveryFilter discoveryFilter) {
        this.serviceFilters.remove(discoveryFilter);
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void removeListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.remove(discoveryProviderListener);
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void rescan() {
        try {
            if (this.jmdns != null) {
                this.jmdns.close();
                this.jmdns = null;
            }
            JmDNS created = createJmDNS();
            this.jmdns = created;
            if (created != null) {
                Iterator<DiscoveryFilter> it = this.serviceFilters.iterator();
                while (it.hasNext()) {
                    String serviceFilter = it.next().getServiceFilter();
                    this.jmdns.addServiceListener(serviceFilter, this.jmdnsListener);
                }
            }
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void reset() {
        stop();
        this.foundServices.clear();
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void restart() {
        stop();
        start();
    }

    public String serviceIdForFilter(String str) {
        for (DiscoveryFilter discoveryFilter : this.serviceFilters) {
            if (discoveryFilter.getServiceFilter().equals(str)) {
                return discoveryFilter.getServiceId();
            }
        }
        return "";
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void setFilters(List<DiscoveryFilter> list) {
        this.serviceFilters = list;
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void start() {
        if (this.isRunning) {
            return;
        }
        this.isRunning = true;
        Timer timer = new Timer();
        this.scanTimer = timer;
        timer.schedule(new MDNSSearchTask(this, 0), 100L, 10000L);
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void stop() {
        this.isRunning = false;
        Timer timer = this.scanTimer;
        if (timer != null) {
            timer.cancel();
            this.scanTimer = null;
        }
        if (this.jmdns != null) {
            Iterator<DiscoveryFilter> it = this.serviceFilters.iterator();
            while (it.hasNext()) {
                this.jmdns.removeServiceListener(it.next().getServiceFilter(), this.jmdnsListener);
            }
        }
    }
}

package com.connectsdk.discovery.provider;

import android.content.Context;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.discovery.provider.ssdp.SSDPClient;
import com.connectsdk.discovery.provider.ssdp.SSDPDevice;
import com.connectsdk.discovery.provider.ssdp.SSDPPacket;
import com.connectsdk.service.config.ServiceDescription;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/* JADX INFO: loaded from: classes3.dex */
public class SSDPDiscoveryProvider implements DiscoveryProvider {
    Context context;
    private Thread notifyThread;
    private Thread responseThread;
    private Timer scanTimer;
    private SSDPClient ssdpClient;
    boolean needToStartSearch = false;
    ConcurrentHashMap<String, ServiceDescription> foundServices = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, ServiceDescription> discoveredServices = new ConcurrentHashMap<>();
    boolean isRunning = false;
    private Runnable mResponseHandler = new Runnable() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.4
        @Override // java.lang.Runnable
        public void run() {
            while (SSDPDiscoveryProvider.this.ssdpClient != null) {
                try {
                    SSDPDiscoveryProvider.this.handleSSDPPacket(new SSDPPacket(SSDPDiscoveryProvider.this.ssdpClient.responseReceive()));
                } catch (IOException e2) {
                    e2.printStackTrace();
                    return;
                } catch (RuntimeException e3) {
                    e3.printStackTrace();
                    return;
                }
            }
        }
    };
    private Runnable mRespNotifyHandler = new Runnable() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.5
        @Override // java.lang.Runnable
        public void run() {
            while (SSDPDiscoveryProvider.this.ssdpClient != null) {
                try {
                    SSDPDiscoveryProvider.this.handleSSDPPacket(new SSDPPacket(SSDPDiscoveryProvider.this.ssdpClient.multicastReceive()));
                } catch (IOException e2) {
                    e2.printStackTrace();
                    return;
                } catch (RuntimeException e3) {
                    e3.printStackTrace();
                    return;
                }
            }
        }
    };
    private Pattern uuidReg = Pattern.compile("(?<=uuid:)(.+?)(?=(::)|$)");
    private CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners = new CopyOnWriteArrayList<>();
    List<DiscoveryFilter> serviceFilters = new CopyOnWriteArrayList();

    public SSDPDiscoveryProvider(Context context) {
        this.context = context;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSSDPPacket(final SSDPPacket sSDPPacket) {
        Util.runOnUI(new Runnable() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.6
            @Override // java.lang.Runnable
            public void run() {
                String str;
                try {
                    SSDPPacket sSDPPacket2 = sSDPPacket;
                    if (sSDPPacket2 != null && sSDPPacket2.getData().size() != 0 && sSDPPacket.getType() != null) {
                        String str2 = sSDPPacket.getData().get(sSDPPacket.getType().equals("NOTIFY * HTTP/1.1") ? "NT" : "ST");
                        if (str2 != null && !"M-SEARCH * HTTP/1.1".equals(sSDPPacket.getType()) && SSDPDiscoveryProvider.this.isSearchingForFilter(str2) && (str = sSDPPacket.getData().get("USN")) != null && str.length() != 0) {
                            Matcher matcher = SSDPDiscoveryProvider.this.uuidReg.matcher(str);
                            if (matcher.find()) {
                                String strGroup = matcher.group();
                                if ("ssdp:byebye".equals(sSDPPacket.getData().get("NTS"))) {
                                    ServiceDescription serviceDescription = SSDPDiscoveryProvider.this.foundServices.get(strGroup);
                                    if (serviceDescription != null) {
                                        SSDPDiscoveryProvider.this.foundServices.remove(strGroup);
                                        SSDPDiscoveryProvider.this.notifyListenersOfLostService(serviceDescription);
                                        return;
                                    }
                                    return;
                                }
                                String str3 = sSDPPacket.getData().get("LOCATION");
                                if (str3 != null && str3.length() != 0) {
                                    ServiceDescription serviceDescription2 = SSDPDiscoveryProvider.this.foundServices.get(strGroup);
                                    ServiceDescription serviceDescription3 = SSDPDiscoveryProvider.this.discoveredServices.get(strGroup);
                                    if (serviceDescription2 == null && serviceDescription3 == null) {
                                        serviceDescription2 = new ServiceDescription();
                                        serviceDescription2.setUUID(strGroup);
                                        serviceDescription2.setServiceFilter(str2);
                                        serviceDescription2.setIpAddress(sSDPPacket.getDatagramPacket().getAddress().getHostAddress());
                                        serviceDescription2.setPort(3001);
                                        SSDPDiscoveryProvider.this.discoveredServices.put(strGroup, serviceDescription2);
                                        SSDPDiscoveryProvider.this.getLocationData(str3, strGroup, str2);
                                    }
                                    if (serviceDescription2 != null) {
                                        serviceDescription2.setLastDetection(new Date().getTime());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyListenersOfLostService(ServiceDescription serviceDescription) {
        for (String str : serviceIdsForFilter(serviceDescription.getServiceFilter())) {
            final ServiceDescription serviceDescriptionM76clone = serviceDescription.m76clone();
            serviceDescriptionM76clone.setServiceID(str);
            Util.runOnUI(new Runnable() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.9
                @Override // java.lang.Runnable
                public void run() {
                    Iterator it = SSDPDiscoveryProvider.this.serviceListeners.iterator();
                    while (it.hasNext()) {
                        ((DiscoveryProviderListener) it.next()).onServiceRemoved(SSDPDiscoveryProvider.this, serviceDescriptionM76clone);
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyListenersOfNewService(ServiceDescription serviceDescription) {
        for (String str : serviceIdsForFilter(serviceDescription.getServiceFilter())) {
            final ServiceDescription serviceDescriptionM76clone = serviceDescription.m76clone();
            serviceDescriptionM76clone.setServiceID(str);
            Util.runOnUI(new Runnable() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.8
                @Override // java.lang.Runnable
                public void run() {
                    Iterator it = SSDPDiscoveryProvider.this.serviceListeners.iterator();
                    while (it.hasNext()) {
                        ((DiscoveryProviderListener) it.next()).onServiceAdded(SSDPDiscoveryProvider.this, serviceDescriptionM76clone);
                    }
                }
            });
        }
    }

    private void openSocket() {
        SSDPClient sSDPClient = this.ssdpClient;
        if (sSDPClient == null || !sSDPClient.isConnected()) {
            try {
                InetAddress ipAddress = Util.getIpAddress(this.context);
                if (ipAddress == null) {
                    return;
                }
                this.ssdpClient = createSocket(ipAddress);
            } catch (UnknownHostException e2) {
                e2.printStackTrace();
            } catch (IOException e3) {
                e3.printStackTrace();
            }
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

    public boolean containsServicesWithFilter(SSDPDevice sSDPDevice, String str) {
        return true;
    }

    public SSDPClient createSocket(InetAddress inetAddress) throws IOException {
        return new SSDPClient(inetAddress);
    }

    public void getLocationData(String str, String str2, String str3) {
        try {
            getLocationData(new URL(str), str2, str3);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public boolean isEmpty() {
        return this.serviceFilters.size() == 0;
    }

    public boolean isSearchingForFilter(String str) {
        Iterator<DiscoveryFilter> it = this.serviceFilters.iterator();
        while (it.hasNext()) {
            if (it.next().getServiceFilter().equals(str)) {
                return true;
            }
        }
        return false;
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
        Util.runInBackground(new Runnable() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.3
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Iterator<DiscoveryFilter> it = SSDPDiscoveryProvider.this.serviceFilters.iterator();
                    while (it.hasNext()) {
                        final String sSDPSearchMessage = SSDPClient.getSSDPSearchMessage(it.next().getServiceFilter());
                        Timer timer = new Timer();
                        for (int i = 0; i < 3; i++) {
                            timer.schedule(new TimerTask() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.3.1
                                @Override // java.util.TimerTask, java.lang.Runnable
                                public void run() {
                                    try {
                                        if (SSDPDiscoveryProvider.this.ssdpClient == null || sSDPSearchMessage == null) {
                                            return;
                                        }
                                        SSDPDiscoveryProvider.this.ssdpClient.send(sSDPSearchMessage);
                                    } catch (IOException e2) {
                                        e2.printStackTrace();
                                    }
                                }
                            }, i * 1000);
                        }
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        });
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void reset() {
        stop();
        this.foundServices.clear();
        this.discoveredServices.clear();
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void restart() {
        stop();
        start();
    }

    public void sendSearch() {
        Util.runInBackground(new Runnable() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ArrayList<String> arrayList = new ArrayList();
                    long time = new Date().getTime() - 60000;
                    for (String str : SSDPDiscoveryProvider.this.foundServices.keySet()) {
                        ServiceDescription serviceDescription = SSDPDiscoveryProvider.this.foundServices.get(str);
                        if (serviceDescription == null || serviceDescription.getLastDetection() < time) {
                            arrayList.add(str);
                        }
                    }
                    for (String str2 : arrayList) {
                        ServiceDescription serviceDescription2 = SSDPDiscoveryProvider.this.foundServices.get(str2);
                        if (serviceDescription2 != null) {
                            SSDPDiscoveryProvider.this.notifyListenersOfLostService(serviceDescription2);
                        }
                        if (SSDPDiscoveryProvider.this.foundServices.containsKey(str2)) {
                            SSDPDiscoveryProvider.this.foundServices.remove(str2);
                        }
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                SSDPDiscoveryProvider.this.rescan();
            }
        }, true);
    }

    public List<String> serviceIdsForFilter(String str) {
        String serviceId;
        ArrayList arrayList = new ArrayList();
        for (DiscoveryFilter discoveryFilter : this.serviceFilters) {
            if (discoveryFilter.getServiceFilter().equals(str) && (serviceId = discoveryFilter.getServiceId()) != null) {
                arrayList.add(serviceId);
            }
        }
        return arrayList;
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
        openSocket();
        Timer timer = new Timer();
        this.scanTimer = timer;
        timer.schedule(new TimerTask() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.1
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                SSDPDiscoveryProvider.this.sendSearch();
            }
        }, 100L, 10000L);
        this.responseThread = new Thread(this.mResponseHandler);
        this.notifyThread = new Thread(this.mRespNotifyHandler);
        this.responseThread.start();
        this.notifyThread.start();
    }

    @Override // com.connectsdk.discovery.DiscoveryProvider
    public void stop() {
        this.isRunning = false;
        Timer timer = this.scanTimer;
        if (timer != null) {
            timer.cancel();
            this.scanTimer = null;
        }
        Thread thread = this.responseThread;
        if (thread != null) {
            thread.interrupt();
            this.responseThread = null;
        }
        Thread thread2 = this.notifyThread;
        if (thread2 != null) {
            thread2.interrupt();
            this.notifyThread = null;
        }
        SSDPClient sSDPClient = this.ssdpClient;
        if (sSDPClient != null) {
            sSDPClient.close();
            this.ssdpClient = null;
        }
    }

    public void getLocationData(final URL url, final String str, final String str2) {
        Util.runInBackground(new Runnable() { // from class: com.connectsdk.discovery.provider.SSDPDiscoveryProvider.7
            @Override // java.lang.Runnable
            public void run() {
                SSDPDevice sSDPDevice;
                ServiceDescription serviceDescription;
                try {
                    sSDPDevice = new SSDPDevice(url, str2);
                } catch (IOException e2) {
                    e2.printStackTrace();
                    sSDPDevice = null;
                } catch (ParserConfigurationException e3) {
                    e3.printStackTrace();
                    sSDPDevice = null;
                } catch (SAXException e4) {
                    e4.printStackTrace();
                    sSDPDevice = null;
                }
                if (sSDPDevice != null) {
                    sSDPDevice.UUID = str;
                    if (SSDPDiscoveryProvider.this.containsServicesWithFilter(sSDPDevice, str2) && (serviceDescription = SSDPDiscoveryProvider.this.discoveredServices.get(str)) != null) {
                        serviceDescription.setServiceFilter(str2);
                        serviceDescription.setFriendlyName(sSDPDevice.friendlyName);
                        serviceDescription.setModelName(sSDPDevice.modelName);
                        serviceDescription.setModelNumber(sSDPDevice.modelNumber);
                        serviceDescription.setModelDescription(sSDPDevice.modelDescription);
                        serviceDescription.setManufacturer(sSDPDevice.manufacturer);
                        serviceDescription.setApplicationURL(sSDPDevice.applicationURL);
                        serviceDescription.setServiceList(sSDPDevice.serviceList);
                        serviceDescription.setResponseHeaders(sSDPDevice.headers);
                        serviceDescription.setLocationXML(sSDPDevice.locationXML);
                        serviceDescription.setServiceURI(sSDPDevice.serviceURI);
                        serviceDescription.setPort(sSDPDevice.port);
                        SSDPDiscoveryProvider.this.foundServices.put(str, serviceDescription);
                        SSDPDiscoveryProvider.this.notifyListenersOfNewService(serviceDescription);
                    }
                }
                SSDPDiscoveryProvider.this.discoveredServices.remove(str);
            }
        }, true);
    }
}

package com.connectsdk.device;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class DefaultConnectableDeviceStore implements ConnectableDeviceStore {
    static final String CLIENT_KEY = "clientKey";
    static final String CONFIG = "config";
    static final int CURRENT_VERSION = 0;
    static final String DEFAULT_SERVICE_NETCASTTV = "NetcastTVService";
    static final String DEFAULT_SERVICE_WEBOSTV = "WebOSTVService";
    static final String DESCRIPTION = "description";
    static final String DIRPATH = "/android/data/connect_sdk/";
    static final String FILENAME = "StoredDevices";
    static final String FILTER = "filter";
    static final String FRIENDLY_NAME = "friendlyName";
    static final String IP_ADDRESS = "ipAddress";
    public static final String KEY_CREATED = "created";
    public static final String KEY_DEVICES = "devices";
    public static final String KEY_UPDATED = "updated";
    public static final String KEY_VERSION = "version";
    static final String MODEL_NAME = "modelName";
    static final String MODEL_NUMBER = "modelNumber";
    static final String PAIRING_KEY = "pairingKey";
    static final String PORT = "port";
    static final String SERVER_CERTIFICATE = "serverCertificate";
    static final String SERVICES = "services";
    static final String SERVICE_UUID = "serviceUUID";
    static final String UUID = "uuid";
    public long created;
    private String fileFullPath;
    public long updated;
    public int version;
    public long maxStoreDuration = TimeUnit.DAYS.toSeconds(3);
    private Map<String, JSONObject> storedDevices = new ConcurrentHashMap();
    private Map<String, ConnectableDevice> activeDevices = new ConcurrentHashMap();
    private boolean waitToWrite = false;

    public DefaultConnectableDeviceStore(Context context) {
        this.fileFullPath = ((Environment.getExternalStorageState().equals("mounted") ? Environment.getExternalStorageDirectory().getAbsolutePath() : "unmounted") + "/android/data/connect_sdk/StoredDevices");
        try {
            this.fileFullPath = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).applicationInfo.dataDir + "/StoredDevices";
        } catch (PackageManager.NameNotFoundException e2) {
            e2.printStackTrace();
        }
        load();
    }

    private ConnectableDevice getActiveDevice(String str) {
        ConnectableDevice connectableDevice = this.activeDevices.get(str);
        if (connectableDevice == null) {
            for (ConnectableDevice connectableDevice2 : this.activeDevices.values()) {
                Iterator<DeviceService> it = connectableDevice2.getServices().iterator();
                while (it.hasNext()) {
                    if (str.equals(it.next().getServiceDescription().getUUID())) {
                        return connectableDevice2;
                    }
                }
            }
        }
        return connectableDevice;
    }

    private JSONObject getStoredDevice(String str) {
        JSONObject jSONObject = this.storedDevices.get(str);
        if (jSONObject == null) {
            for (JSONObject jSONObject2 : this.storedDevices.values()) {
                JSONObject jSONObjectOptJSONObject = jSONObject2.optJSONObject("services");
                if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.has(str)) {
                    return jSONObject2;
                }
            }
        }
        return jSONObject;
    }

    /* JADX WARN: Code duplicated, block: B:25:0x008e  */
    /* JADX WARN: Code duplicated, block: B:32:? A[RETURN, SYNTHETIC] */
    private void load() {
        File file = new File(this.fileFullPath);
        if (!file.exists()) {
            this.version = 0;
            this.created = Util.getTime();
            this.updated = Util.getTime();
            return;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                } else {
                    sb.append(line);
                }
            }
            bufferedReader.close();
            JSONObject jSONObject = new JSONObject(sb.toString());
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("devices");
            if (jSONArrayOptJSONArray != null) {
                for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                    JSONObject jSONObject2 = jSONArrayOptJSONArray.getJSONObject(i);
                    this.storedDevices.put(jSONObject2.getString("id"), jSONObject2);
                }
            }
            this.version = jSONObject.optInt("version", 0);
            this.created = jSONObject.optLong("created", 0L);
            this.updated = jSONObject.optLong("updated", 0L);
        } catch (IOException e2) {
            e2.printStackTrace();
            if (this.storedDevices == null) {
                file.delete();
                this.version = 0;
                this.created = Util.getTime();
                this.updated = Util.getTime();
            }
        } catch (JSONException e3) {
            e3.printStackTrace();
            if (this.storedDevices == null) {
                file.delete();
                this.version = 0;
                this.created = Util.getTime();
                this.updated = Util.getTime();
            }
        }
    }

    private void store() {
        this.updated = Util.getTime();
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("version", this.version);
            jSONObject.put("created", this.created);
            jSONObject.put("updated", this.updated);
            jSONObject.put("devices", new JSONArray((Collection) this.storedDevices.values()));
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        if (this.waitToWrite) {
            return;
        }
        writeStoreToDisk(jSONObject);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void writeStoreToDisk(final JSONObject jSONObject) {
        final long j = this.updated;
        this.waitToWrite = true;
        Util.runInBackground(new Runnable() { // from class: com.connectsdk.device.DefaultConnectableDeviceStore.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    try {
                        File file = new File(DefaultConnectableDeviceStore.this.fileFullPath);
                        if (!file.exists()) {
                            file.getParentFile().mkdirs();
                        }
                        FileWriter fileWriter = new FileWriter(file);
                        fileWriter.write(jSONObject.toString());
                        fileWriter.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    DefaultConnectableDeviceStore.this.waitToWrite = false;
                    long j2 = j;
                    DefaultConnectableDeviceStore defaultConnectableDeviceStore = DefaultConnectableDeviceStore.this;
                    if (j2 < defaultConnectableDeviceStore.updated) {
                        defaultConnectableDeviceStore.writeStoreToDisk(jSONObject);
                    }
                } catch (Throwable th) {
                    DefaultConnectableDeviceStore.this.waitToWrite = false;
                    throw th;
                }
            }
        });
    }

    @Override // com.connectsdk.device.ConnectableDeviceStore
    public void addDevice(ConnectableDevice connectableDevice) {
        if (connectableDevice == null || connectableDevice.getServices().size() == 0) {
            return;
        }
        if (!this.activeDevices.containsKey(connectableDevice.getId())) {
            this.activeDevices.put(connectableDevice.getId(), connectableDevice);
        }
        if (getStoredDevice(connectableDevice.getId()) != null) {
            updateDevice(connectableDevice);
        } else {
            this.storedDevices.put(connectableDevice.getId(), connectableDevice.toJSONObject());
            store();
        }
    }

    @Override // com.connectsdk.device.ConnectableDeviceStore
    public ConnectableDevice getDevice(String str) {
        JSONObject storedDevice;
        if (str == null || str.length() == 0) {
            return null;
        }
        ConnectableDevice activeDevice = getActiveDevice(str);
        return (activeDevice != null || (storedDevice = getStoredDevice(str)) == null) ? activeDevice : new ConnectableDevice(storedDevice);
    }

    @Override // com.connectsdk.device.ConnectableDeviceStore
    public ServiceConfig getServiceConfig(ServiceDescription serviceDescription) {
        String uuid;
        JSONObject storedDevice;
        JSONObject jSONObjectOptJSONObject;
        JSONObject jSONObjectOptJSONObject2;
        JSONObject jSONObjectOptJSONObject3;
        if (serviceDescription == null || (uuid = serviceDescription.getUUID()) == null || uuid.length() == 0 || (storedDevice = getStoredDevice(uuid)) == null || (jSONObjectOptJSONObject = storedDevice.optJSONObject("services")) == null || (jSONObjectOptJSONObject2 = jSONObjectOptJSONObject.optJSONObject(uuid)) == null || (jSONObjectOptJSONObject3 = jSONObjectOptJSONObject2.optJSONObject("config")) == null) {
            return null;
        }
        return ServiceConfig.getConfig(jSONObjectOptJSONObject3);
    }

    @Override // com.connectsdk.device.ConnectableDeviceStore
    public JSONObject getStoredDevices() {
        JSONObject jSONObject = new JSONObject();
        for (Map.Entry<String, JSONObject> entry : this.storedDevices.entrySet()) {
            try {
                jSONObject.put(entry.getKey(), entry.getValue());
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        }
        return jSONObject;
    }

    @Override // com.connectsdk.device.ConnectableDeviceStore
    public void removeAll() {
        this.activeDevices.clear();
        this.storedDevices.clear();
        store();
    }

    @Override // com.connectsdk.device.ConnectableDeviceStore
    public void removeDevice(ConnectableDevice connectableDevice) {
        if (connectableDevice == null) {
            return;
        }
        this.activeDevices.remove(connectableDevice.getId());
        this.storedDevices.remove(connectableDevice.getId());
        store();
    }

    @Override // com.connectsdk.device.ConnectableDeviceStore
    public void updateDevice(ConnectableDevice connectableDevice) {
        JSONObject storedDevice;
        if (connectableDevice == null || connectableDevice.getServices().size() == 0 || (storedDevice = getStoredDevice(connectableDevice.getId())) == null) {
            return;
        }
        try {
            storedDevice.put("lastKnownIPAddress", connectableDevice.getLastKnownIPAddress());
            storedDevice.put("lastSeenOnWifi", connectableDevice.getLastSeenOnWifi());
            storedDevice.put("lastConnected", connectableDevice.getLastConnected());
            storedDevice.put("lastDetection", connectableDevice.getLastDetection());
            JSONObject jSONObjectOptJSONObject = storedDevice.optJSONObject("services");
            if (jSONObjectOptJSONObject == null) {
                jSONObjectOptJSONObject = new JSONObject();
            }
            for (DeviceService deviceService : connectableDevice.getServices()) {
                JSONObject jSONObject = deviceService.toJSONObject();
                if (jSONObject != null) {
                    jSONObjectOptJSONObject.put(deviceService.getServiceDescription().getUUID(), jSONObject);
                }
            }
            storedDevice.put("services", jSONObjectOptJSONObject);
            this.storedDevices.put(connectableDevice.getId(), storedDevice);
            this.activeDevices.put(connectableDevice.getId(), connectableDevice);
            store();
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
    }
}

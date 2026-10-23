package com.connectsdk.service;

import android.annotation.SuppressLint;
import android.graphics.PointF;
import androidx.annotation.NonNull;
import com.connectsdk.core.AppInfo;
import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.ExternalInputInfo;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.ExternalInputControl;
import com.connectsdk.service.capability.KeyControl;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MouseControl;
import com.connectsdk.service.capability.PlaylistControl;
import com.connectsdk.service.capability.PowerControl;
import com.connectsdk.service.capability.TVControl;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.ToastControl;
import com.connectsdk.service.capability.VizioKeyCode;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.NotSupportedServiceSubscription;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.config.WebOSTVServiceConfig;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession;
import com.connectsdk.service.sessions.WebOSWebAppSession;
import com.connectsdk.service.webos.WebOSTVDeviceService;
import com.connectsdk.service.webos.WebOSTVKeyboardInput;
import com.connectsdk.service.webos.WebOSTVMouseSocketConnection;
import com.connectsdk.service.webos.WebOSTVServiceSocketClient;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;

/* JADX INFO: loaded from: classes3.dex */
@SuppressLint({"DefaultLocale"})
public class WebOSTVService extends WebOSTVDeviceService implements Launcher, MediaPlayer, PlaylistControl, VolumeControl, TVControl, ToastControl, ExternalInputControl, MouseControl, KeyControl, TextInputControl, WebAppLauncher {
    static String APP_STATE = "ssap://system.launcher/getAppState";
    static String APP_STATUS = "ssap://com.webos.service.appstatus/getAppStatus";
    static String CHANNEL = "ssap://tv/getCurrentChannel";
    static String CHANNEL_LIST = "ssap://tv/getChannelList";
    static final String CLOSE_APP_URI = "ssap://system.launcher/close";
    static final String CLOSE_MEDIA_URI = "ssap://media.viewer/close";
    static final String CLOSE_WEBAPP_URI = "ssap://webapp/closeWebApp";
    static String FOREGROUND_APP = "ssap://com.webos.applicationManager/getForegroundAppInfo";
    public static final String ID = "webOS TV";
    private static final String MEDIA_PLAYER_ID = "MediaPlayer";
    static String MUTE = "ssap://audio/getMute";
    static String PROGRAM = "ssap://tv/getChannelProgramInfo";
    static String VOLUME = "ssap://audio/getVolume";
    static String VOLUME_STATUS = "ssap://audio/getStatus";
    WebOSTVKeyboardInput keyboardInput;
    ConcurrentHashMap<String, String> mAppToAppIdMappings;
    private WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener mSocketListener;
    ConcurrentHashMap<String, WebOSWebAppSession> mWebAppSessions;
    WebOSTVMouseSocketConnection mouseSocket;
    List<String> permissions;
    WebOSTVServiceSocketClient socket;

    /* JADX INFO: renamed from: com.connectsdk.service.WebOSTVService$40, reason: invalid class name */
    public static /* synthetic */ class AnonymousClass40 {
        static final /* synthetic */ int[] $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode;

        static {
            int[] iArr = new int[KeyControl.KeyCode.values().length];
            $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode = iArr;
            try {
                iArr[KeyControl.KeyCode.NUM_0.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.NUM_1.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.NUM_2.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.NUM_3.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.NUM_4.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.NUM_5.ordinal()] = 6;
            } catch (NoSuchFieldError unused6) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.NUM_6.ordinal()] = 7;
            } catch (NoSuchFieldError unused7) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.NUM_7.ordinal()] = 8;
            } catch (NoSuchFieldError unused8) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.NUM_8.ordinal()] = 9;
            } catch (NoSuchFieldError unused9) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.NUM_9.ordinal()] = 10;
            } catch (NoSuchFieldError unused10) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.DASH.ordinal()] = 11;
            } catch (NoSuchFieldError unused11) {
            }
            try {
                $SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[KeyControl.KeyCode.ENTER.ordinal()] = 12;
            } catch (NoSuchFieldError unused12) {
            }
        }
    }

    public interface ACRAuthTokenListener extends ResponseListener<String> {
    }

    public interface LaunchPointsListener extends ResponseListener<JSONArray> {
    }

    public interface SecureAccessTestListener extends ResponseListener<Boolean> {
    }

    public interface ServiceInfoListener extends ResponseListener<JSONArray> {
    }

    public interface SystemInfoListener extends ResponseListener<JSONObject> {
    }

    public WebOSTVService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        this.mSocketListener = new WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener() { // from class: com.connectsdk.service.WebOSTVService.2
            @Override // com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener
            public void onBeforeRegister(final DeviceService.PairingType pairingType) {
                if (DiscoveryManager.getInstance().getPairingLevel().compareTo(DiscoveryManager.PairingLevel.PROTECTED) >= 0) {
                    Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.WebOSTVService.2.4
                        @Override // java.lang.Runnable
                        public void run() {
                            WebOSTVService webOSTVService = WebOSTVService.this;
                            DeviceService.DeviceServiceListener deviceServiceListener = webOSTVService.listener;
                            if (deviceServiceListener != null) {
                                deviceServiceListener.onPairingRequired(webOSTVService, pairingType, null);
                            }
                        }
                    });
                }
            }

            @Override // com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener
            public void onCloseWithError(final ServiceCommandError serviceCommandError) {
                Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.WebOSTVService.2.3
                    @Override // java.lang.Runnable
                    public void run() {
                        WebOSTVService webOSTVService = WebOSTVService.this;
                        DeviceService.DeviceServiceListener deviceServiceListener = webOSTVService.listener;
                        if (deviceServiceListener != null) {
                            deviceServiceListener.onDisconnect(webOSTVService, serviceCommandError);
                        }
                        try {
                            WebOSTVService.this.socket.setListener(null);
                            WebOSTVService.this.socket.disconnect();
                            WebOSTVService.this.socket = null;
                        } catch (Exception unused) {
                        }
                    }
                });
            }

            @Override // com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener
            public void onConnect() {
                WebOSTVService.this.reportConnected(true);
            }

            @Override // com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener
            public void onFailWithError(final ServiceCommandError serviceCommandError) {
                try {
                    WebOSTVService.this.socket.setListener(null);
                    WebOSTVService.this.socket.disconnect();
                    WebOSTVService.this.socket = null;
                    Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.WebOSTVService.2.2
                        @Override // java.lang.Runnable
                        public void run() {
                            WebOSTVService webOSTVService = WebOSTVService.this;
                            DeviceService.DeviceServiceListener deviceServiceListener = webOSTVService.listener;
                            if (deviceServiceListener != null) {
                                deviceServiceListener.onPairingFailed(webOSTVService, serviceCommandError);
                            }
                        }
                    });
                } catch (Exception e2) {
                    throw new RuntimeException(e2);
                }
            }

            @Override // com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener
            public Boolean onReceiveMessage(JSONObject jSONObject) {
                return Boolean.TRUE;
            }

            @Override // com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener
            public void onRegistrationFailed(final ServiceCommandError serviceCommandError) {
                Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.WebOSTVService.2.1
                    @Override // java.lang.Runnable
                    public void run() {
                        WebOSTVService webOSTVService = WebOSTVService.this;
                        DeviceService.DeviceServiceListener deviceServiceListener = webOSTVService.listener;
                        if (deviceServiceListener != null) {
                            deviceServiceListener.onPairingFailed(webOSTVService, serviceCommandError);
                        }
                        WebOSTVService.this.disconnect();
                    }
                });
            }

            @Override // com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener
            public void updateClientKey(String str) {
                try {
                    WebOSTVService.this.setClientKey(str);
                } catch (Exception e2) {
                    e2.getMessage();
                }
            }

            @Override // com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener
            public void updateIPAddress(String str) {
                WebOSTVService.this.serviceDescription.setIpAddress(str);
            }

            @Override // com.connectsdk.service.webos.WebOSTVServiceSocketClient.WebOSTVServiceSocketClientListener
            public void updateUUID(String str) {
                WebOSTVService.this.serviceDescription.setUUID(str);
            }
        };
        this.serviceConfig = new WebOSTVServiceConfig(serviceConfig.toJSONObject());
        this.pairingType = DeviceService.PairingType.FIRST_SCREEN;
        this.mAppToAppIdMappings = new ConcurrentHashMap<>();
        this.mWebAppSessions = new ConcurrentHashMap<>();
    }

    @NonNull
    private JSONObject createPlayMediaJsonRequestForSsap(MediaInfo mediaInfo, boolean z, String str) throws JSONException {
        return new JSONObject() {

            {
                put("target", mediaInfo.getUrl());
                put("title", WebOSTVService.this.getJsonValue(mediaInfo.getTitle()));
                put("description", WebOSTVService.this.getJsonValue(mediaInfo.getDescription()));
                put("mimeType", WebOSTVService.this.getJsonValue(mediaInfo.getMimeType()));
                put("iconSrc", WebOSTVService.this.getJsonValue(str));
                put("loop", z);
            }
        };
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter("webOS TV", "urn:lge-com:service:webos-second-screen:1");
    }

    private void displayMedia(JSONObject jSONObject, final MediaPlayer.LaunchListener launchListener) {
        new ServiceCommand(this, "ssap://media.viewer/open", jSONObject, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.9
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(launchListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                JSONObject jSONObject2 = (JSONObject) obj;
                LaunchSession launchSessionLaunchSessionForAppId = LaunchSession.launchSessionForAppId(jSONObject2.optString("id"));
                launchSessionLaunchSessionForAppId.setService(WebOSTVService.this);
                launchSessionLaunchSessionForAppId.setSessionId(jSONObject2.optString("sessionId"));
                launchSessionLaunchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.Media);
                Util.postSuccess(launchListener, new MediaPlayer.MediaLaunchObject(launchSessionLaunchSessionForAppId, WebOSTVService.this));
            }
        }).send();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<ExternalInputInfo> externalnputInfoFromJSONArray(JSONArray jSONArray) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < jSONArray.length(); i++) {
            try {
                JSONObject jSONObject = (JSONObject) jSONArray.get(i);
                String string = jSONObject.getString("id");
                String string2 = jSONObject.getString("label");
                boolean z = jSONObject.getBoolean("connected");
                String string3 = jSONObject.getString("icon");
                ExternalInputInfo externalInputInfo = new ExternalInputInfo();
                externalInputInfo.setRawData(jSONObject);
                externalInputInfo.setId(string);
                externalInputInfo.setName(string2);
                externalInputInfo.setConnected(z);
                externalInputInfo.setIconURL(string3);
                arrayList.add(externalInputInfo);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        }
        return arrayList;
    }

    private ServiceCommand<TVControl.State3DModeListener> get3DEnabled(boolean z, final TVControl.State3DModeListener state3DModeListener) {
        ResponseListener<Object> responseListener = new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.16
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(state3DModeListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                try {
                    Util.postSuccess(state3DModeListener, Boolean.valueOf(((JSONObject) obj).getJSONObject("status3D").getBoolean("status")));
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        };
        ServiceCommand<TVControl.State3DModeListener> uRLServiceSubscription = z ? new URLServiceSubscription<>(this, "ssap://com.webos.service.tv.display/get3DStatus", null, true, responseListener) : new ServiceCommand<>(this, "ssap://com.webos.service.tv.display/get3DStatus", null, true, responseListener);
        uRLServiceSubscription.send();
        return uRLServiceSubscription;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Object getJsonValue(Object obj) {
        return obj == null ? JSONObject.NULL : obj;
    }

    private ServiceCommand<WebAppSession.WebAppPinStatusListener> isWebAppPinned(boolean z, String str, final WebAppSession.WebAppPinStatusListener webAppPinStatusListener) {
        if (str == null || str.length() == 0) {
            if (webAppPinStatusListener != null) {
                webAppPinStatusListener.onError(new ServiceCommandError(-1, "You must provide a valid web app id", null));
            }
            return null;
        }
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("webAppId", str);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        ResponseListener<Object> responseListener = new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.32
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(webAppPinStatusListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                boolean zOptBoolean = ((JSONObject) obj).optBoolean("pinned");
                WebAppSession.WebAppPinStatusListener webAppPinStatusListener2 = webAppPinStatusListener;
                if (webAppPinStatusListener2 != null) {
                    webAppPinStatusListener2.onSuccess(Boolean.valueOf(zOptBoolean));
                }
            }
        };
        ServiceCommand<WebAppSession.WebAppPinStatusListener> uRLServiceSubscription = z ? new URLServiceSubscription<>(this, "ssap://webapp/isWebAppPinned", jSONObject, true, responseListener) : new ServiceCommand<>(this, "ssap://webapp/isWebAppPinned", jSONObject, true, responseListener);
        uRLServiceSubscription.send();
        return uRLServiceSubscription;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyPairingRequired() {
        DeviceService.DeviceServiceListener deviceServiceListener = this.listener;
        if (deviceServiceListener != null) {
            deviceServiceListener.onPairingRequired(this, this.pairingType, null);
        }
    }

    private void playMediaByNativeApp(MediaInfo mediaInfo, boolean z, MediaPlayer.LaunchListener launchListener) {
        ImageInfo imageInfo;
        MediaPlayer mediaPlayer;
        DeviceService dLNAService = getDLNAService();
        if (dLNAService != null && (mediaPlayer = (MediaPlayer) dLNAService.getAPI(MediaPlayer.class)) != null) {
            mediaPlayer.playMedia(mediaInfo, z, launchListener);
            return;
        }
        List<ImageInfo> images = mediaInfo.getImages();
        try {
            displayMedia(createPlayMediaJsonRequestForSsap(mediaInfo, z, (images == null || images.isEmpty() || (imageInfo = images.get(0)) == null) ? null : imageInfo.getUrl()), launchListener);
        } catch (JSONException e2) {
            Util.postError(launchListener, new ServiceCommandError(-1, e2.getLocalizedMessage(), e2));
            String str = Util.T;
        }
    }

    private void playMediaByWebApp(final MediaInfo mediaInfo, final boolean z, final MediaPlayer.LaunchListener launchListener) {
        final WebAppSession.LaunchListener launchListener2 = new WebAppSession.LaunchListener() { // from class: com.connectsdk.service.WebOSTVService.13
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                launchListener.onError(serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(WebAppSession webAppSession) {
                webAppSession.playMedia(mediaInfo, z, launchListener);
            }
        };
        getWebAppLauncher().joinWebApp("MediaPlayer", new WebAppSession.LaunchListener() { // from class: com.connectsdk.service.WebOSTVService.14
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                WebOSTVService.this.getWebAppLauncher().launchWebApp("MediaPlayer", launchListener2);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(WebAppSession webAppSession) {
                webAppSession.playMedia(mediaInfo, z, launchListener);
            }
        });
    }

    private void sendMessage(Object obj, LaunchSession launchSession, ResponseListener<Object> responseListener) {
        if (launchSession == null || launchSession.getAppId() == null) {
            Util.postError(responseListener, new ServiceCommandError(0, "Must provide a valid LaunchSession object", null));
            return;
        }
        if (obj == null) {
            Util.postError(responseListener, new ServiceCommandError(0, "Cannot send a null message", null));
            return;
        }
        if (this.socket == null) {
            connect();
        }
        String appId = launchSession.getAppId();
        if (launchSession.getSessionType() == LaunchSession.LaunchSessionType.WebApp) {
            appId = this.mAppToAppIdMappings.get(appId);
        }
        if (appId == null || appId.length() == 0) {
            Util.postError(responseListener, new ServiceCommandError(-1, "You must provide a valid LaunchSession to send messages to", null));
            return;
        }
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("type", "p2p");
            jSONObject.put("to", appId);
            jSONObject.put("payload", obj);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        sendCommand(new ServiceCommand<>(this, null, jSONObject, true, responseListener));
    }

    private void sendSpecialKey(final String str, final ResponseListener<Object> responseListener) {
        try {
            WebOSTVMouseSocketConnection webOSTVMouseSocketConnection = this.mouseSocket;
            if (webOSTVMouseSocketConnection != null) {
                webOSTVMouseSocketConnection.button(str);
                Util.postSuccess(responseListener, null);
            } else {
                connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() { // from class: com.connectsdk.service.WebOSTVService.25
                    @Override // com.connectsdk.service.webos.WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener
                    public void onConnected() {
                        WebOSTVService.this.mouseSocket.button(str);
                        Util.postSuccess(responseListener, null);
                    }
                });
            }
        } catch (Exception e2) {
            Util.postError(responseListener, new ServiceCommandError(str, e2.getMessage()));
            e2.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WebOSWebAppSession webAppSessionForLaunchSession(LaunchSession launchSession) {
        if (this.mWebAppSessions == null) {
            this.mWebAppSessions = new ConcurrentHashMap<>();
        }
        if (launchSession.getService() == null) {
            launchSession.setService(this);
        }
        WebOSWebAppSession webOSWebAppSession = this.mWebAppSessions.get(launchSession.getAppId());
        if (webOSWebAppSession != null) {
            return webOSWebAppSession;
        }
        WebOSWebAppSession webOSWebAppSession2 = new WebOSWebAppSession(launchSession, this);
        this.mWebAppSessions.put(launchSession.getAppId(), webOSWebAppSession2);
        return webOSWebAppSession2;
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void back(ResponseListener<Object> responseListener) {
        sendSpecialKey("BACK", responseListener);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void blue(ResponseListener<Object> responseListener) {
        sendSpecialKey("BLUE", responseListener);
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public void button(String str) {
    }

    @Override // com.connectsdk.service.DeviceService
    public void cancelPairing() {
        WebOSTVServiceSocketClient webOSTVServiceSocketClient = this.socket;
        if (webOSTVServiceSocketClient != null) {
            webOSTVServiceSocketClient.disconnect();
        }
    }

    public void channelDown() {
        channelDown(null);
    }

    public void channelUp() {
        channelUp(null);
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public void click() {
        WebOSTVMouseSocketConnection webOSTVMouseSocketConnection = this.mouseSocket;
        if (webOSTVMouseSocketConnection != null) {
            webOSTVMouseSocketConnection.click();
        } else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() { // from class: com.connectsdk.service.WebOSTVService.22
                @Override // com.connectsdk.service.webos.WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener
                public void onConnected() {
                    WebOSTVService.this.mouseSocket.click();
                }
            });
        }
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void closeApp(LaunchSession launchSession, ResponseListener<Object> responseListener) {
        String appId = launchSession.getAppId();
        String sessionId = launchSession.getSessionId();
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("id", appId);
            jSONObject.put("sessionId", sessionId);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        new ServiceCommand(launchSession.getService(), "ssap://system.launcher/close", jSONObject, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.ExternalInputControl
    public void closeInputPicker(LaunchSession launchSession, ResponseListener<Object> responseListener) {
        closeApp(launchSession, responseListener);
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void closeMedia(LaunchSession launchSession, ResponseListener<Object> responseListener) {
        JSONObject jSONObject = new JSONObject();
        try {
            if (launchSession.getAppId() != null && launchSession.getAppId().length() > 0) {
                jSONObject.put("id", launchSession.getAppId());
            }
            if (launchSession.getSessionId() != null && launchSession.getSessionId().length() > 0) {
                jSONObject.put("sessionId", launchSession.getSessionId());
            }
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        new ServiceCommand(launchSession.getService(), "ssap://media.viewer/close", jSONObject, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void closeWebApp(LaunchSession launchSession, ResponseListener<Object> responseListener) {
        if (launchSession == null || launchSession.getAppId() == null || launchSession.getAppId().length() == 0) {
            Util.postError(responseListener, new ServiceCommandError(0, "Must provide a valid launch session", null));
            return;
        }
        WebOSWebAppSession webOSWebAppSession = this.mWebAppSessions.get(launchSession.getAppId());
        if (webOSWebAppSession != null) {
            webOSWebAppSession.disconnectFromWebApp();
        }
        JSONObject jSONObject = new JSONObject();
        try {
            if (launchSession.getAppId() != null) {
                jSONObject.put("webAppId", launchSession.getAppId());
            }
            if (launchSession.getSessionId() != null) {
                jSONObject.put("sessionId", launchSession.getSessionId());
            }
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        new ServiceCommand(this, "ssap://webapp/closeWebApp", jSONObject, true, responseListener).send();
    }

    @Override // com.connectsdk.service.DeviceService
    public void connect() {
        if (this.socket == null) {
            WebOSTVServiceSocketClient webOSTVServiceSocketClient = new WebOSTVServiceSocketClient(getWebOSTVServiceConfig(), getPairingType(), getPermissions(), WebOSTVServiceSocketClient.getURI(getServiceDescription().getIpAddress(), getServiceDescription().getPort()));
            this.socket = webOSTVServiceSocketClient;
            webOSTVServiceSocketClient.setListener(this.mSocketListener);
        }
        if (isConnected()) {
            return;
        }
        this.socket.connect();
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public void connectMouse() {
        connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() { // from class: com.connectsdk.service.WebOSTVService.20
            @Override // com.connectsdk.service.webos.WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener
            public void onConnected() {
            }
        });
    }

    public void connectToApp(String str, final WebAppSession.LaunchListener launchListener) {
        LaunchSession launchSessionLaunchSessionForAppId = LaunchSession.launchSessionForAppId(str);
        launchSessionLaunchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.App);
        launchSessionLaunchSessionForAppId.setService(this);
        final WebOSWebAppSession webOSWebAppSessionWebAppSessionForLaunchSession = webAppSessionForLaunchSession(launchSessionLaunchSessionForAppId);
        connectToWebApp(webOSWebAppSessionWebAppSessionForLaunchSession, false, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.33
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(launchListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                Util.postSuccess(launchListener, webOSWebAppSessionWebAppSessionForLaunchSession);
            }
        });
    }

    public void connectToWebApp(final WebOSWebAppSession webOSWebAppSession, final boolean z, final ResponseListener<Object> responseListener) {
        LaunchSession launchSession;
        if (this.mWebAppSessions == null) {
            this.mWebAppSessions = new ConcurrentHashMap<>();
        }
        if (this.mAppToAppIdMappings == null) {
            this.mAppToAppIdMappings = new ConcurrentHashMap<>();
        }
        if (webOSWebAppSession == null || (launchSession = webOSWebAppSession.launchSession) == null) {
            Util.postError(responseListener, new ServiceCommandError(0, "You must provide a valid LaunchSession object", null));
            return;
        }
        final String appId = launchSession.getAppId();
        String str = webOSWebAppSession.launchSession.getSessionType() == LaunchSession.LaunchSessionType.WebApp ? "webAppId" : "appId";
        if (appId == null || appId.length() == 0) {
            Util.postError(responseListener, new ServiceCommandError(-1, "You must provide a valid web app session", null));
            return;
        }
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put(str, appId);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        URLServiceSubscription<ResponseListener<Object>> uRLServiceSubscription = new URLServiceSubscription<>(webOSWebAppSession.socket, "ssap://webapp/connectToApp", jSONObject, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.29
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                webOSWebAppSession.disconnectFromWebApp();
                if (!((serviceCommandError == null || serviceCommandError.getPayload() == null) ? false : serviceCommandError.getPayload().toString().contains("app channel closed"))) {
                    Util.postError(responseListener, serviceCommandError);
                } else if (webOSWebAppSession.getWebAppSessionListener() != null) {
                    Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.WebOSTVService.29.2
                        @Override // java.lang.Runnable
                        public void run() {
                            webOSWebAppSession.getWebAppSessionListener().onWebAppSessionDisconnect(webOSWebAppSession);
                        }
                    });
                }
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(final Object obj) {
                JSONObject jSONObject2 = (JSONObject) obj;
                String strOptString = jSONObject2.optString("state");
                if (!strOptString.equalsIgnoreCase("CONNECTED")) {
                    if (z && strOptString.equalsIgnoreCase("WAITING_FOR_APP")) {
                        Util.postError(responseListener, new ServiceCommandError(0, "Web app is not currently running", null));
                        return;
                    }
                    return;
                }
                String strOptString2 = jSONObject2.optString("appId");
                if (strOptString2 != null && strOptString2.length() != 0) {
                    if (webOSWebAppSession.launchSession.getSessionType() == LaunchSession.LaunchSessionType.WebApp) {
                        WebOSTVService.this.mAppToAppIdMappings.put(strOptString2, appId);
                    }
                    webOSWebAppSession.setFullAppId(strOptString2);
                }
                if (responseListener != null) {
                    Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.WebOSTVService.29.1
                        @Override // java.lang.Runnable
                        public void run() {
                            responseListener.onSuccess(obj);
                        }
                    });
                }
            }
        });
        webOSWebAppSession.appToAppSubscription = uRLServiceSubscription;
        uRLServiceSubscription.subscribe();
    }

    @Override // com.connectsdk.service.DeviceService
    public void disconnect() {
        String str = Util.T;
        this.serviceDescription.getIpAddress();
        Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.WebOSTVService.1
            @Override // java.lang.Runnable
            public void run() {
                WebOSTVService webOSTVService = WebOSTVService.this;
                DeviceService.DeviceServiceListener deviceServiceListener = webOSTVService.listener;
                if (deviceServiceListener != null) {
                    deviceServiceListener.onDisconnect(webOSTVService, null);
                }
            }
        });
        WebOSTVServiceSocketClient webOSTVServiceSocketClient = this.socket;
        if (webOSTVServiceSocketClient != null) {
            webOSTVServiceSocketClient.setListener(null);
            this.socket.disconnect();
            this.socket = null;
        }
        ConcurrentHashMap<String, String> concurrentHashMap = this.mAppToAppIdMappings;
        if (concurrentHashMap != null) {
            concurrentHashMap.clear();
        }
        ConcurrentHashMap<String, WebOSWebAppSession> concurrentHashMap2 = this.mWebAppSessions;
        if (concurrentHashMap2 != null) {
            Enumeration<WebOSWebAppSession> enumerationElements = concurrentHashMap2.elements();
            while (enumerationElements.hasMoreElements()) {
                enumerationElements.nextElement().disconnectFromWebApp();
            }
            this.mWebAppSessions.clear();
        }
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public void disconnectMouse() {
        WebOSTVMouseSocketConnection webOSTVMouseSocketConnection = this.mouseSocket;
        if (webOSTVMouseSocketConnection == null) {
            return;
        }
        webOSTVMouseSocketConnection.disconnect();
        this.mouseSocket = null;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void displayImage(final String str, final String str2, final String str3, final String str4, final String str5, final MediaPlayer.LaunchListener launchListener) {
        JSONObject jSONObject;
        MediaPlayer mediaPlayer;
        if (!"4.0.0".equalsIgnoreCase(this.serviceDescription.getVersion())) {
            final WebAppSession.LaunchListener launchListener2 = new WebAppSession.LaunchListener() { // from class: com.connectsdk.service.WebOSTVService.11
                @Override // com.connectsdk.service.capability.listeners.ErrorListener
                public void onError(ServiceCommandError serviceCommandError) {
                    launchListener.onError(serviceCommandError);
                }

                @Override // com.connectsdk.service.capability.listeners.ResponseListener
                public void onSuccess(WebAppSession webAppSession) {
                    webAppSession.displayImage(str, str2, str3, str4, str5, launchListener);
                }
            };
            getWebAppLauncher().joinWebApp("MediaPlayer", new WebAppSession.LaunchListener() { // from class: com.connectsdk.service.WebOSTVService.12
                @Override // com.connectsdk.service.capability.listeners.ErrorListener
                public void onError(ServiceCommandError serviceCommandError) {
                    WebOSTVService.this.getWebAppLauncher().launchWebApp("MediaPlayer", launchListener2);
                }

                @Override // com.connectsdk.service.capability.listeners.ResponseListener
                public void onSuccess(WebAppSession webAppSession) {
                    webAppSession.displayImage(str, str2, str3, str4, str5, launchListener);
                }
            });
            return;
        }
        DeviceService dLNAService = getDLNAService();
        if (dLNAService != null && (mediaPlayer = (MediaPlayer) dLNAService.getAPI(MediaPlayer.class)) != null) {
            mediaPlayer.displayImage(str, str2, str3, str4, str5, launchListener);
            return;
        }
        try {
            jSONObject = new JSONObject() {

                {
                    put("target", str);
                    put("title", str3 == null ? JSONObject.NULL : str3);
                    put("description", str4 == null ? JSONObject.NULL : str4);
                    put("mimeType", str2 == null ? JSONObject.NULL : str2);
                    put("iconSrc", str5 == null ? JSONObject.NULL : str5);
                }
            };
        } catch (JSONException e2) {
            e2.printStackTrace();
            Util.postError(launchListener, new ServiceCommandError(-1, e2.getLocalizedMessage(), e2));
            jSONObject = null;
        }
        if (jSONObject != null) {
            displayMedia(jSONObject, launchListener);
        }
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void down(ResponseListener<Object> responseListener) {
        sendSpecialKey("DOWN", responseListener);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void exit_lg(ResponseListener<Object> responseListener) {
        sendSpecialKey("EXIT", responseListener);
    }

    public void getACRAuthToken(final ACRAuthTokenListener aCRAuthTokenListener) {
        new ServiceCommand(this, "ssap://tv/getACRAuthToken", null, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.38
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(aCRAuthTokenListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                try {
                    Util.postSuccess(aCRAuthTokenListener, (String) ((JSONObject) obj).get("token"));
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }).send();
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void getAppList(final Launcher.AppListListener appListListener) {
        new ServiceCommand(this, "ssap://com.webos.applicationManager/listApps", null, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.8
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(appListListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                try {
                    JSONArray jSONArray = (JSONArray) ((JSONObject) obj).get("apps");
                    ArrayList arrayList = new ArrayList();
                    for (int i = 0; i < jSONArray.length(); i++) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i);
                        AppInfo appInfo = new AppInfo();
                        appInfo.setId(jSONObject.getString("id"));
                        appInfo.setName(jSONObject.getString("title"));
                        appInfo.setRawData(jSONObject);
                        arrayList.add(appInfo);
                    }
                    Util.postSuccess(appListListener, arrayList);
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }).send();
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void getAppState(LaunchSession launchSession, Launcher.AppStateListener appStateListener) {
        getAppState(false, launchSession, appStateListener);
    }

    public void getChannelCurrentProgramInfo(TVControl.ProgramInfoListener programInfoListener) {
        getChannelCurrentProgramInfo(false, programInfoListener);
    }

    @Override // com.connectsdk.service.capability.TVControl
    public void getChannelList(TVControl.ChannelListListener channelListListener) {
        getChannelList(false, channelListListener);
    }

    public String getClientKey() {
        return getWebOSTVServiceConfig().getClientKey();
    }

    @Override // com.connectsdk.service.capability.TVControl
    public void getCurrentChannel(TVControl.ChannelListener channelListener) {
        getCurrentChannel(false, channelListener);
    }

    @Override // com.connectsdk.service.capability.ExternalInputControl
    public ExternalInputControl getExternalInput() {
        return this;
    }

    @Override // com.connectsdk.service.capability.ExternalInputControl
    public CapabilityMethods.CapabilityPriorityLevel getExternalInputControlPriorityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.ExternalInputControl
    public void getExternalInputList(final ExternalInputControl.ExternalInputListListener externalInputListListener) {
        new ServiceCommand(this, "ssap://tv/getExternalInputList", null, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.19
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(externalInputListListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                try {
                    Util.postSuccess(externalInputListListener, WebOSTVService.this.externalnputInfoFromJSONArray((JSONArray) ((JSONObject) obj).get("devices")));
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }).send();
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public KeyControl getKeyControl() {
        return this;
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public CapabilityMethods.CapabilityPriorityLevel getKeyControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    public void getLaunchPoints(final LaunchPointsListener launchPointsListener) {
        new ServiceCommand(this, "ssap://com.webos.applicationManager/listLaunchPoints", null, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.39
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(launchPointsListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                try {
                    Util.postSuccess(launchPointsListener, (JSONArray) ((JSONObject) obj).get("launchPoints"));
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }).send();
    }

    @Override // com.connectsdk.service.capability.Launcher
    public Launcher getLauncher() {
        return this;
    }

    @Override // com.connectsdk.service.capability.Launcher
    public CapabilityMethods.CapabilityPriorityLevel getLauncherCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void getMediaInfo(MediaPlayer.MediaInfoListener mediaInfoListener) {
        Util.postError(mediaInfoListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public CapabilityMethods.CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public MouseControl getMouseControl() {
        return this;
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public CapabilityMethods.CapabilityPriorityLevel getMouseControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void getMute(VolumeControl.MuteListener muteListener) {
        getMuteStatus(false, muteListener);
    }

    public List<String> getPermissions() {
        List<String> list = this.permissions;
        if (list != null) {
            return list;
        }
        ArrayList arrayList = new ArrayList();
        Collections.addAll(arrayList, WebOSTVDeviceService.kWebOSTVServiceOpenPermissions);
        if (DiscoveryManager.getInstance().getPairingLevel() == DiscoveryManager.PairingLevel.PROTECTED) {
            Collections.addAll(arrayList, WebOSTVDeviceService.kWebOSTVServiceProtectedPermissions);
        } else if (DiscoveryManager.getInstance().getPairingLevel() == DiscoveryManager.PairingLevel.ON) {
            Collections.addAll(arrayList, WebOSTVDeviceService.kWebOSTVServiceProtectedPermissions);
            Collections.addAll(arrayList, WebOSTVDeviceService.kWebOSTVServicePersonalActivityPermissions);
        }
        this.permissions = arrayList;
        return arrayList;
    }

    @Override // com.connectsdk.service.webos.WebOSTVDeviceService, com.connectsdk.service.capability.MediaControl
    public void getPlayState(MediaControl.PlayStateListener playStateListener) {
        Util.postError(playStateListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.PlaylistControl
    public PlaylistControl getPlaylistControl() {
        return this;
    }

    @Override // com.connectsdk.service.capability.PlaylistControl
    public CapabilityMethods.CapabilityPriorityLevel getPlaylistControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.DeviceService
    public CapabilityMethods.CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> cls) {
        if (cls.equals(MediaPlayer.class)) {
            return getMediaPlayerCapabilityLevel();
        }
        if (cls.equals(MediaControl.class)) {
            return getMediaControlCapabilityLevel();
        }
        if (cls.equals(Launcher.class)) {
            return getLauncherCapabilityLevel();
        }
        if (cls.equals(TVControl.class)) {
            return getTVControlCapabilityLevel();
        }
        if (cls.equals(VolumeControl.class)) {
            return getVolumeControlCapabilityLevel();
        }
        if (cls.equals(ExternalInputControl.class)) {
            return getExternalInputControlPriorityLevel();
        }
        if (cls.equals(MouseControl.class)) {
            return getMouseControlCapabilityLevel();
        }
        if (cls.equals(TextInputControl.class)) {
            return getTextInputControlCapabilityLevel();
        }
        if (cls.equals(PowerControl.class)) {
            return getPowerControlCapabilityLevel();
        }
        if (cls.equals(KeyControl.class)) {
            return getKeyControlCapabilityLevel();
        }
        if (cls.equals(ToastControl.class)) {
            return getToastControlCapabilityLevel();
        }
        if (cls.equals(WebAppLauncher.class)) {
            return getWebAppLauncherCapabilityLevel();
        }
        return cls.equals(PlaylistControl.class) ? getPlaylistControlCapabilityLevel() : CapabilityMethods.CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override // com.connectsdk.service.capability.TVControl
    public void getProgramInfo(TVControl.ProgramInfoListener programInfoListener) {
        Util.postError(programInfoListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.TVControl
    public void getProgramList(TVControl.ProgramListListener programListListener) {
        getProgramList(false, programListListener);
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void getRunningApp(Launcher.AppInfoListener appInfoListener) {
        getRunningApp(false, appInfoListener);
    }

    public void getServiceInfo(final ServiceInfoListener serviceInfoListener) {
        new ServiceCommand(this, "ssap://api/getServiceList", null, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.35
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(serviceInfoListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                try {
                    Util.postSuccess(serviceInfoListener, (JSONArray) ((JSONObject) obj).get("services"));
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }).send();
    }

    public void getSystemInfo(final SystemInfoListener systemInfoListener) {
        new ServiceCommand(this, "ssap://system/getSystemInfo", null, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.36
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(systemInfoListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                try {
                    Util.postSuccess(systemInfoListener, (JSONObject) ((JSONObject) obj).get("features"));
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }).send();
    }

    @Override // com.connectsdk.service.capability.TVControl
    public TVControl getTVControl() {
        return this;
    }

    @Override // com.connectsdk.service.capability.TVControl
    public CapabilityMethods.CapabilityPriorityLevel getTVControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.TextInputControl
    public TextInputControl getTextInputControl() {
        return this;
    }

    @Override // com.connectsdk.service.capability.TextInputControl
    public CapabilityMethods.CapabilityPriorityLevel getTextInputControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.ToastControl
    public ToastControl getToastControl() {
        return this;
    }

    @Override // com.connectsdk.service.capability.ToastControl
    public CapabilityMethods.CapabilityPriorityLevel getToastControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void getVolume(VolumeControl.VolumeListener volumeListener) {
        getVolume(false, volumeListener);
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public VolumeControl getVolumeControl() {
        return this;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public CapabilityMethods.CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    public void getVolumeStatus(VolumeControl.VolumeStatusListener volumeStatusListener) {
        getVolumeStatus(false, volumeStatusListener);
    }

    public ConcurrentHashMap<String, String> getWebAppIdMappings() {
        return this.mAppToAppIdMappings;
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public WebAppLauncher getWebAppLauncher() {
        return this;
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public CapabilityMethods.CapabilityPriorityLevel getWebAppLauncherCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    public WebOSTVServiceConfig getWebOSTVServiceConfig() {
        return (WebOSTVServiceConfig) this.serviceConfig;
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void green(ResponseListener<Object> responseListener) {
        sendSpecialKey("GREEN", responseListener);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void guide_lg(ResponseListener<Object> responseListener) {
        sendSpecialKey("GUIDE", responseListener);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void home(ResponseListener<Object> responseListener) {
        sendSpecialKey("HOME", responseListener);
    }

    @Override // com.connectsdk.service.DeviceService
    public boolean isConnectable() {
        return true;
    }

    @Override // com.connectsdk.service.DeviceService
    public boolean isConnected() {
        if (this.socket == null) {
            return false;
        }
        if (DiscoveryManager.getInstance().getPairingLevel().compareTo(DiscoveryManager.PairingLevel.PROTECTED) >= 0) {
            return this.socket.isConnected() && this.socket.getClientKey() != "";
        }
        return this.socket.isConnected();
    }

    public void joinApp(String str, WebAppSession.LaunchListener launchListener) {
        LaunchSession launchSessionLaunchSessionForAppId = LaunchSession.launchSessionForAppId(str);
        launchSessionLaunchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.App);
        launchSessionLaunchSessionForAppId.setService(this);
        joinWebApp(launchSessionLaunchSessionForAppId, launchListener);
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void joinWebApp(LaunchSession launchSession, final WebAppSession.LaunchListener launchListener) {
        final WebOSWebAppSession webOSWebAppSessionWebAppSessionForLaunchSession = webAppSessionForLaunchSession(launchSession);
        webOSWebAppSessionWebAppSessionForLaunchSession.join(new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.34
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(launchListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                Util.postSuccess(launchListener, webOSWebAppSessionWebAppSessionForLaunchSession);
            }
        });
    }

    @Override // com.connectsdk.service.capability.PlaylistControl
    public void jumpToTrack(long j, ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void launchApp(String str, Launcher.AppLaunchListener appLaunchListener) {
        AppInfo appInfo = new AppInfo();
        appInfo.setId(str);
        launchAppWithInfo(appInfo, appLaunchListener);
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void launchAppStore(String str, Launcher.AppLaunchListener appLaunchListener) {
        AppInfo appInfo = new AppInfo("com.webos.app.discovery");
        appInfo.setName("LG Store");
        JSONObject jSONObject = new JSONObject();
        if (str != null && str.length() > 0) {
            try {
                jSONObject.put("query", "category/GAME_APPS/".concat(str));
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        }
        launchAppWithInfo(appInfo, jSONObject, appLaunchListener);
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void launchAppWithInfo(AppInfo appInfo, Launcher.AppLaunchListener appLaunchListener) {
        launchAppWithInfo(appInfo, null, appLaunchListener);
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void launchBrowser(String str, final Launcher.AppLaunchListener appLaunchListener) {
        JSONObject jSONObject = new JSONObject();
        ResponseListener<Object> responseListener = new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.4
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(appLaunchListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                JSONObject jSONObject2 = (JSONObject) obj;
                LaunchSession launchSession = new LaunchSession();
                launchSession.setService(WebOSTVService.this);
                launchSession.setAppId(jSONObject2.optString("id"));
                launchSession.setSessionId(jSONObject2.optString("sessionId"));
                launchSession.setSessionType(LaunchSession.LaunchSessionType.App);
                launchSession.setRawData(jSONObject2);
                Util.postSuccess(appLaunchListener, launchSession);
            }
        };
        try {
            jSONObject.put("target", str);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        new ServiceCommand(this, "ssap://system.launcher/open", jSONObject, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void launchHulu(String str, Launcher.AppLaunchListener appLaunchListener) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("contentId", str);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        launchAppWithInfo(new AppInfo() { // from class: com.connectsdk.service.WebOSTVService.6
            {
                setId("hulu");
                setName("Hulu");
            }
        }, jSONObject, appLaunchListener);
    }

    @Override // com.connectsdk.service.capability.ExternalInputControl
    public void launchInputPicker(final Launcher.AppLaunchListener appLaunchListener) {
        final AppInfo appInfo = new AppInfo() { // from class: com.connectsdk.service.WebOSTVService.17
            {
                setId("com.webos.app.inputpicker");
                setName("InputPicker");
            }
        };
        launchAppWithInfo(appInfo, null, new Launcher.AppLaunchListener() { // from class: com.connectsdk.service.WebOSTVService.18
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                appInfo.setId("com.webos.app.inputmgr");
                WebOSTVService.this.launchAppWithInfo(appInfo, null, appLaunchListener);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(LaunchSession launchSession) {
                appLaunchListener.onSuccess(launchSession);
            }
        });
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void launchNetflix(String str, Launcher.AppLaunchListener appLaunchListener) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("contentId", ("m=http%3A%2F%2Fapi.netflix.com%2Fcatalog%2Ftitles%2Fmovies%2F" + str + "&source_type=4"));
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        launchAppWithInfo(new AppInfo() { // from class: com.connectsdk.service.WebOSTVService.7
            {
                setId("netflix");
                setName("Netflix");
            }
        }, jSONObject, appLaunchListener);
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void launchWebApp(String str, WebAppSession.LaunchListener launchListener) {
        launchWebApp(str, null, true, launchListener);
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void launchYouTube(String str, Launcher.AppLaunchListener appLaunchListener) {
        launchYouTube(str, 0.0f, appLaunchListener);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void left(ResponseListener<Object> responseListener) {
        sendSpecialKey("LEFT", responseListener);
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public void move(final double d, final double d2) {
        WebOSTVMouseSocketConnection webOSTVMouseSocketConnection = this.mouseSocket;
        if (webOSTVMouseSocketConnection != null) {
            webOSTVMouseSocketConnection.move(d, d2);
        } else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() { // from class: com.connectsdk.service.WebOSTVService.23
                @Override // com.connectsdk.service.webos.WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener
                public void onConnected() {
                    WebOSTVService.this.mouseSocket.move(d, d2);
                }
            });
        }
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void ok(final ResponseListener<Object> responseListener) {
        WebOSTVMouseSocketConnection webOSTVMouseSocketConnection = this.mouseSocket;
        if (webOSTVMouseSocketConnection == null) {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() { // from class: com.connectsdk.service.WebOSTVService.26
                @Override // com.connectsdk.service.webos.WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener
                public void onConnected() {
                    WebOSTVService.this.mouseSocket.click();
                    Util.postSuccess(responseListener, null);
                }
            });
        } else {
            webOSTVMouseSocketConnection.click();
            Util.postSuccess(responseListener, null);
        }
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void pinWebApp(String str, final ResponseListener<Object> responseListener) {
        if (str == null || str.length() == 0) {
            if (responseListener != null) {
                responseListener.onError(new ServiceCommandError(-1, "You must provide a valid web app id", null));
            }
        } else {
            JSONObject jSONObject = new JSONObject();
            try {
                jSONObject.put("webAppId", str);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
            new URLServiceSubscription(this, "ssap://webapp/pinWebApp", jSONObject, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.30
                @Override // com.connectsdk.service.capability.listeners.ErrorListener
                public void onError(ServiceCommandError serviceCommandError) {
                    Util.postError(responseListener, serviceCommandError);
                }

                @Override // com.connectsdk.service.capability.listeners.ResponseListener
                public void onSuccess(Object obj) {
                    if (((JSONObject) obj).has("pairingType")) {
                        WebOSTVService.this.notifyPairingRequired();
                        return;
                    }
                    ResponseListener responseListener2 = responseListener;
                    if (responseListener2 != null) {
                        responseListener2.onSuccess(obj);
                    }
                }
            }).send();
        }
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void playMedia(String str, String str2, String str3, String str4, String str5, boolean z, MediaPlayer.LaunchListener launchListener) {
        playMedia(new MediaInfo.Builder(str, str2).setTitle(str3).setDescription(str4).setIcon(str5).build(), z, launchListener);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void power(ResponseListener<Object> responseListener) {
        sendSpecialKey("POWER", responseListener);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void red(ResponseListener<Object> responseListener) {
        sendSpecialKey("RED", responseListener);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void right(ResponseListener<Object> responseListener) {
        sendSpecialKey("RIGHT", responseListener);
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public void scroll(final double d, final double d2) {
        WebOSTVMouseSocketConnection webOSTVMouseSocketConnection = this.mouseSocket;
        if (webOSTVMouseSocketConnection != null) {
            webOSTVMouseSocketConnection.scroll(d, d2);
        } else {
            connectMouse(new WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener() { // from class: com.connectsdk.service.WebOSTVService.24
                @Override // com.connectsdk.service.webos.WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener
                public void onConnected() {
                    WebOSTVService.this.mouseSocket.scroll(d, d2);
                }
            });
        }
    }

    public void secureAccessTest(final SecureAccessTestListener secureAccessTestListener) {
        new ServiceCommand(this, "ssap://com.webos.service.secondscreen.gateway/test/secure", null, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.37
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(secureAccessTestListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                try {
                    Boolean bool = (Boolean) ((JSONObject) obj).get("returnValue");
                    bool.booleanValue();
                    Util.postSuccess(secureAccessTestListener, bool);
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }).send();
    }

    @Override // com.connectsdk.service.DeviceService, com.connectsdk.service.command.ServiceCommand.ServiceCommandProcessor
    public void sendCommand(ServiceCommand<?> serviceCommand) {
        WebOSTVServiceSocketClient webOSTVServiceSocketClient = this.socket;
        if (webOSTVServiceSocketClient != null) {
            webOSTVServiceSocketClient.sendCommand(serviceCommand);
        }
    }

    @Override // com.connectsdk.service.capability.TextInputControl
    public void sendDelete() {
        WebOSTVKeyboardInput webOSTVKeyboardInput = this.keyboardInput;
        if (webOSTVKeyboardInput != null) {
            webOSTVKeyboardInput.sendDel();
        }
    }

    @Override // com.connectsdk.service.capability.TextInputControl
    public void sendEnter() {
        WebOSTVKeyboardInput webOSTVKeyboardInput = this.keyboardInput;
        if (webOSTVKeyboardInput != null) {
            webOSTVKeyboardInput.sendEnter();
        }
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void sendKeyCode(KeyControl.KeyCode keyCode, ResponseListener<Object> responseListener) {
        switch (AnonymousClass40.$SwitchMap$com$connectsdk$service$capability$KeyControl$KeyCode[keyCode.ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
                sendSpecialKey(String.valueOf(keyCode.getCode()), responseListener);
                break;
            case 11:
                sendSpecialKey("DASH", responseListener);
                break;
            case 12:
                sendSpecialKey("ENTER", responseListener);
                break;
            default:
                Util.postError(responseListener, new ServiceCommandError(0, "The keycode is not available", null));
                break;
        }
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void sendKeyString(String str, ResponseListener<Object> responseListener) {
        sendSpecialKey(str, responseListener);
    }

    @Override // com.connectsdk.service.DeviceService
    public void sendPairingKey(String str) {
        WebOSTVServiceSocketClient webOSTVServiceSocketClient = this.socket;
        if (webOSTVServiceSocketClient != null) {
            webOSTVServiceSocketClient.sendPairingKey(str);
        }
    }

    @Override // com.connectsdk.service.capability.TextInputControl
    public void sendText(String str) {
        WebOSTVKeyboardInput webOSTVKeyboardInput = this.keyboardInput;
        if (webOSTVKeyboardInput != null) {
            webOSTVKeyboardInput.addToQueue(str);
        }
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void sendVizeoKeyCode(VizioKeyCode vizioKeyCode, ResponseListener<Object> responseListener) {
    }

    @Override // com.connectsdk.service.capability.TVControl
    public void set3DEnabled(boolean z, ResponseListener<Object> responseListener) {
        new ServiceCommand(this, z ? "ssap://com.webos.service.tv.display/set3DOn" : "ssap://com.webos.service.tv.display/set3DOff", null, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.TVControl
    public void setChannel(ChannelInfo channelInfo, ResponseListener<Object> responseListener) {
        if (channelInfo == null) {
            throw new NullPointerException("channelInfo must not be null");
        }
        JSONObject jSONObject = new JSONObject();
        try {
            if (channelInfo.getId() != null) {
                jSONObject.put("channelId", channelInfo.getId());
            }
            if (channelInfo.getNumber() != null) {
                jSONObject.put("channelNumber", channelInfo.getNumber());
            }
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        new ServiceCommand(this, "ssap://tv/openChannel", jSONObject, true, responseListener).send();
    }

    public void setChannelById(String str) {
        setChannelById(str, null);
    }

    public void setClientKey(String str) {
        getWebOSTVServiceConfig().setClientKey(str);
    }

    /* JADX WARN: Code duplicated, block: B:9:0x0019 A[Catch: JSONException -> 0x0017, TRY_LEAVE, TryCatch #0 {JSONException -> 0x0017, blocks: (B:4:0x0007, B:6:0x000d, B:9:0x0019), top: B:15:0x0007 }] */
    @Override // com.connectsdk.service.capability.ExternalInputControl
    public void setExternalInput(ExternalInputInfo externalInputInfo, ResponseListener<Object> responseListener) {
        JSONObject jSONObject = new JSONObject();
        if (externalInputInfo != null) {
            try {
                if (externalInputInfo.getId() != null) {
                    jSONObject.put("inputId", externalInputInfo.getId());
                } else {
                    String str = Util.T;
                }
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
        } else {
            String str2 = Util.T;
        }
        new ServiceCommand(this, "ssap://tv/switchInput", jSONObject, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void setMute(boolean z, ResponseListener<Object> responseListener) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("mute", z);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        new ServiceCommand(this, "ssap://audio/setMute", jSONObject, true, responseListener).send();
    }

    @Override // com.connectsdk.service.webos.WebOSTVDeviceService, com.connectsdk.service.DeviceService
    public void setPairingType(DeviceService.PairingType pairingType) {
        this.pairingType = pairingType;
    }

    public void setPermissions(List<String> list) {
        this.permissions = list;
        WebOSTVServiceConfig webOSTVServiceConfig = (WebOSTVServiceConfig) this.serviceConfig;
        if (webOSTVServiceConfig.getClientKey() != null) {
            webOSTVServiceConfig.setClientKey(null);
            if (isConnected()) {
                String str = Util.T;
                disconnect();
            }
        }
    }

    @Override // com.connectsdk.service.capability.PlaylistControl
    public void setPlayMode(PlaylistControl.PlayMode playMode, ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.DeviceService
    public void setServiceDescription(ServiceDescription serviceDescription) {
        super.setServiceDescription(serviceDescription);
        if (this.serviceDescription.getVersion() != null || this.serviceDescription.getResponseHeaders() == null) {
            return;
        }
        String[] strArrSplit = serviceDescription.getResponseHeaders().get("Server").get(0).split(" ")[0].split("/");
        this.serviceDescription.setVersion(strArrSplit[strArrSplit.length - 1]);
        try {
            DocumentBuilder documentBuilderNewDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(serviceDescription.getLocationXML()));
            String[] strArrSplit2 = documentBuilderNewDocumentBuilder.parse(inputSource).getElementsByTagName("serviceId").item(0).getTextContent().split("-");
            this.serviceDescription.setPort(Integer.parseInt(strArrSplit2[strArrSplit2.length - 1]));
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        updateCapabilities();
    }

    public void setVolume(int i) {
        setVolume(i, null);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void setting_lg(ResponseListener<Object> responseListener) {
        sendSpecialKey("MENU", responseListener);
    }

    @Override // com.connectsdk.service.capability.ToastControl
    public void showClickableToastForApp(String str, AppInfo appInfo, JSONObject jSONObject, ResponseListener<Object> responseListener) {
        showClickableToastForApp(str, appInfo, jSONObject, null, null, responseListener);
    }

    @Override // com.connectsdk.service.capability.ToastControl
    public void showClickableToastForURL(String str, String str2, ResponseListener<Object> responseListener) {
        showClickableToastForURL(str, str2, null, null, responseListener);
    }

    @Override // com.connectsdk.service.capability.ToastControl
    public void showToast(String str, ResponseListener<Object> responseListener) {
        showToast(str, null, null, responseListener);
    }

    @Override // com.connectsdk.service.capability.TVControl
    public ServiceSubscription<TVControl.State3DModeListener> subscribe3DEnabled(TVControl.State3DModeListener state3DModeListener) {
        return (ServiceSubscription) get3DEnabled(true, state3DModeListener);
    }

    @Override // com.connectsdk.service.capability.Launcher
    public ServiceSubscription<Launcher.AppStateListener> subscribeAppState(LaunchSession launchSession, Launcher.AppStateListener appStateListener) {
        return (URLServiceSubscription) getAppState(true, launchSession, appStateListener);
    }

    public ServiceSubscription<TVControl.ProgramInfoListener> subscribeChannelCurrentProgramInfo(TVControl.ProgramInfoListener programInfoListener) {
        return (ServiceSubscription) getChannelCurrentProgramInfo(true, programInfoListener);
    }

    public ServiceSubscription<TVControl.ChannelListListener> subscribeChannelList(TVControl.ChannelListListener channelListListener) {
        return (ServiceSubscription) getChannelList(true, channelListListener);
    }

    @Override // com.connectsdk.service.capability.TVControl
    public ServiceSubscription<TVControl.ChannelListener> subscribeCurrentChannel(TVControl.ChannelListener channelListener) {
        return (ServiceSubscription) getCurrentChannel(true, channelListener);
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public ServiceSubscription<WebAppSession.WebAppPinStatusListener> subscribeIsWebAppPinned(String str, WebAppSession.WebAppPinStatusListener webAppPinStatusListener) {
        return (URLServiceSubscription) isWebAppPinned(true, str, webAppPinStatusListener);
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public ServiceSubscription<MediaPlayer.MediaInfoListener> subscribeMediaInfo(MediaPlayer.MediaInfoListener mediaInfoListener) {
        mediaInfoListener.onError(ServiceCommandError.notSupported());
        return null;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public ServiceSubscription<VolumeControl.MuteListener> subscribeMute(VolumeControl.MuteListener muteListener) {
        return (ServiceSubscription) getMuteStatus(true, muteListener);
    }

    @Override // com.connectsdk.service.webos.WebOSTVDeviceService, com.connectsdk.service.capability.MediaControl
    public ServiceSubscription<MediaControl.PlayStateListener> subscribePlayState(MediaControl.PlayStateListener playStateListener) {
        Util.postError(playStateListener, ServiceCommandError.notSupported());
        return null;
    }

    @Override // com.connectsdk.service.capability.TVControl
    public ServiceSubscription<TVControl.ProgramInfoListener> subscribeProgramInfo(TVControl.ProgramInfoListener programInfoListener) {
        Util.postError(programInfoListener, ServiceCommandError.notSupported());
        return new NotSupportedServiceSubscription();
    }

    @Override // com.connectsdk.service.capability.TVControl
    public ServiceSubscription<TVControl.ProgramListListener> subscribeProgramList(TVControl.ProgramListListener programListListener) {
        return (ServiceSubscription) getProgramList(true, programListListener);
    }

    @Override // com.connectsdk.service.capability.Launcher
    public ServiceSubscription<Launcher.AppInfoListener> subscribeRunningApp(Launcher.AppInfoListener appInfoListener) {
        return (URLServiceSubscription) getRunningApp(true, appInfoListener);
    }

    @Override // com.connectsdk.service.capability.TextInputControl
    public ServiceSubscription<TextInputControl.TextInputStatusListener> subscribeTextInputStatus(TextInputControl.TextInputStatusListener textInputStatusListener) {
        WebOSTVKeyboardInput webOSTVKeyboardInput = new WebOSTVKeyboardInput(this);
        this.keyboardInput = webOSTVKeyboardInput;
        return webOSTVKeyboardInput.connect(textInputStatusListener);
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public ServiceSubscription<VolumeControl.VolumeListener> subscribeVolume(VolumeControl.VolumeListener volumeListener) {
        return (ServiceSubscription) getVolume(true, volumeListener);
    }

    public ServiceSubscription<VolumeControl.VolumeStatusListener> subscribeVolumeStatus(VolumeControl.VolumeStatusListener volumeStatusListener) {
        return (ServiceSubscription) getVolumeStatus(true, volumeStatusListener);
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void unPinWebApp(String str, final ResponseListener<Object> responseListener) {
        if (str == null || str.length() == 0) {
            if (responseListener != null) {
                responseListener.onError(new ServiceCommandError(-1, "You must provide a valid web app id", null));
            }
        } else {
            JSONObject jSONObject = new JSONObject();
            try {
                jSONObject.put("webAppId", str);
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
            new URLServiceSubscription(this, "ssap://webapp/removePinnedWebApp", jSONObject, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.31
                @Override // com.connectsdk.service.capability.listeners.ErrorListener
                public void onError(ServiceCommandError serviceCommandError) {
                    Util.postError(responseListener, serviceCommandError);
                }

                @Override // com.connectsdk.service.capability.listeners.ResponseListener
                public void onSuccess(Object obj) {
                    if (((JSONObject) obj).has("pairingType")) {
                        WebOSTVService.this.notifyPairingRequired();
                        return;
                    }
                    ResponseListener responseListener2 = responseListener;
                    if (responseListener2 != null) {
                        responseListener2.onSuccess(obj);
                    }
                }
            }).send();
        }
    }

    @Override // com.connectsdk.service.DeviceService, com.connectsdk.service.command.ServiceCommand.ServiceCommandProcessor
    public void unsubscribe(URLServiceSubscription<?> uRLServiceSubscription) {
        WebOSTVServiceSocketClient webOSTVServiceSocketClient = this.socket;
        if (webOSTVServiceSocketClient != null) {
            webOSTVServiceSocketClient.unsubscribe(uRLServiceSubscription);
        }
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void up(ResponseListener<Object> responseListener) {
        sendSpecialKey("UP", responseListener);
    }

    @Override // com.connectsdk.service.DeviceService
    public void updateCapabilities() {
        ArrayList arrayList = new ArrayList();
        Collections.addAll(arrayList, VolumeControl.Capabilities);
        String[] strArr = MediaPlayer.Capabilities;
        Collections.addAll(arrayList, strArr);
        if (DiscoveryManager.getInstance().getPairingLevel().compareTo(DiscoveryManager.PairingLevel.PROTECTED) >= 0) {
            Collections.addAll(arrayList, TextInputControl.Capabilities);
            Collections.addAll(arrayList, MouseControl.Capabilities);
            Collections.addAll(arrayList, KeyControl.Capabilities);
            Collections.addAll(arrayList, strArr);
            Collections.addAll(arrayList, Launcher.Capabilities);
            Collections.addAll(arrayList, TVControl.Capabilities);
            Collections.addAll(arrayList, ExternalInputControl.Capabilities);
            Collections.addAll(arrayList, ToastControl.Capabilities);
            arrayList.add("PowerControl.Off");
        } else {
            java.util.Collections.addAll(arrayList, "Launcher.App", "Launcher.App.Params", "Launcher.App.Close", "Launcher.Browser");
            java.util.Collections.addAll(arrayList, "Launcher.Browser.Params", "Launcher.Hulu", "Launcher.Netflix", "Launcher.Netflix.Params");
            java.util.Collections.addAll(arrayList, "Launcher.YouTube", "Launcher.YouTube.Params", "Launcher.AppStore", "Launcher.AppStore.Params");
            arrayList.add("Launcher.AppState");
            arrayList.add("Launcher.AppState.Subscribe");
        }
        ServiceDescription serviceDescription = this.serviceDescription;
        if (serviceDescription != null) {
            if (serviceDescription.getVersion() == null || !(this.serviceDescription.getVersion().contains("4.0.0") || this.serviceDescription.getVersion().contains("4.0.1"))) {
                Collections.addAll(arrayList, WebAppLauncher.Capabilities);
                Collections.addAll(arrayList, MediaControl.Capabilities);
                java.util.Collections.addAll(arrayList, "MediaPlayer.Subtitle.WebVTT", "PlaylistControl.JumpToTrack", "PlaylistControl.Next", "PlaylistControl.Previous");
                arrayList.add("MediaPlayer.Loop");
            } else {
                java.util.Collections.addAll(arrayList, "WebAppLauncher.Launch", "WebAppLauncher.Launch.Params", "MediaControl.Play", "MediaControl.Pause");
                java.util.Collections.addAll(arrayList, "MediaControl.Stop", "MediaControl.Seek", "MediaControl.Position", "MediaControl.Duration");
                arrayList.add("MediaControl.PlayState");
                arrayList.add("WebAppLauncher.Close");
                if (getDLNAService() != null) {
                    arrayList.add("MediaPlayer.Subtitle.SRT");
                }
            }
        }
        setCapabilities(arrayList);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void voice_lg(ResponseListener<Object> responseListener) {
        sendSpecialKey("EXIT", responseListener);
    }

    public void volumeDown() {
        volumeDown(null);
    }

    public void volumeUp() {
        volumeUp(null);
    }

    @Override // com.connectsdk.service.capability.KeyControl
    public void yellow(ResponseListener<Object> responseListener) {
        sendSpecialKey("YELLOW", responseListener);
    }

    private void connectMouse(final WebOSTVMouseSocketConnection.WebOSTVMouseSocketListener webOSTVMouseSocketListener) {
        if (this.mouseSocket != null) {
            return;
        }
        new ServiceCommand(this, "ssap://com.webos.service.networkinput/getPointerInputSocket", null, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.21
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                String str = Util.T;
                serviceCommandError.getMessage();
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj) {
                try {
                    String str = (String) ((JSONObject) obj).get("socketPath");
                    WebOSTVService.this.mouseSocket = new WebOSTVMouseSocketConnection(str, webOSTVMouseSocketListener);
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }).send();
    }

    @Override // com.connectsdk.service.capability.TVControl
    public void channelDown(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, "ssap://tv/channelDown", null, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.TVControl
    public void channelUp(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, "ssap://tv/channelUp", null, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void launchAppWithInfo(AppInfo appInfo, Object obj, final Launcher.AppLaunchListener appLaunchListener) {
        String str;
        JSONObject jSONObject = new JSONObject();
        final String id = appInfo.getId();
        if (obj != null) {
            try {
                str = (String) ((JSONObject) obj).get("contentId");
            } catch (JSONException e2) {
                e2.printStackTrace();
                str = null;
            }
        } else {
            str = null;
        }
        try {
            jSONObject.put("id", id);
            if (str != null) {
                jSONObject.put("contentId", str);
            }
            if (obj != null) {
                jSONObject.put("params", obj);
            }
        } catch (JSONException e3) {
            e3.printStackTrace();
        }
        new ServiceCommand(this, "ssap://system.launcher/launch", jSONObject, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.3
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(appLaunchListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Object obj2) {
                LaunchSession launchSession = new LaunchSession();
                launchSession.setService(WebOSTVService.this);
                launchSession.setAppId(id);
                launchSession.setSessionId(((JSONObject) obj2).optString("sessionId"));
                launchSession.setSessionType(LaunchSession.LaunchSessionType.App);
                Util.postSuccess(appLaunchListener, launchSession);
            }
        }).send();
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void launchWebApp(String str, boolean z, WebAppSession.LaunchListener launchListener) {
        launchWebApp(str, null, z, launchListener);
    }

    @Override // com.connectsdk.service.capability.Launcher
    public void launchYouTube(String str, float f, Launcher.AppLaunchListener appLaunchListener) {
        JSONObject jSONObject = new JSONObject();
        if (str != null && str.length() > 0) {
            if (f < 0.0d) {
                Util.postError(appLaunchListener, new ServiceCommandError(0, "Start time may not be negative", null));
                return;
            } else {
                try {
                    jSONObject.put("contentId", String.format("%s&pairingCode=%s&t=%.1f", str, UUID.randomUUID().toString(), Float.valueOf(f)));
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }
        launchAppWithInfo(new AppInfo() { // from class: com.connectsdk.service.WebOSTVService.5
            {
                setId("youtube.leanback.v4");
                setName("YouTube");
            }
        }, jSONObject, appLaunchListener);
    }

    public void setChannelById(String str, ResponseListener<Object> responseListener) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("channelId", str);
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        new ServiceCommand(this, "ssap://tv/openChannel", jSONObject, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void setVolume(float f, ResponseListener<Object> responseListener) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("volume", Math.round(f * 100.0f));
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        new ServiceCommand(this, "ssap://audio/setVolume", jSONObject, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.ToastControl
    public void showClickableToastForApp(String str, AppInfo appInfo, JSONObject jSONObject, String str2, String str3, ResponseListener<Object> responseListener) {
        JSONObject jSONObject2 = new JSONObject();
        try {
            jSONObject2.put("message", str);
            if (str2 != null) {
                jSONObject2.put("iconData", str2);
                jSONObject2.put("iconExtension", str3);
            }
            if (appInfo != null) {
                JSONObject jSONObject3 = new JSONObject();
                jSONObject3.put("appId", appInfo.getId());
                if (jSONObject != null) {
                    jSONObject3.put("params", jSONObject);
                }
                jSONObject2.put("onClick", jSONObject3);
            }
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        sendToast(jSONObject2, responseListener);
    }

    @Override // com.connectsdk.service.capability.ToastControl
    public void showClickableToastForURL(String str, String str2, String str3, String str4, ResponseListener<Object> responseListener) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("message", str);
            if (str3 != null) {
                jSONObject.put("iconData", str3);
                jSONObject.put("iconExtension", str4);
            }
            if (str2 != null) {
                JSONObject jSONObject2 = new JSONObject();
                jSONObject2.put("target", str2);
                jSONObject.put("onClick", jSONObject2);
            }
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        sendToast(jSONObject, responseListener);
    }

    @Override // com.connectsdk.service.capability.ToastControl
    public void showToast(String str, String str2, String str3, ResponseListener<Object> responseListener) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("message", str);
            if (str2 != null) {
                jSONObject.put("iconData", str2);
                jSONObject.put("iconExtension", str3);
            }
        } catch (JSONException e2) {
            e2.printStackTrace();
        }
        sendToast(jSONObject, responseListener);
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void volumeDown(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, "ssap://audio/volumeDown", null, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void volumeUp(ResponseListener<Object> responseListener) {
        new ServiceCommand(this, "ssap://audio/volumeUp", null, true, responseListener).send();
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void joinWebApp(String str, WebAppSession.LaunchListener launchListener) {
        LaunchSession launchSessionLaunchSessionForAppId = LaunchSession.launchSessionForAppId(str);
        launchSessionLaunchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.WebApp);
        launchSessionLaunchSessionForAppId.setService(this);
        joinWebApp(launchSessionLaunchSessionForAppId, launchListener);
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void launchWebApp(final String str, JSONObject jSONObject, final WebAppSession.LaunchListener launchListener) {
        if (str != null && str.length() != 0) {
            final WebOSWebAppSession webOSWebAppSession = this.mWebAppSessions.get(str);
            JSONObject jSONObject2 = new JSONObject();
            try {
                jSONObject2.put("webAppId", str);
                if (jSONObject != null) {
                    jSONObject2.put("urlParams", jSONObject);
                }
            } catch (JSONException e2) {
                e2.printStackTrace();
            }
            new ServiceCommand(this, "ssap://webapp/launchWebApp", jSONObject2, true, new ResponseListener<Object>() { // from class: com.connectsdk.service.WebOSTVService.27
                @Override // com.connectsdk.service.capability.listeners.ErrorListener
                public void onError(ServiceCommandError serviceCommandError) {
                    Util.postError(launchListener, serviceCommandError);
                }

                @Override // com.connectsdk.service.capability.listeners.ResponseListener
                public void onSuccess(Object obj) {
                    LaunchSession launchSessionLaunchSessionForAppId;
                    JSONObject jSONObject3 = (JSONObject) obj;
                    WebOSWebAppSession webOSWebAppSession2 = webOSWebAppSession;
                    if (webOSWebAppSession2 != null) {
                        launchSessionLaunchSessionForAppId = webOSWebAppSession2.launchSession;
                    } else {
                        launchSessionLaunchSessionForAppId = LaunchSession.launchSessionForAppId(str);
                        webOSWebAppSession2 = new WebOSWebAppSession(launchSessionLaunchSessionForAppId, WebOSTVService.this);
                        WebOSTVService.this.mWebAppSessions.put(str, webOSWebAppSession2);
                    }
                    launchSessionLaunchSessionForAppId.setService(WebOSTVService.this);
                    launchSessionLaunchSessionForAppId.setSessionId(jSONObject3.optString("sessionId"));
                    launchSessionLaunchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.WebApp);
                    launchSessionLaunchSessionForAppId.setRawData(jSONObject3);
                    Util.postSuccess(launchListener, webOSWebAppSession2);
                }
            }).send();
            return;
        }
        Util.postError(launchListener, new ServiceCommandError(-1, "You need to provide a valid webAppId.", null));
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public void move(PointF pointF) {
        move(pointF.x, pointF.y);
    }

    @Override // com.connectsdk.service.capability.MouseControl
    public void scroll(PointF pointF) {
        scroll(pointF.x, pointF.y);
    }

    @Override // com.connectsdk.service.capability.TVControl
    public void get3DEnabled(TVControl.State3DModeListener state3DModeListener) {
        get3DEnabled(false, state3DModeListener);
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void playMedia(MediaInfo mediaInfo, boolean z, MediaPlayer.LaunchListener launchListener) {
        if ("4.0.0".equalsIgnoreCase(this.serviceDescription.getVersion())) {
            playMediaByNativeApp(mediaInfo, z, launchListener);
        } else {
            playMediaByWebApp(mediaInfo, z, launchListener);
        }
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void displayImage(MediaInfo mediaInfo, MediaPlayer.LaunchListener launchListener) {
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        String url = null;
        if (mediaInfo != null) {
            String url2 = mediaInfo.getUrl();
            String mimeType = mediaInfo.getMimeType();
            String title = mediaInfo.getTitle();
            String description = mediaInfo.getDescription();
            if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
                url = mediaInfo.getImages().get(0).getUrl();
            }
            str5 = url;
            str = url2;
            str2 = mimeType;
            str3 = title;
            str4 = description;
        } else {
            str = null;
            str2 = null;
            str3 = null;
            str4 = null;
            str5 = null;
        }
        displayImage(str, str2, str3, str4, str5, launchListener);
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void isWebAppPinned(String str, WebAppSession.WebAppPinStatusListener webAppPinStatusListener) {
        isWebAppPinned(false, str, webAppPinStatusListener);
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void launchWebApp(final String str, final JSONObject jSONObject, boolean z, final WebAppSession.LaunchListener launchListener) {
        if (str == null) {
            Util.postError(launchListener, new ServiceCommandError(0, "Must pass a web App id", null));
        } else if (z) {
            launchWebApp(str, jSONObject, launchListener);
        } else {
            getLauncher().getRunningApp(new Launcher.AppInfoListener() { // from class: com.connectsdk.service.WebOSTVService.28
                @Override // com.connectsdk.service.capability.listeners.ErrorListener
                public void onError(ServiceCommandError serviceCommandError) {
                    Util.postError(launchListener, serviceCommandError);
                }

                @Override // com.connectsdk.service.capability.listeners.ResponseListener
                public void onSuccess(AppInfo appInfo) {
                    if (appInfo.getId().indexOf(str) == -1) {
                        WebOSTVService.this.launchWebApp(str, jSONObject, launchListener);
                        return;
                    }
                    LaunchSession launchSessionLaunchSessionForAppId = LaunchSession.launchSessionForAppId(str);
                    launchSessionLaunchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.WebApp);
                    launchSessionLaunchSessionForAppId.setService(WebOSTVService.this);
                    launchSessionLaunchSessionForAppId.setRawData(appInfo.getRawData());
                    Util.postSuccess(launchListener, WebOSTVService.this.webAppSessionForLaunchSession(launchSessionLaunchSessionForAppId));
                }
            });
        }
    }

    public void sendMessage(String str, LaunchSession launchSession, ResponseListener<Object> responseListener) {
        if (str != null && str.length() > 0) {
            sendMessage((Object) str, launchSession, responseListener);
        } else {
            Util.postError(responseListener, new ServiceCommandError(0, "Cannot send a null message", null));
        }
    }

    public void sendMessage(JSONObject jSONObject, LaunchSession launchSession, ResponseListener<Object> responseListener) {
        if (jSONObject != null && jSONObject.length() > 0) {
            sendMessage((Object) jSONObject, launchSession, responseListener);
        } else {
            Util.postError(responseListener, new ServiceCommandError(0, "Cannot send a null message", null));
        }
    }
}

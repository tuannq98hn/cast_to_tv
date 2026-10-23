package com.connectsdk.service;

import android.net.Uri;
import android.os.Bundle;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.CastWebAppSession;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class CastService extends DeviceService implements MediaPlayer, MediaControl, VolumeControl, WebAppLauncher {
    public static final String CAST_SERVICE_MUTE_SUBSCRIPTION_NAME = "mute";
    public static final String CAST_SERVICE_VOLUME_SUBSCRIPTION_NAME = "volume";
    public static final String ID = "Chromecast";
    private static final long MEDIA_TRACK_ID = 1;
    public static final String PLAY_STATE = "PlayState";
    static String applicationID = "F23CE7E5";
    CastDevice castDevice;
    CopyOnWriteArraySet<ConnectionListener> commandQueue;
    String currentAppId;
    boolean currentMuteStatus;
    float currentVolumeLevel;
    String launchingAppId;
    GoogleApiClient mApiClient;
    CastClient mCastClient;
    CastListener mCastClientListener;
    ConnectionCallbacks mConnectionCallbacks;
    ConnectionFailedListener mConnectionFailedListener;
    RemoteMediaPlayer mMediaPlayer;
    boolean mWaitingForReconnect;
    Map<String, CastWebAppSession> sessions;
    List<URLServiceSubscription<?>> subscriptions;

    /* JADX INFO: renamed from: com.connectsdk.service.CastService$15, reason: invalid class name */
    public class AnonymousClass15 implements ConnectionListener {
        final /* synthetic */ ResponseListener val$listener;

        public AnonymousClass15(ResponseListener responseListener) {
            this.val$listener = responseListener;
        }

        @Override // com.connectsdk.service.CastService.ConnectionListener
        public void onConnected() {
            try {
                CastService castService = CastService.this;
                castService.mCastClient.joinApplication(castService.mApiClient).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() { // from class: com.connectsdk.service.CastService.15.1
                    @Override // com.google.android.gms.common.api.ResultCallback
                    public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
                        CastService castService2;
                        RemoteMediaPlayer remoteMediaPlayer;
                        GoogleApiClient googleApiClient;
                        if (!applicationConnectionResult.getStatus().isSuccess()) {
                            Util.postError(AnonymousClass15.this.val$listener, new ServiceCommandError(0, "Failed to join application", applicationConnectionResult));
                            return;
                        }
                        if (applicationConnectionResult.getApplicationMetadata() == null || applicationConnectionResult.getApplicationMetadata().getName() == null || applicationConnectionResult.getApplicationMetadata().getName().equals("Backdrop") || (remoteMediaPlayer = (castService2 = CastService.this).mMediaPlayer) == null || (googleApiClient = castService2.mApiClient) == null) {
                            Util.postSuccess(AnonymousClass15.this.val$listener, applicationConnectionResult);
                        } else {
                            remoteMediaPlayer.requestStatus(googleApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() { // from class: com.connectsdk.service.CastService.15.1.1
                                @Override // com.google.android.gms.common.api.ResultCallback
                                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                                    Util.postSuccess(AnonymousClass15.this.val$listener, mediaChannelResult);
                                }
                            });
                        }
                    }
                });
            } catch (Exception unused) {
                Util.postError(this.val$listener, new ServiceCommandError(0, "Unable to join", null));
            }
        }
    }

    /* JADX INFO: renamed from: com.connectsdk.service.CastService$16, reason: invalid class name */
    public class AnonymousClass16 implements LaunchWebAppListener {
        final /* synthetic */ WebAppSession.LaunchListener val$listener;

        public AnonymousClass16(WebAppSession.LaunchListener launchListener) {
            this.val$listener = launchListener;
        }

        @Override // com.connectsdk.service.CastService.LaunchWebAppListener
        public void onFailure(ServiceCommandError serviceCommandError) {
            Util.postError(this.val$listener, serviceCommandError);
        }

        @Override // com.connectsdk.service.CastService.LaunchWebAppListener
        public void onSuccess(final WebAppSession webAppSession) {
            webAppSession.connect(new ResponseListener<Object>() { // from class: com.connectsdk.service.CastService.16.1
                @Override // com.connectsdk.service.capability.listeners.ErrorListener
                public void onError(ServiceCommandError serviceCommandError) {
                    Util.postError(AnonymousClass16.this.val$listener, serviceCommandError);
                }

                @Override // com.connectsdk.service.capability.listeners.ResponseListener
                public void onSuccess(Object obj) {
                    CastService.this.requestStatus(new ResponseListener<Object>() { // from class: com.connectsdk.service.CastService.16.1.1
                        @Override // com.connectsdk.service.capability.listeners.ErrorListener
                        public void onError(ServiceCommandError serviceCommandError) {
                            Util.postSuccess(AnonymousClass16.this.val$listener, webAppSession);
                        }

                        @Override // com.connectsdk.service.capability.listeners.ResponseListener
                        public void onSuccess(Object obj2) {
                            Util.postSuccess(AnonymousClass16.this.val$listener, webAppSession);
                        }
                    });
                }
            });
        }
    }

    public class ApplicationConnectionResultCallback implements ResultCallback<Cast.ApplicationConnectionResult> {
        LaunchWebAppListener listener;

        public ApplicationConnectionResultCallback(LaunchWebAppListener launchWebAppListener) {
            this.listener = launchWebAppListener;
        }

        @Override // com.google.android.gms.common.api.ResultCallback
        public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
            Status status = applicationConnectionResult.getStatus();
            if (!status.isSuccess()) {
                LaunchWebAppListener launchWebAppListener = this.listener;
                if (launchWebAppListener != null) {
                    launchWebAppListener.onFailure(new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                    return;
                }
                return;
            }
            ApplicationMetadata applicationMetadata = applicationConnectionResult.getApplicationMetadata();
            CastService.this.currentAppId = applicationMetadata.getApplicationId();
            LaunchSession launchSessionLaunchSessionForAppId = LaunchSession.launchSessionForAppId(applicationMetadata.getApplicationId());
            launchSessionLaunchSessionForAppId.setAppName(applicationMetadata.getName());
            launchSessionLaunchSessionForAppId.setSessionId(applicationConnectionResult.getSessionId());
            launchSessionLaunchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.WebApp);
            launchSessionLaunchSessionForAppId.setService(CastService.this);
            CastWebAppSession castWebAppSession = new CastWebAppSession(launchSessionLaunchSessionForAppId, CastService.this);
            castWebAppSession.setMetadata(applicationMetadata);
            CastService.this.sessions.put(applicationMetadata.getApplicationId(), castWebAppSession);
            LaunchWebAppListener launchWebAppListener2 = this.listener;
            if (launchWebAppListener2 != null) {
                launchWebAppListener2.onSuccess(castWebAppSession);
            }
            CastService.this.launchingAppId = null;
        }
    }

    public static class CastClientException extends Exception {
        public CastClientException(String str, Exception exc) {
            super(str, exc);
        }
    }

    public class CastListener extends Cast.Listener {
        public /* synthetic */ CastListener(CastService castService, int i) {
            this();
        }

        @Override // com.google.android.gms.cast.Cast.Listener
        public void onApplicationDisconnected(int i) {
            CastWebAppSession castWebAppSession;
            String str = Util.T;
            CastService castService = CastService.this;
            String str2 = castService.currentAppId;
            if (str2 == null || (castWebAppSession = castService.sessions.get(str2)) == null) {
                return;
            }
            castWebAppSession.handleAppClose();
            CastService.this.currentAppId = null;
        }

        @Override // com.google.android.gms.cast.Cast.Listener
        public void onApplicationStatusChanged() {
            CastService.this.runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.CastListener.1
                @Override // com.connectsdk.service.CastService.ConnectionListener
                public void onConnected() {
                    CastService castService = CastService.this;
                    GoogleApiClient googleApiClient = castService.mApiClient;
                    if (googleApiClient != null) {
                        try {
                            ApplicationMetadata applicationMetadata = castService.mCastClient.getApplicationMetadata(googleApiClient);
                            if (applicationMetadata != null) {
                                CastService.this.currentAppId = applicationMetadata.getApplicationId();
                            }
                        } catch (CastClientException unused) {
                            String str = Util.T;
                        }
                    }
                }
            });
        }

        @Override // com.google.android.gms.cast.Cast.Listener
        public void onVolumeChanged() {
            CastService.this.runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.CastListener.2
                @Override // com.connectsdk.service.CastService.ConnectionListener
                public void onConnected() {
                    try {
                        CastService castService = CastService.this;
                        castService.currentVolumeLevel = (float) castService.mCastClient.getVolume(castService.mApiClient);
                        CastService castService2 = CastService.this;
                        castService2.currentMuteStatus = castService2.mCastClient.isMute(castService2.mApiClient);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    if (CastService.this.subscriptions.size() > 0) {
                        for (URLServiceSubscription<?> uRLServiceSubscription : CastService.this.subscriptions) {
                            int i = 0;
                            if (uRLServiceSubscription.getTarget().equals("volume")) {
                                while (i < uRLServiceSubscription.getListeners().size()) {
                                    Util.postSuccess((ResponseListener) uRLServiceSubscription.getListeners().get(i), Float.valueOf(CastService.this.currentVolumeLevel));
                                    i++;
                                }
                            } else if (uRLServiceSubscription.getTarget().equals("mute")) {
                                while (i < uRLServiceSubscription.getListeners().size()) {
                                    Util.postSuccess((ResponseListener) uRLServiceSubscription.getListeners().get(i), Boolean.valueOf(CastService.this.currentMuteStatus));
                                    i++;
                                }
                            }
                        }
                    }
                }
            });
        }

        private CastListener() {
        }
    }

    public class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        public /* synthetic */ ConnectionCallbacks(CastService castService, int i) {
            this();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void joinFinished() {
            CastService castService = CastService.this;
            if (castService.mWaitingForReconnect) {
                castService.mWaitingForReconnect = false;
            } else {
                castService.connected = true;
                castService.reportConnected(true);
            }
            if (CastService.this.commandQueue.isEmpty()) {
                return;
            }
            for (ConnectionListener connectionListener : CastService.this.commandQueue) {
                connectionListener.onConnected();
                CastService.this.commandQueue.remove(connectionListener);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onJoinApplicationResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
            CastService castService;
            RemoteMediaPlayer remoteMediaPlayer;
            GoogleApiClient googleApiClient;
            if (!applicationConnectionResult.getStatus().isSuccess()) {
                joinFinished();
                return;
            }
            if (applicationConnectionResult.getApplicationMetadata() == null || applicationConnectionResult.getApplicationMetadata().getName() == null || applicationConnectionResult.getApplicationMetadata().getName().equals("Backdrop") || (remoteMediaPlayer = (castService = CastService.this).mMediaPlayer) == null || (googleApiClient = castService.mApiClient) == null) {
                joinFinished();
            } else {
                remoteMediaPlayer.requestStatus(googleApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() { // from class: com.connectsdk.service.CastService.ConnectionCallbacks.2
                    @Override // com.google.android.gms.common.api.ResultCallback
                    public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                        ConnectionCallbacks.this.joinFinished();
                    }
                });
            }
        }

        @Override // com.google.android.gms.common.api.internal.ConnectionCallbacks
        public void onConnected(Bundle bundle) {
            String str = Util.T;
            CastService castService = CastService.this;
            boolean z = castService.mWaitingForReconnect;
            castService.attachMediaPlayer();
            GoogleApiClient googleApiClient = CastService.this.mApiClient;
            if (googleApiClient == null || !googleApiClient.isConnected()) {
                return;
            }
            try {
                CastService castService2 = CastService.this;
                castService2.mCastClient.joinApplication(castService2.mApiClient).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() { // from class: com.connectsdk.service.CastService.ConnectionCallbacks.1
                    @Override // com.google.android.gms.common.api.ResultCallback
                    public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
                        ConnectionCallbacks.this.onJoinApplicationResult(applicationConnectionResult);
                    }
                });
            } catch (CastClientException unused) {
                String str2 = Util.T;
            }
        }

        @Override // com.google.android.gms.common.api.internal.ConnectionCallbacks
        public void onConnectionSuspended(int i) {
            String str = Util.T;
            CastService castService = CastService.this;
            castService.mWaitingForReconnect = true;
            castService.detachMediaPlayer();
        }

        private ConnectionCallbacks() {
        }
    }

    public class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        public /* synthetic */ ConnectionFailedListener(CastService castService, int i) {
            this();
        }

        @Override // com.google.android.gms.common.api.internal.OnConnectionFailedListener
        public void onConnectionFailed(final ConnectionResult connectionResult) {
            String str = Util.T;
            (connectionResult != null ? connectionResult : "").toString();
            CastService.this.detachMediaPlayer();
            CastService castService = CastService.this;
            castService.connected = false;
            castService.mWaitingForReconnect = false;
            castService.mApiClient = null;
            Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.CastService.ConnectionFailedListener.1
                @Override // java.lang.Runnable
                public void run() {
                    if (CastService.this.listener != null) {
                        ServiceCommandError serviceCommandError = new ServiceCommandError(connectionResult.getErrorCode(), "Failed to connect to Google Cast device", connectionResult);
                        CastService castService2 = CastService.this;
                        castService2.listener.onConnectionFailure(castService2, serviceCommandError);
                    }
                }
            });
        }

        private ConnectionFailedListener() {
        }
    }

    public interface ConnectionListener {
        void onConnected();
    }

    public interface LaunchWebAppListener {
        void onFailure(ServiceCommandError serviceCommandError);

        void onSuccess(WebAppSession webAppSession);
    }

    public CastService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        this.commandQueue = new CopyOnWriteArraySet<>();
        this.mCastClient = new CastClient();
        int i = 0;
        this.mCastClientListener = new CastListener(this, i);
        this.mConnectionCallbacks = new ConnectionCallbacks(this, i);
        this.mConnectionFailedListener = new ConnectionFailedListener(this, i);
        this.sessions = new HashMap();
        this.subscriptions = new ArrayList();
        this.mWaitingForReconnect = false;
    }

    private void addSubscription(URLServiceSubscription<?> uRLServiceSubscription) {
        this.subscriptions.add(uRLServiceSubscription);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void attachMediaPlayer() {
        if (this.mMediaPlayer != null) {
            return;
        }
        RemoteMediaPlayer remoteMediaPlayerCreateMediaPlayer = createMediaPlayer();
        this.mMediaPlayer = remoteMediaPlayerCreateMediaPlayer;
        remoteMediaPlayerCreateMediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() { // from class: com.connectsdk.service.CastService.6
            @Override // com.google.android.gms.cast.RemoteMediaPlayer.OnStatusUpdatedListener
            public void onStatusUpdated() {
                if (CastService.this.subscriptions.size() > 0) {
                    for (URLServiceSubscription<?> uRLServiceSubscription : CastService.this.subscriptions) {
                        if (uRLServiceSubscription.getTarget().equalsIgnoreCase("PlayState")) {
                            for (int i = 0; i < uRLServiceSubscription.getListeners().size(); i++) {
                                ResponseListener responseListener = (ResponseListener) uRLServiceSubscription.getListeners().get(i);
                                RemoteMediaPlayer remoteMediaPlayer = CastService.this.mMediaPlayer;
                                if (remoteMediaPlayer != null && remoteMediaPlayer.getMediaStatus() != null) {
                                    Util.postSuccess(responseListener, MediaControl.PlayStateStatus.convertPlayerStateToPlayStateStatus(CastService.this.mMediaPlayer.getMediaStatus().getPlayerState()));
                                }
                            }
                        }
                    }
                }
            }
        });
        this.mMediaPlayer.setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() { // from class: com.connectsdk.service.CastService.7
            @Override // com.google.android.gms.cast.RemoteMediaPlayer.OnMetadataUpdatedListener
            public void onMetadataUpdated() {
                if (CastService.this.subscriptions.size() > 0) {
                    for (URLServiceSubscription<?> uRLServiceSubscription : CastService.this.subscriptions) {
                        if (uRLServiceSubscription.getTarget().equalsIgnoreCase("info")) {
                            for (int i = 0; i < uRLServiceSubscription.getListeners().size(); i++) {
                                CastService.this.getMediaInfo((MediaPlayer.MediaInfoListener) uRLServiceSubscription.getListeners().get(i));
                            }
                        }
                    }
                }
            }
        });
        GoogleApiClient googleApiClient = this.mApiClient;
        if (googleApiClient != null) {
            try {
                this.mCastClient.setMessageReceivedCallbacks(googleApiClient, this.mMediaPlayer.getNamespace(), this.mMediaPlayer);
            } catch (Exception unused) {
                String str = Util.T;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void detachMediaPlayer() {
        GoogleApiClient googleApiClient;
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer != null && (googleApiClient = this.mApiClient) != null) {
            try {
                this.mCastClient.removeMessageReceivedCallbacks(googleApiClient, remoteMediaPlayer.getNamespace());
            } catch (CastClientException unused) {
                String str = Util.T;
            }
        }
        this.mMediaPlayer = null;
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter("Chromecast", "Chromecast");
    }

    public static String getApplicationID() {
        return applicationID;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadMedia(MediaInfo mediaInfo, final WebAppSession webAppSession, final MediaPlayer.LaunchListener launchListener) {
        try {
            this.mMediaPlayer.load(this.mApiClient, mediaInfo, true).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() { // from class: com.connectsdk.service.CastService.10
                @Override // com.google.android.gms.common.api.ResultCallback
                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (!status.isSuccess()) {
                        Util.postError(launchListener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                        return;
                    }
                    webAppSession.launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
                    CastService castService = CastService.this;
                    castService.mMediaPlayer.setActiveMediaTracks(castService.mApiClient, new long[]{1});
                    Util.postSuccess(launchListener, new MediaPlayer.MediaLaunchObject(webAppSession.launchSession, CastService.this));
                }
            });
        } catch (Exception unused) {
            Util.postError(launchListener, new ServiceCommandError(0, "Unable to load", null));
        }
    }

    private void playMedia(String str, SubtitleInfo subtitleInfo, String str2, String str3, String str4, String str5, boolean z, MediaPlayer.LaunchListener launchListener) {
        JSONObject jSONObject;
        MediaMetadata mediaMetadata = new MediaMetadata(1);
        mediaMetadata.putString("com.google.android.gms.cast.metadata.TITLE", str3);
        mediaMetadata.putString("com.google.android.gms.cast.metadata.SUBTITLE", str4);
        jSONObject = new JSONObject();
        try {
            jSONObject.put("streamType", "cast");
        } catch (Exception unused) {
        }
        if (str5 != null) {
            mediaMetadata.addImage(new WebImage(Uri.parse(str5), 100, 100));
        }
        ArrayList arrayList = new ArrayList();
        if (subtitleInfo != null) {
            arrayList.add(new MediaTrack.Builder(1L, 1).setName(subtitleInfo.getLabel()).setSubtype(1).setContentId(subtitleInfo.getUrl()).setContentType(subtitleInfo.getMimeType()).setLanguage(subtitleInfo.getLanguage()).build());
        }
        playMedia(new MediaInfo.Builder(str).setContentType(str2).setStreamType(1).setMetadata(mediaMetadata).setStreamDuration(1000L).setCustomData(jSONObject).setMediaTracks(arrayList).build(), applicationID, launchListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void runCommand(ConnectionListener connectionListener) {
        GoogleApiClient googleApiClient = this.mApiClient;
        if (googleApiClient != null && googleApiClient.isConnected()) {
            connectionListener.onConnected();
        } else {
            connect();
            this.commandQueue.add(connectionListener);
        }
    }

    public static void setApplicationID(String str) {
        applicationID = str;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void closeMedia(final LaunchSession launchSession, final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.11
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.stopApplication(castService.mApiClient, launchSession.getSessionId()).setResultCallback(new ResultCallback<Status>() { // from class: com.connectsdk.service.CastService.11.1
                        @Override // com.google.android.gms.common.api.ResultCallback
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Util.postSuccess(responseListener, status);
                            } else {
                                Util.postError(responseListener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                            }
                        }
                    });
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to stop", null));
                }
            }
        });
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void closeWebApp(LaunchSession launchSession, final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.18
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.stopApplication(castService.mApiClient).setResultCallback(new ResultCallback<Status>() { // from class: com.connectsdk.service.CastService.18.1
                        @Override // com.google.android.gms.common.api.ResultCallback
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Util.postSuccess(responseListener, null);
                            } else {
                                Util.postError(responseListener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                            }
                        }
                    });
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to stop", null));
                }
            }
        });
    }

    @Override // com.connectsdk.service.DeviceService
    public void connect() {
        if (this.castDevice == null) {
            this.castDevice = (CastDevice) getServiceDescription().getDevice();
        }
        if (this.mApiClient == null) {
            this.mApiClient = createApiClient();
        }
        if (this.mApiClient.isConnecting() || this.mApiClient.isConnected()) {
            return;
        }
        this.mApiClient.connect();
    }

    public GoogleApiClient createApiClient() {
        return new GoogleApiClient.Builder(DiscoveryManager.getInstance().getContext()).addApi(Cast.API, Cast.CastOptions.builder(this.castDevice, this.mCastClientListener).build()).addConnectionCallbacks(this.mConnectionCallbacks).addOnConnectionFailedListener(this.mConnectionFailedListener).build();
    }

    public RemoteMediaPlayer createMediaPlayer() {
        return new RemoteMediaPlayer();
    }

    @Override // com.connectsdk.service.DeviceService
    public void disconnect() {
        this.mWaitingForReconnect = false;
        detachMediaPlayer();
        if (!this.commandQueue.isEmpty()) {
            this.commandQueue.clear();
        }
        GoogleApiClient googleApiClient = this.mApiClient;
        if (googleApiClient != null && googleApiClient.isConnected()) {
            try {
                this.mCastClient.leaveApplication(this.mApiClient);
            } catch (CastClientException unused) {
                String str = Util.T;
            }
            this.mApiClient.disconnect();
        }
        if (this.connected) {
            Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.CastService.1
                @Override // java.lang.Runnable
                public void run() {
                    if (CastService.this.getListener() != null) {
                        CastService.this.getListener().onDisconnect(CastService.this, null);
                    }
                }
            });
        }
        this.connected = false;
        this.mApiClient = null;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void displayImage(String str, String str2, String str3, String str4, String str5, MediaPlayer.LaunchListener launchListener) {
        JSONObject jSONObject;
        MediaMetadata mediaMetadata = new MediaMetadata(4);
        mediaMetadata.putString("com.google.android.gms.cast.metadata.TITLE", str3);
        mediaMetadata.putString("com.google.android.gms.cast.metadata.SUBTITLE", str4);
        jSONObject = new JSONObject();
        try {
            jSONObject.put("streamType", "cast");
        } catch (Exception unused) {
        }
        if (str5 != null) {
            mediaMetadata.addImage(new WebImage(Uri.parse(str5), 100, 100));
        }
        playMedia(new MediaInfo.Builder(str).setContentType(str2).setStreamType(0).setMetadata(mediaMetadata).setStreamDuration(0L).setCustomData(jSONObject).build(), applicationID, launchListener);
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void fastForward(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    public GoogleApiClient getApiClient() {
        return this.mApiClient;
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void getDuration(MediaControl.DurationListener durationListener) {
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer == null || remoteMediaPlayer.getMediaStatus() == null) {
            Util.postError(durationListener, new ServiceCommandError(0, "There is no media currently available", null));
        } else {
            Util.postSuccess(durationListener, Long.valueOf(this.mMediaPlayer.getStreamDuration()));
        }
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public MediaControl getMediaControl() {
        return this;
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public CapabilityMethods.CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void getMediaInfo(MediaPlayer.MediaInfoListener mediaInfoListener) {
        String str;
        String str2;
        ArrayList arrayList;
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer == null) {
            return;
        }
        ArrayList arrayList2 = null;
        if (remoteMediaPlayer.getMediaInfo() == null) {
            Util.postError(mediaInfoListener, new ServiceCommandError(0, "Media Info is null", null));
            return;
        }
        String contentId = this.mMediaPlayer.getMediaInfo().getContentId();
        String contentType = this.mMediaPlayer.getMediaInfo().getContentType();
        MediaMetadata metadata = this.mMediaPlayer.getMediaInfo().getMetadata();
        if (metadata != null) {
            String string = metadata.getString("com.google.android.gms.cast.metadata.TITLE");
            String string2 = metadata.getString("com.google.android.gms.cast.metadata.SUBTITLE");
            if (metadata.getImages() != null && metadata.getImages().size() > 0) {
                String string3 = metadata.getImages().get(0).getUrl().toString();
                arrayList2 = new ArrayList();
                arrayList2.add(new ImageInfo(string3));
            }
            arrayList = arrayList2;
            str2 = string2;
            str = string;
        } else {
            str = null;
            str2 = null;
            arrayList = null;
        }
        Util.postSuccess(mediaInfoListener, new com.connectsdk.core.MediaInfo(contentId, contentType, str, str2, arrayList));
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public CapabilityMethods.CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void getMute(VolumeControl.MuteListener muteListener) {
        Util.postSuccess(muteListener, Boolean.valueOf(this.currentMuteStatus));
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void getPlayState(MediaControl.PlayStateListener playStateListener) {
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer == null || remoteMediaPlayer.getMediaStatus() == null) {
            Util.postError(playStateListener, new ServiceCommandError(0, "There is no media currently available", null));
        } else {
            Util.postSuccess(playStateListener, MediaControl.PlayStateStatus.convertPlayerStateToPlayStateStatus(this.mMediaPlayer.getMediaStatus().getPlayerState()));
        }
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void getPosition(MediaControl.PositionListener positionListener) {
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer == null || remoteMediaPlayer.getMediaStatus() == null) {
            Util.postError(positionListener, new ServiceCommandError(0, "There is no media currently available", null));
        } else {
            Util.postSuccess(positionListener, Long.valueOf(this.mMediaPlayer.getApproximateStreamPosition()));
        }
    }

    @Override // com.connectsdk.service.DeviceService
    public CapabilityMethods.CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> cls) {
        if (cls.equals(MediaPlayer.class)) {
            return getMediaPlayerCapabilityLevel();
        }
        if (cls.equals(MediaControl.class)) {
            return getMediaControlCapabilityLevel();
        }
        if (cls.equals(VolumeControl.class)) {
            return getVolumeControlCapabilityLevel();
        }
        return cls.equals(WebAppLauncher.class) ? getWebAppLauncherCapabilityLevel() : CapabilityMethods.CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override // com.connectsdk.service.DeviceService
    public String getServiceName() {
        return "Chromecast";
    }

    public List<URLServiceSubscription<?>> getSubscriptions() {
        return this.subscriptions;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void getVolume(VolumeControl.VolumeListener volumeListener) {
        Util.postSuccess(volumeListener, Float.valueOf(this.currentVolumeLevel));
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public VolumeControl getVolumeControl() {
        return this;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public CapabilityMethods.CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public WebAppLauncher getWebAppLauncher() {
        return this;
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public CapabilityMethods.CapabilityPriorityLevel getWebAppLauncherCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.DeviceService
    public boolean isConnectable() {
        return true;
    }

    @Override // com.connectsdk.service.DeviceService
    public boolean isConnected() {
        return this.connected;
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void isWebAppPinned(String str, WebAppSession.WebAppPinStatusListener webAppPinStatusListener) {
        Util.postError(webAppPinStatusListener, ServiceCommandError.notSupported());
    }

    public void joinApplication(ResponseListener<Object> responseListener) {
        runCommand(new AnonymousClass15(responseListener));
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void joinWebApp(final LaunchSession launchSession, final WebAppSession.LaunchListener launchListener) {
        final ApplicationConnectionResultCallback applicationConnectionResultCallback = new ApplicationConnectionResultCallback(new AnonymousClass16(launchListener));
        this.launchingAppId = launchSession.getAppId();
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.17
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.joinApplication(castService.mApiClient, launchSession.getAppId()).setResultCallback(applicationConnectionResultCallback);
                } catch (Exception unused) {
                    Util.postError(launchListener, new ServiceCommandError(0, "Unable to join", null));
                }
            }
        });
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void launchWebApp(String str, WebAppSession.LaunchListener launchListener) {
        launchWebApp(str, true, launchListener);
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void next(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void pause(final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.3
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mMediaPlayer.pause(castService.mApiClient);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to pause", null));
                }
            }
        });
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void pinWebApp(String str, ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void play(final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.2
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mMediaPlayer.play(castService.mApiClient);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to play", null));
                }
            }
        });
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void previous(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    public void requestStatus(final ResponseListener<Object> responseListener) {
        try {
            this.mMediaPlayer.requestStatus(this.mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() { // from class: com.connectsdk.service.CastService.14
                @Override // com.google.android.gms.common.api.ResultCallback
                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    if (mediaChannelResult.getStatus().isSuccess()) {
                        Util.postSuccess(responseListener, mediaChannelResult);
                    } else {
                        Util.postError(responseListener, new ServiceCommandError(0, "Failed to request status", mediaChannelResult));
                    }
                }
            });
        } catch (Exception unused) {
            Util.postError(responseListener, new ServiceCommandError(0, "There is no media currently available", null));
        }
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void rewind(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void seek(final long j, final ResponseListener<Object> responseListener) {
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer == null || remoteMediaPlayer.getMediaStatus() == null) {
            Util.postError(responseListener, new ServiceCommandError(0, "There is no media currently available", null));
        } else {
            runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.5
                @Override // com.connectsdk.service.CastService.ConnectionListener
                public void onConnected() {
                    try {
                        CastService castService = CastService.this;
                        castService.mMediaPlayer.seek(castService.mApiClient, j, 0).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() { // from class: com.connectsdk.service.CastService.5.1
                            @Override // com.google.android.gms.common.api.ResultCallback
                            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                                Status status = mediaChannelResult.getStatus();
                                if (status.isSuccess()) {
                                    Util.postSuccess(responseListener, null);
                                } else {
                                    Util.postError(responseListener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                                }
                            }
                        });
                    } catch (Exception unused) {
                        Util.postError(responseListener, new ServiceCommandError(0, "Unable to seek", null));
                    }
                }
            });
        }
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void setMute(final boolean z, final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.22
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.setMute(castService.mApiClient, z);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "setting mute status failed", null));
                }
            }
        });
    }

    public void setSubscriptions(List<URLServiceSubscription<?>> list) {
        this.subscriptions = list;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void setVolume(final float f, final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.21
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.setVolume(castService.mApiClient, f);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "setting volume level failed", null));
                }
            }
        });
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void stop(final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.4
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mMediaPlayer.stop(castService.mApiClient);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to stop", null));
                }
            }
        });
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public ServiceSubscription<WebAppSession.WebAppPinStatusListener> subscribeIsWebAppPinned(String str, WebAppSession.WebAppPinStatusListener webAppPinStatusListener) {
        Util.postError(webAppPinStatusListener, ServiceCommandError.notSupported());
        return null;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public ServiceSubscription<MediaPlayer.MediaInfoListener> subscribeMediaInfo(MediaPlayer.MediaInfoListener mediaInfoListener) {
        URLServiceSubscription uRLServiceSubscription = new URLServiceSubscription<>(this, "info", null, null);
        uRLServiceSubscription.addListener(mediaInfoListener);
        addSubscription(uRLServiceSubscription);
        return uRLServiceSubscription;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public ServiceSubscription<VolumeControl.MuteListener> subscribeMute(VolumeControl.MuteListener muteListener) {
        URLServiceSubscription uRLServiceSubscription = new URLServiceSubscription<>(this, "mute", null, null);
        uRLServiceSubscription.addListener(muteListener);
        addSubscription(uRLServiceSubscription);
        return uRLServiceSubscription;
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public ServiceSubscription<MediaControl.PlayStateListener> subscribePlayState(MediaControl.PlayStateListener playStateListener) {
        URLServiceSubscription uRLServiceSubscription = new URLServiceSubscription<>(this, "PlayState", null, null);
        uRLServiceSubscription.addListener(playStateListener);
        addSubscription(uRLServiceSubscription);
        return uRLServiceSubscription;
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public ServiceSubscription<VolumeControl.VolumeListener> subscribeVolume(VolumeControl.VolumeListener volumeListener) {
        URLServiceSubscription uRLServiceSubscription = new URLServiceSubscription<>(this, "volume", null, null);
        uRLServiceSubscription.addListener(volumeListener);
        addSubscription(uRLServiceSubscription);
        return uRLServiceSubscription;
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void unPinWebApp(String str, ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.DeviceService, com.connectsdk.service.command.ServiceCommand.ServiceCommandProcessor
    public void unsubscribe(URLServiceSubscription<?> uRLServiceSubscription) {
        this.subscriptions.remove(uRLServiceSubscription);
    }

    @Override // com.connectsdk.service.DeviceService
    public void updateCapabilities() {
        ArrayList arrayList = new ArrayList();
        Collections.addAll(arrayList, MediaPlayer.Capabilities);
        arrayList.add("MediaPlayer.Subtitle.WebVTT");
        Collections.addAll(arrayList, VolumeControl.Capabilities);
        java.util.Collections.addAll(arrayList, "MediaControl.Play", "MediaControl.Pause", "MediaControl.Stop", "MediaControl.Duration");
        java.util.Collections.addAll(arrayList, "MediaControl.Seek", "MediaControl.Position", "MediaControl.PlayState", "MediaControl.PlayState.Subscribe");
        java.util.Collections.addAll(arrayList, "WebAppLauncher.Launch", "WebAppLauncher.Message.Send", "WebAppLauncher.Message.Receive", "WebAppLauncher.Message.Send.JSON");
        java.util.Collections.addAll(arrayList, "WebAppLauncher.Message.Receive.JSON", "WebAppLauncher.Connect", "WebAppLauncher.Disconnect", "WebAppLauncher.Join");
        arrayList.add("WebAppLauncher.Close");
        setCapabilities(arrayList);
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void volumeDown(final ResponseListener<Object> responseListener) {
        getVolume(new VolumeControl.VolumeListener() { // from class: com.connectsdk.service.CastService.20
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(responseListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Float f) {
                if (f.floatValue() <= 0.0d) {
                    Util.postSuccess(responseListener, null);
                    return;
                }
                float fFloatValue = (float) (((double) f.floatValue()) - 0.01d);
                if (fFloatValue < 0.0d) {
                    fFloatValue = 0.0f;
                }
                CastService.this.setVolume(fFloatValue, responseListener);
                Util.postSuccess(responseListener, null);
            }
        });
    }

    @Override // com.connectsdk.service.capability.VolumeControl
    public void volumeUp(final ResponseListener<Object> responseListener) {
        getVolume(new VolumeControl.VolumeListener() { // from class: com.connectsdk.service.CastService.19
            @Override // com.connectsdk.service.capability.listeners.ErrorListener
            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(responseListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.capability.listeners.ResponseListener
            public void onSuccess(Float f) {
                if (f.floatValue() >= 1.0d) {
                    Util.postSuccess(responseListener, null);
                    return;
                }
                float fFloatValue = (float) (((double) f.floatValue()) + 0.01d);
                if (fFloatValue > 1.0d) {
                    fFloatValue = 1.0f;
                }
                CastService.this.setVolume(fFloatValue, responseListener);
                Util.postSuccess(responseListener, null);
            }
        });
    }

    public static class CastClient {
        private CastClientException createCastClientException(Exception exc) {
            return new CastClientException("CastClient error", exc);
        }

        public ApplicationMetadata getApplicationMetadata(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.getApplicationMetadata(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public Object getApplicationStatus(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.getApplicationStatus(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public double getVolume(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.getVolume(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public boolean isMute(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.isMute(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.joinApplication(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public PendingResult<Cast.ApplicationConnectionResult> launchApplication(GoogleApiClient googleApiClient, String str, LaunchOptions launchOptions) throws CastClientException {
            try {
                return Cast.CastApi.launchApplication(googleApiClient, str, launchOptions);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public void leaveApplication(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                Cast.CastApi.leaveApplication(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public void removeMessageReceivedCallbacks(GoogleApiClient googleApiClient, String str) throws CastClientException {
            try {
                Cast.CastApi.removeMessageReceivedCallbacks(googleApiClient, str);
            } catch (IOException e2) {
                throw createCastClientException(e2);
            } catch (RuntimeException e3) {
                throw createCastClientException(e3);
            }
        }

        public void setMessageReceivedCallbacks(GoogleApiClient googleApiClient, String str, RemoteMediaPlayer remoteMediaPlayer) throws CastClientException {
            try {
                Cast.CastApi.setMessageReceivedCallbacks(googleApiClient, remoteMediaPlayer.getNamespace(), remoteMediaPlayer);
            } catch (IOException e2) {
                throw createCastClientException(e2);
            } catch (RuntimeException e3) {
                throw createCastClientException(e3);
            }
        }

        public void setMute(GoogleApiClient googleApiClient, boolean z) throws CastClientException, IOException {
            try {
                Cast.CastApi.setMute(googleApiClient, z);
            } catch (IOException e2) {
                throw createCastClientException(e2);
            } catch (RuntimeException e3) {
                throw createCastClientException(e3);
            }
        }

        public void setVolume(GoogleApiClient googleApiClient, float f) throws CastClientException, IOException {
            try {
                Cast.CastApi.setVolume(googleApiClient, f);
            } catch (IOException e2) {
                throw createCastClientException(e2);
            } catch (RuntimeException e3) {
                throw createCastClientException(e3);
            }
        }

        public PendingResult<Status> stopApplication(GoogleApiClient googleApiClient, String str) throws CastClientException {
            try {
                return Cast.CastApi.stopApplication(googleApiClient, str);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient googleApiClient, String str) throws CastClientException {
            try {
                return Cast.CastApi.joinApplication(googleApiClient, str);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public PendingResult<Status> stopApplication(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.stopApplication(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void launchWebApp(final String str, final boolean z, final WebAppSession.LaunchListener launchListener) {
        this.launchingAppId = str;
        final LaunchWebAppListener launchWebAppListener = new LaunchWebAppListener() { // from class: com.connectsdk.service.CastService.12
            @Override // com.connectsdk.service.CastService.LaunchWebAppListener
            public void onFailure(ServiceCommandError serviceCommandError) {
                Util.postError(launchListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.CastService.LaunchWebAppListener
            public void onSuccess(WebAppSession webAppSession) {
                Util.postSuccess(launchListener, webAppSession);
            }
        };
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.13
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    if (z) {
                        LaunchOptions launchOptions = new LaunchOptions();
                        launchOptions.setRelaunchIfRunning(z);
                        CastService castService = CastService.this;
                        castService.mCastClient.launchApplication(castService.mApiClient, str, launchOptions).setResultCallback(CastService.this.new ApplicationConnectionResultCallback(launchWebAppListener));
                    } else {
                        CastService castService2 = CastService.this;
                        castService2.mCastClient.joinApplication(castService2.mApiClient).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() { // from class: com.connectsdk.service.CastService.13.1
                            @Override // com.google.android.gms.common.api.ResultCallback
                            public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
                                if (!applicationConnectionResult.getStatus().isSuccess() || applicationConnectionResult.getApplicationMetadata() == null || applicationConnectionResult.getApplicationMetadata().getName() == null || !applicationConnectionResult.getApplicationMetadata().getApplicationId().equals(str)) {
                                    LaunchOptions launchOptions2 = new LaunchOptions();
                                    launchOptions2.setRelaunchIfRunning(true);
                                    try {
                                        CastService castService3 = CastService.this;
                                        PendingResult<Cast.ApplicationConnectionResult> pendingResultLaunchApplication = castService3.mCastClient.launchApplication(castService3.mApiClient, str, launchOptions2);
                                        pendingResultLaunchApplication.setResultCallback(CastService.this.new ApplicationConnectionResultCallback(launchWebAppListener));
                                        return;
                                    } catch (Exception unused) {
                                        Util.postError(launchListener, new ServiceCommandError(0, "Unable to launch", null));
                                        return;
                                    }
                                }
                                ApplicationMetadata applicationMetadata = applicationConnectionResult.getApplicationMetadata();
                                CastService.this.currentAppId = applicationMetadata.getApplicationId();
                                LaunchSession launchSessionLaunchSessionForAppId = LaunchSession.launchSessionForAppId(applicationMetadata.getApplicationId());
                                launchSessionLaunchSessionForAppId.setAppName(applicationMetadata.getName());
                                launchSessionLaunchSessionForAppId.setSessionId(applicationConnectionResult.getSessionId());
                                launchSessionLaunchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.WebApp);
                                launchSessionLaunchSessionForAppId.setService(CastService.this);
                                CastWebAppSession castWebAppSession = new CastWebAppSession(launchSessionLaunchSessionForAppId, CastService.this);
                                castWebAppSession.setMetadata(applicationMetadata);
                                CastService.this.sessions.put(applicationMetadata.getApplicationId(), castWebAppSession);
                                Util.postSuccess(launchListener, castWebAppSession);
                            }
                        });
                    }
                } catch (Exception unused) {
                    Util.postError(launchListener, new ServiceCommandError(0, "Unable to launch", null));
                }
            }
        });
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void joinWebApp(String str, WebAppSession.LaunchListener launchListener) {
        LaunchSession launchSessionLaunchSessionForAppId = LaunchSession.launchSessionForAppId(str);
        launchSessionLaunchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.WebApp);
        launchSessionLaunchSessionForAppId.setService(this);
        joinWebApp(launchSessionLaunchSessionForAppId, launchListener);
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void launchWebApp(String str, JSONObject jSONObject, WebAppSession.LaunchListener launchListener) {
        Util.postError(launchListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.WebAppLauncher
    public void launchWebApp(String str, JSONObject jSONObject, boolean z, WebAppSession.LaunchListener launchListener) {
        Util.postError(launchListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void displayImage(com.connectsdk.core.MediaInfo mediaInfo, MediaPlayer.LaunchListener launchListener) {
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

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void playMedia(String str, String str2, String str3, String str4, String str5, boolean z, MediaPlayer.LaunchListener launchListener) {
        playMedia(str, null, str2, str3, str4, str5, z, launchListener);
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void playMedia(com.connectsdk.core.MediaInfo mediaInfo, boolean z, MediaPlayer.LaunchListener launchListener) {
        String str;
        SubtitleInfo subtitleInfo;
        String str2;
        String str3;
        String str4;
        String str5;
        try {
            this.mCastClient.getApplicationStatus(this.mApiClient);
        } catch (CastClientException e2) {
            Util.postError(launchListener, new ServiceCommandError(e2.getMessage()));
        }
        String url = null;
        if (mediaInfo != null) {
            String url2 = mediaInfo.getUrl();
            SubtitleInfo subtitleInfo2 = mediaInfo.getSubtitleInfo();
            String mimeType = mediaInfo.getMimeType();
            String title = mediaInfo.getTitle();
            String description = mediaInfo.getDescription();
            if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
                url = mediaInfo.getImages().get(0).getUrl();
            }
            str5 = url;
            str4 = description;
            str3 = title;
            str2 = mimeType;
            subtitleInfo = subtitleInfo2;
            str = url2;
        } else {
            str = null;
            subtitleInfo = null;
            str2 = null;
            str3 = null;
            str4 = null;
            str5 = null;
        }
        playMedia(str, subtitleInfo, str2, str3, str4, str5, z, launchListener);
    }

    private void playMedia(final MediaInfo mediaInfo, final String str, final MediaPlayer.LaunchListener launchListener) {
        final ApplicationConnectionResultCallback applicationConnectionResultCallback = new ApplicationConnectionResultCallback(new LaunchWebAppListener() { // from class: com.connectsdk.service.CastService.8
            @Override // com.connectsdk.service.CastService.LaunchWebAppListener
            public void onFailure(ServiceCommandError serviceCommandError) {
                Util.postError(launchListener, serviceCommandError);
            }

            @Override // com.connectsdk.service.CastService.LaunchWebAppListener
            public void onSuccess(final WebAppSession webAppSession) {
                CastService.this.runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.8.1
                    @Override // com.connectsdk.service.CastService.ConnectionListener
                    public void onConnected() {
                        CastService.this.loadMedia(mediaInfo, webAppSession, launchListener);
                    }
                });
            }
        });
        this.launchingAppId = str;
        runCommand(new ConnectionListener() { // from class: com.connectsdk.service.CastService.9
            @Override // com.connectsdk.service.CastService.ConnectionListener
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    boolean z = castService.mCastClient.getApplicationStatus(castService.mApiClient) == null || !str.equals(CastService.this.currentAppId);
                    LaunchOptions launchOptions = new LaunchOptions();
                    launchOptions.setRelaunchIfRunning(z);
                    CastService castService2 = CastService.this;
                    castService2.mCastClient.launchApplication(castService2.mApiClient, str, launchOptions).setResultCallback(applicationConnectionResultCallback);
                } catch (Exception unused) {
                    Util.postError(launchListener, new ServiceCommandError(0, "Unable to launch", null));
                }
            }
        });
    }
}

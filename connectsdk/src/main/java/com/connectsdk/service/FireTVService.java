package com.connectsdk.service;

import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.amazon.whisperplay.fling.media.service.CustomMediaPlayer;
import com.amazon.whisperplay.fling.media.service.MediaPlayerInfo;
import com.amazon.whisperplay.fling.media.service.MediaPlayerStatus;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.FireTVServiceError;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.LaunchSession;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class FireTVService extends DeviceService implements MediaPlayer, MediaControl {
    public static final String ID = "FireTV";
    private static final String META_DESCRIPTION = "description";
    private static final String META_ICON_IMAGE = "poster";
    private static final String META_KIND = "kind";
    private static final String META_LABEL = "label";
    private static final String META_MIME_TYPE = "type";
    private static final String META_NOREPLAY = "noreplay";
    private static final String META_SRC = "src";
    private static final String META_SRCLANG = "srclang";
    private static final String META_TITLE = "title";
    private static final String META_TRACKS = "tracks";
    DIALService dialService;
    private PlayStateSubscription playStateSubscription;
    private final RemoteMediaPlayer remoteMediaPlayer;

    /* JADX INFO: renamed from: com.connectsdk.service.FireTVService$7, reason: invalid class name */
    public static /* synthetic */ class AnonymousClass7 {
        static final /* synthetic */ int[] $SwitchMap$com$amazon$whisperplay$fling$media$service$MediaPlayerStatus$MediaState;

        static {
            int[] iArr = new int[MediaPlayerStatus.MediaState.values().length];
            $SwitchMap$com$amazon$whisperplay$fling$media$service$MediaPlayerStatus$MediaState = iArr;
            try {
                iArr[MediaPlayerStatus.MediaState.PreparingMedia.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$amazon$whisperplay$fling$media$service$MediaPlayerStatus$MediaState[MediaPlayerStatus.MediaState.Playing.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$amazon$whisperplay$fling$media$service$MediaPlayerStatus$MediaState[MediaPlayerStatus.MediaState.Paused.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$com$amazon$whisperplay$fling$media$service$MediaPlayerStatus$MediaState[MediaPlayerStatus.MediaState.Finished.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                $SwitchMap$com$amazon$whisperplay$fling$media$service$MediaPlayerStatus$MediaState[MediaPlayerStatus.MediaState.NoSource.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
        }
    }

    public interface ConvertResult<Response, Result> {
        Response convert(Result result) throws Exception;
    }

    public class PlayStateSubscription extends Subscription<MediaControl.PlayStateStatus, MediaControl.PlayStateListener> implements CustomMediaPlayer.StatusListener {
        public PlayStateSubscription(MediaControl.PlayStateListener playStateListener) {
            super(playStateListener);
        }

        @Override // com.amazon.whisperplay.fling.media.service.CustomMediaPlayer.StatusListener
        public void onStatusChange(MediaPlayerStatus mediaPlayerStatus, long j) {
            notifyListeners(FireTVService.this.createPlayStateStatusFromFireTVStatus(mediaPlayerStatus));
        }

        @Override // com.connectsdk.service.command.ServiceSubscription
        public void unsubscribe() {
            FireTVService.this.remoteMediaPlayer.removeStatusListener(this);
            FireTVService.this.playStateSubscription = null;
        }
    }

    public static abstract class Subscription<Status, Listener extends ResponseListener<Status>> implements ServiceSubscription<Listener> {
        List<Listener> listeners;
        Status prevStatus;

        public Subscription(Listener listener) {
            ArrayList arrayList = new ArrayList();
            this.listeners = arrayList;
            if (listener != null) {
                arrayList.add(listener);
            }
        }

        @Override // com.connectsdk.service.command.ServiceSubscription
        public List<Listener> getListeners() {
            return this.listeners;
        }

        public synchronized void notifyListeners(final Status status) {
            if (!status.equals(this.prevStatus)) {
                Util.runOnUI(new Runnable() { // from class: com.connectsdk.service.FireTVService.Subscription.1
                    @Override // java.lang.Runnable
                    public void run() {
                        Iterator<Listener> it = Subscription.this.listeners.iterator();
                        while (it.hasNext()) {
                            it.next().onSuccess(status);
                        }
                    }
                });
                this.prevStatus = status;
            }
        }

        @Override // com.connectsdk.service.command.ServiceSubscription
        public Listener addListener(Listener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
            return listener;
        }

        @Override // com.connectsdk.service.command.ServiceSubscription
        public void removeListener(Listener listener) {
            this.listeners.remove(listener);
        }
    }

    public FireTVService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        if (serviceDescription == null || !(serviceDescription.getDevice() instanceof RemoteMediaPlayer)) {
            this.remoteMediaPlayer = null;
        } else {
            this.remoteMediaPlayer = (RemoteMediaPlayer) serviceDescription.getDevice();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public MediaPlayer.MediaLaunchObject createMediaLaunchObject() {
        LaunchSession launchSession = new LaunchSession();
        launchSession.setService(this);
        launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
        launchSession.setAppId(this.remoteMediaPlayer.getUniqueIdentifier());
        launchSession.setAppName(this.remoteMediaPlayer.getName());
        return new MediaPlayer.MediaLaunchObject(launchSession, this);
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter("FireTV", "FireTV");
    }

    private String getMetadata(MediaInfo mediaInfo) throws JSONException {
        ImageInfo imageInfo;
        JSONObject jSONObject = new JSONObject();
        if (mediaInfo.getTitle() != null && !mediaInfo.getTitle().isEmpty()) {
            jSONObject.put("title", mediaInfo.getTitle());
        }
        if (mediaInfo.getDescription() != null && !mediaInfo.getDescription().isEmpty()) {
            jSONObject.put("description", mediaInfo.getDescription());
        }
        jSONObject.put("type", mediaInfo.getMimeType());
        if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0 && (imageInfo = mediaInfo.getImages().get(0)) != null && imageInfo.getUrl() != null && !imageInfo.getUrl().isEmpty()) {
            jSONObject.put("poster", imageInfo.getUrl());
        }
        jSONObject.put("noreplay", true);
        if (mediaInfo.getSubtitleInfo() != null) {
            JSONArray jSONArray = new JSONArray();
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("kind", "subtitles");
            jSONObject2.put("src", mediaInfo.getSubtitleInfo().getUrl());
            String label = mediaInfo.getSubtitleInfo().getLabel();
            if (label == null) {
                label = "";
            }
            jSONObject2.put("label", label);
            String language = mediaInfo.getSubtitleInfo().getLanguage();
            jSONObject2.put("srclang", language != null ? language : "");
            jSONArray.put(jSONObject2);
            jSONObject.put("tracks", jSONArray);
        }
        return jSONObject.toString();
    }

    private <T> void handleAsyncFuture(ResponseListener<T> responseListener, RemoteMediaPlayer.AsyncFuture<T> asyncFuture, String str) {
        handleAsyncFutureWithConversion(responseListener, asyncFuture, new ConvertResult<T, T>() { // from class: com.connectsdk.service.FireTVService.5
            @Override // com.connectsdk.service.FireTVService.ConvertResult
            public T convert(T t) {
                return t;
            }
        }, str);
    }

    private <Response, Result> void handleAsyncFutureWithConversion(final ResponseListener<Response> responseListener, RemoteMediaPlayer.AsyncFuture<Result> asyncFuture, final ConvertResult<Response, Result> convertResult, final String str) {
        if (asyncFuture != null) {
            asyncFuture.getAsync(new RemoteMediaPlayer.FutureListener<Result>() { // from class: com.connectsdk.service.FireTVService.6
                @Override // com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer.FutureListener
                public void futureIsNow(Future<Result> future) {
                    try {
                        Util.postSuccess(responseListener, convertResult.convert(future.get()));
                    } catch (ExecutionException e2) {
                        Util.postError(responseListener, new FireTVServiceError(str, e2.getCause()));
                    } catch (Exception e3) {
                        Util.postError(responseListener, new FireTVServiceError(str, e3));
                    }
                }
            });
        } else {
            Util.postError(responseListener, new FireTVServiceError(str));
        }
    }

    private void handleVoidAsyncFuture(ResponseListener<Object> responseListener, RemoteMediaPlayer.AsyncFuture<Void> asyncFuture, String str) {
        handleAsyncFutureWithConversion(responseListener, asyncFuture, new ConvertResult<Object, Void>() { // from class: com.connectsdk.service.FireTVService.4
            @Override // com.connectsdk.service.FireTVService.ConvertResult
            public Object convert(Void r1) {
                return r1;
            }
        }, str);
    }

    private void setMediaSource(MediaInfo mediaInfo, MediaPlayer.LaunchListener launchListener) {
        try {
            handleAsyncFutureWithConversion(launchListener, this.remoteMediaPlayer.setMediaSource(mediaInfo.getUrl(), getMetadata(mediaInfo), true, false), new ConvertResult<MediaPlayer.MediaLaunchObject, Void>() { // from class: com.connectsdk.service.FireTVService.3
                @Override // com.connectsdk.service.FireTVService.ConvertResult
                public MediaPlayer.MediaLaunchObject convert(Void r1) {
                    return FireTVService.this.createMediaLaunchObject();
                }
            }, "Error setting media source");
        } catch (Exception e2) {
            Util.postError(launchListener, new FireTVServiceError("Error setting media source", e2));
        }
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void closeMedia(LaunchSession launchSession, ResponseListener<Object> responseListener) {
        stop(responseListener);
    }

    @Override // com.connectsdk.service.DeviceService
    public void connect() {
        super.connect();
        if (this.remoteMediaPlayer != null) {
            this.connected = true;
            reportConnected(true);
        }
    }

    public MediaControl.PlayStateStatus createPlayStateStatusFromFireTVStatus(MediaPlayerStatus mediaPlayerStatus) {
        MediaControl.PlayStateStatus playStateStatus = MediaControl.PlayStateStatus.Unknown;
        if (mediaPlayerStatus == null) {
            return playStateStatus;
        }
        int i = AnonymousClass7.$SwitchMap$com$amazon$whisperplay$fling$media$service$MediaPlayerStatus$MediaState[mediaPlayerStatus.getState().ordinal()];
        if (i == 1) {
            return MediaControl.PlayStateStatus.Buffering;
        }
        if (i == 2) {
            return MediaControl.PlayStateStatus.Playing;
        }
        if (i == 3) {
            return MediaControl.PlayStateStatus.Paused;
        }
        if (i != 4) {
            return i != 5 ? playStateStatus : MediaControl.PlayStateStatus.Idle;
        }
        return MediaControl.PlayStateStatus.Finished;
    }

    @Override // com.connectsdk.service.DeviceService
    public void disconnect() {
        super.disconnect();
        PlayStateSubscription playStateSubscription = this.playStateSubscription;
        if (playStateSubscription != null) {
            playStateSubscription.unsubscribe();
            this.playStateSubscription = null;
        }
        this.connected = false;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void displayImage(String str, String str2, String str3, String str4, String str5, MediaPlayer.LaunchListener launchListener) {
        setMediaSource(new MediaInfo.Builder(str, str2).setTitle(str3).setDescription(str4).setIcon(str5).build(), launchListener);
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void fastForward(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    public DIALService getDIALService() {
        ConnectableDevice connectableDevice;
        DIALService dIALService;
        if (this.dialService == null && (connectableDevice = DiscoveryManager.getInstance().getAllDevices().get(this.serviceDescription.getIpAddress())) != null) {
            Iterator<DeviceService> it = connectableDevice.getServices().iterator();
            while (true) {
                if (!it.hasNext()) {
                    dIALService = null;
                    break;
                }
                DeviceService next = it.next();
                if (DIALService.class.isAssignableFrom(next.getClass())) {
                    dIALService = (DIALService) next;
                    break;
                }
            }
            this.dialService = dIALService;
        }
        return this.dialService;
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void getDuration(MediaControl.DurationListener durationListener) {
        try {
            handleAsyncFuture(durationListener, this.remoteMediaPlayer.getDuration(), "Error getting duration");
        } catch (Exception e2) {
            Util.postError(durationListener, new FireTVServiceError("Error getting duration", e2));
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
        try {
            handleAsyncFutureWithConversion(mediaInfoListener, this.remoteMediaPlayer.getMediaInfo(), new ConvertResult<MediaInfo, MediaPlayerInfo>() { // from class: com.connectsdk.service.FireTVService.1
                @Override // com.connectsdk.service.FireTVService.ConvertResult
                public MediaInfo convert(MediaPlayerInfo mediaPlayerInfo) throws JSONException {
                    ArrayList arrayList;
                    JSONObject jSONObject = new JSONObject(mediaPlayerInfo.getMetadata());
                    if (jSONObject.has("poster")) {
                        arrayList = new ArrayList();
                        arrayList.add(new ImageInfo(jSONObject.getString("poster")));
                    } else {
                        arrayList = null;
                    }
                    return new MediaInfo(mediaPlayerInfo.getSource(), jSONObject.getString("type"), jSONObject.getString("title"), jSONObject.getString("description"), arrayList);
                }
            }, "Error getting media info");
        } catch (Exception unused) {
            Util.postError(mediaInfoListener, new FireTVServiceError("Error getting media info"));
        }
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public CapabilityMethods.CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityMethods.CapabilityPriorityLevel.HIGH;
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void getPlayState(MediaControl.PlayStateListener playStateListener) {
        try {
            handleAsyncFutureWithConversion(playStateListener, this.remoteMediaPlayer.getStatus(), new ConvertResult<MediaControl.PlayStateStatus, MediaPlayerStatus>() { // from class: com.connectsdk.service.FireTVService.2
                @Override // com.connectsdk.service.FireTVService.ConvertResult
                public MediaControl.PlayStateStatus convert(MediaPlayerStatus mediaPlayerStatus) {
                    return FireTVService.this.createPlayStateStatusFromFireTVStatus(mediaPlayerStatus);
                }
            }, "Error getting play state");
        } catch (Exception e2) {
            Util.postError(playStateListener, new FireTVServiceError("Error getting play state", e2));
        }
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void getPosition(MediaControl.PositionListener positionListener) {
        try {
            handleAsyncFuture(positionListener, this.remoteMediaPlayer.getPosition(), "Error getting position");
        } catch (Exception e2) {
            Util.postError(positionListener, new FireTVServiceError("Error getting position", e2));
        }
    }

    @Override // com.connectsdk.service.DeviceService
    public CapabilityMethods.CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> cls) {
        if (cls != null) {
            if (cls.equals(MediaPlayer.class)) {
                return getMediaPlayerCapabilityLevel();
            }
            if (cls.equals(MediaControl.class)) {
                return getMediaControlCapabilityLevel();
            }
        }
        return CapabilityMethods.CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    @Override // com.connectsdk.service.DeviceService
    public boolean isConnectable() {
        return true;
    }

    @Override // com.connectsdk.service.DeviceService
    public boolean isConnected() {
        return this.connected;
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void next(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void pause(ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.pause(), "Error pausing");
        } catch (Exception e2) {
            Util.postError(responseListener, new FireTVServiceError("Error pausing", e2));
        }
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void play(ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.play(), "Error playing");
        } catch (Exception e2) {
            Util.postError(responseListener, new FireTVServiceError("Error playing", e2));
        }
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void playMedia(String str, String str2, String str3, String str4, String str5, boolean z, MediaPlayer.LaunchListener launchListener) {
        setMediaSource(new MediaInfo.Builder(str, str2).setTitle(str3).setDescription(str4).setIcon(str5).build(), launchListener);
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void previous(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void rewind(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void seek(long j, ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.seek(CustomMediaPlayer.PlayerSeekMode.Absolute, j), "Error seeking");
        } catch (Exception e2) {
            Util.postError(responseListener, new FireTVServiceError("Error seeking", e2));
        }
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public void stop(ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.stop(), "Error stopping");
        } catch (Exception e2) {
            Util.postError(responseListener, new FireTVServiceError("Error stopping", e2));
        }
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public ServiceSubscription<MediaPlayer.MediaInfoListener> subscribeMediaInfo(MediaPlayer.MediaInfoListener mediaInfoListener) {
        Util.postError(mediaInfoListener, ServiceCommandError.notSupported());
        return null;
    }

    @Override // com.connectsdk.service.capability.MediaControl
    public ServiceSubscription<MediaControl.PlayStateListener> subscribePlayState(MediaControl.PlayStateListener playStateListener) {
        PlayStateSubscription playStateSubscription = this.playStateSubscription;
        if (playStateSubscription == null) {
            PlayStateSubscription playStateSubscription2 = new PlayStateSubscription(playStateListener);
            this.playStateSubscription = playStateSubscription2;
            this.remoteMediaPlayer.addStatusListener(playStateSubscription2);
        } else if (!playStateSubscription.getListeners().contains(playStateListener)) {
            this.playStateSubscription.addListener(playStateListener);
        }
        getPlayState(playStateListener);
        return this.playStateSubscription;
    }

    @Override // com.connectsdk.service.DeviceService
    public void updateCapabilities() {
        ArrayList arrayList = new ArrayList();
        arrayList.add("MediaPlayer.MediaInfo.Get");
        arrayList.add("MediaPlayer.Display.Image");
        arrayList.add("MediaPlayer.Play.Audio");
        arrayList.add("MediaPlayer.Play.Video");
        arrayList.add("MediaPlayer.Close");
        java.util.Collections.addAll(arrayList, "MediaPlayer.MetaData.MimeType", "MediaPlayer.MetaData.Thumbnail", "MediaPlayer.MetaData.Title", "MediaPlayer.Subtitle.WebVTT");
        java.util.Collections.addAll(arrayList, "MediaControl.Play", "MediaControl.Pause", "MediaControl.Stop", "MediaControl.Seek");
        java.util.Collections.addAll(arrayList, "MediaControl.Duration", "MediaControl.Position", "MediaControl.PlayState", "MediaControl.PlayState.Subscribe");
        setCapabilities(arrayList);
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void displayImage(MediaInfo mediaInfo, MediaPlayer.LaunchListener launchListener) {
        setMediaSource(mediaInfo, launchListener);
    }

    @Override // com.connectsdk.service.capability.MediaPlayer
    public void playMedia(MediaInfo mediaInfo, boolean z, MediaPlayer.LaunchListener launchListener) {
        setMediaSource(mediaInfo, launchListener);
    }
}

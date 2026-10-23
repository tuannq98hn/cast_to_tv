package com.connectsdk.service.webos;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

/* JADX INFO: loaded from: classes3.dex */
public class WebOSTVMouseSocketConnection {
    WebOSTVTrustManager customTrustManager;
    WebOSTVMouseSocketListener listener;
    String socketPath;
    WebSocketClient ws;

    /* JADX INFO: renamed from: com.connectsdk.service.webos.WebOSTVMouseSocketConnection$2, reason: invalid class name */
    public static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$connectsdk$service$webos$WebOSTVMouseSocketConnection$ButtonType;

        static {
            int[] iArr = new int[ButtonType.values().length];
            $SwitchMap$com$connectsdk$service$webos$WebOSTVMouseSocketConnection$ButtonType = iArr;
            try {
                iArr[ButtonType.HOME.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$connectsdk$service$webos$WebOSTVMouseSocketConnection$ButtonType[ButtonType.BACK.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$connectsdk$service$webos$WebOSTVMouseSocketConnection$ButtonType[ButtonType.UP.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$com$connectsdk$service$webos$WebOSTVMouseSocketConnection$ButtonType[ButtonType.DOWN.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                $SwitchMap$com$connectsdk$service$webos$WebOSTVMouseSocketConnection$ButtonType[ButtonType.LEFT.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
            try {
                $SwitchMap$com$connectsdk$service$webos$WebOSTVMouseSocketConnection$ButtonType[ButtonType.RIGHT.ordinal()] = 6;
            } catch (NoSuchFieldError unused6) {
            }
        }
    }

    public enum ButtonType {
        HOME,
        BACK,
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    public interface WebOSTVMouseSocketListener {
        void onConnected();
    }

    public WebOSTVMouseSocketConnection(String str, WebOSTVMouseSocketListener webOSTVMouseSocketListener) {
        this.listener = webOSTVMouseSocketListener;
        this.socketPath = str;
        try {
            connectPointer(new URI(this.socketPath));
        } catch (URISyntaxException e2) {
            e2.printStackTrace();
        }
    }

    public void button(ButtonType buttonType) {
        String str;
        switch (AnonymousClass2.$SwitchMap$com$connectsdk$service$webos$WebOSTVMouseSocketConnection$ButtonType[buttonType.ordinal()]) {
            case 1:
                str = "HOME";
                break;
            case 2:
                str = "BACK";
                break;
            case 3:
                str = "UP";
                break;
            case 4:
                str = "DOWN";
                break;
            case 5:
                str = "LEFT";
                break;
            case 6:
                str = "RIGHT";
                break;
            default:
                str = "NONE";
                break;
        }
        button(str);
    }

    public void click() {
        if (isConnected()) {
            this.ws.send("type:click\n\n");
        }
    }

    public void connectPointer(URI uri) {
        WebSocketClient webSocketClient = this.ws;
        if (webSocketClient != null) {
            webSocketClient.close();
            this.ws = null;
        }
        this.ws = new WebSocketClient(uri) { // from class: com.connectsdk.service.webos.WebOSTVMouseSocketConnection.1
            @Override // org.java_websocket.client.WebSocketClient
            public void onClose(int i, String str, boolean z) {
            }

            @Override // org.java_websocket.client.WebSocketClient
            public void onError(Exception exc) {
            }

            @Override // org.java_websocket.client.WebSocketClient
            public void onMessage(String str) {
            }

            @Override // org.java_websocket.client.WebSocketClient
            public void onOpen(ServerHandshake serverHandshake) {
                this.uri.toString();
                WebOSTVMouseSocketListener webOSTVMouseSocketListener = WebOSTVMouseSocketConnection.this.listener;
                if (webOSTVMouseSocketListener != null) {
                    webOSTVMouseSocketListener.onConnected();
                }
            }
        };
        try {
            SSLContext sSLContext = SSLContext.getInstance("TLS");
            WebOSTVTrustManager webOSTVTrustManager = new WebOSTVTrustManager();
            this.customTrustManager = webOSTVTrustManager;
            sSLContext.init(null, new WebOSTVTrustManager[]{webOSTVTrustManager}, null);
            this.ws.setSocket(sSLContext.getSocketFactory().createSocket());
            this.ws.setConnectionLostTimeout(0);
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (RuntimeException e3) {
            e3.printStackTrace();
        } catch (KeyException e4) {
            e4.printStackTrace();
        } catch (NoSuchAlgorithmException e5) {
            e5.printStackTrace();
        }
        this.ws.connect();
    }

    public void disconnect() {
        WebSocketClient webSocketClient = this.ws;
        if (webSocketClient != null) {
            webSocketClient.close();
            this.ws = null;
        }
    }

    public boolean isConnected() {
        WebSocketClient webSocketClient = this.ws;
        if (webSocketClient == null) {
            System.out.println("ws is null");
        } else if (webSocketClient.getReadyState() != ReadyState.OPEN) {
            System.out.println("ws state is not ready");
        }
        WebSocketClient webSocketClient2 = this.ws;
        return webSocketClient2 != null && webSocketClient2.getReadyState() == ReadyState.OPEN;
    }

    public void move(double d, double d2) {
        if (isConnected()) {
            this.ws.send("type:move\ndx:" + d + "\ndy:" + d2 + "\ndown:0\n\n");
        }
    }

    public void scroll(double d, double d2) {
        if (isConnected()) {
            this.ws.send("type:scroll\ndx:" + d + "\ndy:" + d2 + "\n\n");
        }
    }

    public void move(double d, double d2, boolean z) {
        if (isConnected()) {
            this.ws.send("type:move\ndx:" + d + "\ndy:" + d2 + "\ndown:" + (z ? 1 : 0) + "\n\n");
        }
    }

    public void button(String str) {
        if (isConnected()) {
            this.ws.send("type:button\nname:" + str + "\n\n");
        }
    }
}

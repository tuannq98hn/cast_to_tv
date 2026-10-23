package com.connectsdk.core;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.connectsdk.service.capability.listeners.ErrorListener;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.http.conn.util.InetAddressUtils;

/* JADX INFO: loaded from: classes3.dex */
public final class Util {
    private static final int NUM_OF_THREADS = 20;
    public static String T = "Connect SDK";
    private static Executor executor;
    private static Handler handler;

    static {
        createExecutor();
    }

    public static byte[] convertIpAddress(int i) {
        return new byte[]{(byte) (i & 255), (byte) ((i >> 8) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 24) & 255)};
    }

    public static void createExecutor() {
        executor = Executors.newFixedThreadPool(20, new ThreadFactory() { // from class: com.connectsdk.core.Util.1
            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("2nd Screen BG");
                return thread;
            }
        });
    }

    public static Executor getExecutor() {
        return executor;
    }

    public static InetAddress getIpAddress(Context context) throws UnknownHostException {
        int ipAddress = ((WifiManager) context.getSystemService("wifi")).getConnectionInfo().getIpAddress();
        if (ipAddress == 0) {
            return null;
        }
        return InetAddress.getByAddress(convertIpAddress(ipAddress));
    }

    public static long getTime() {
        return TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
    }

    public static boolean isIPv4Address(String str) {
        return InetAddressUtils.isIPv4Address(str);
    }

    public static boolean isIPv6Address(String str) {
        return InetAddressUtils.isIPv6Address(str);
    }

    public static boolean isMain() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void postError(final ErrorListener errorListener, final ServiceCommandError serviceCommandError) {
        if (errorListener == null) {
            return;
        }
        runOnUI(new Runnable() { // from class: com.connectsdk.core.Util.3
            @Override // java.lang.Runnable
            public void run() {
                errorListener.onError(serviceCommandError);
            }
        });
    }

    public static <T> void postSuccess(final ResponseListener<T> responseListener, final T t) {
        if (responseListener == null) {
            return;
        }
        runOnUI(new Runnable() { // from class: com.connectsdk.core.Util.2
            @Override // java.lang.Runnable
            public void run() {
                if (t != null) {
                    responseListener.onSuccess(t);
                }
            }
        });
    }

    public static void runInBackground(Runnable runnable, boolean z) {
        if (z || isMain()) {
            executor.execute(runnable);
        } else {
            runnable.run();
        }
    }

    public static void runOnUI(Runnable runnable) {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        handler.post(runnable);
    }

    public static String uniqueID() {
        StringBuilder sb = new StringBuilder("35");
        sb.append(Build.BOARD.length() % 10);
        sb.append(Build.BRAND.length() % 10);
        sb.append(Build.DEVICE.length() % 10);
        sb.append(Build.DISPLAY.length() % 10);
        sb.append(Build.HOST.length() % 10);
        sb.append(Build.ID.length() % 10);
        sb.append(Build.MANUFACTURER.length() % 10);
        sb.append(Build.MODEL.length() % 10);
        sb.append(Build.PRODUCT.length() % 10);
        sb.append(Build.TAGS.length() % 10);
        sb.append(Build.TYPE.length() % 10);
        sb.append(Build.USER.length() % 10);
        return sb.toString();
    }

    public static void runInBackground(Runnable runnable) {
        runInBackground(runnable, false);
    }
}

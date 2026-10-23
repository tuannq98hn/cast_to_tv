package com.connectsdk.service.command;

/* JADX INFO: loaded from: classes3.dex */
public class ServiceCommandError extends Error {
    private static final long serialVersionUID = 4232138682873631468L;
    protected int code;
    protected String keycode;
    protected Object payload;

    public ServiceCommandError() {
    }

    public static ServiceCommandError getError(int i) {
        String str;
        if (i == 400) {
            str = "Bad Request";
        } else if (i == 401) {
            str = "Unauthorized";
        } else if (i == 500) {
            str = "Internal Server Error";
        } else {
            str = i == 503 ? "Service Unavailable" : "Unknown Error";
        }
        return new ServiceCommandError(i, str, null);
    }

    public static ServiceCommandError notSupported() {
        return new NotSupportedServiceCommandError();
    }

    public int getCode() {
        return this.code;
    }

    public String getKeycode() {
        return this.keycode;
    }

    public Object getPayload() {
        return this.payload;
    }

    public ServiceCommandError(String str) {
        super(str);
    }

    public ServiceCommandError(int i, String str) {
        super(str);
        this.code = i;
    }

    public ServiceCommandError(String str, String str2) {
        super(str2);
        this.keycode = str;
    }

    public ServiceCommandError(int i, String str, Object obj) {
        super(str);
        this.code = i;
        this.payload = obj;
    }
}

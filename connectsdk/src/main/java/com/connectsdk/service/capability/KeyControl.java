package com.connectsdk.service.capability;

import com.connectsdk.service.capability.listeners.ResponseListener;

/* JADX INFO: loaded from: classes3.dex */
public interface KeyControl extends CapabilityMethods {
    public static final String Any = "KeyControl.Any";
    public static final String Back = "KeyControl.Back";
    public static final String[] Capabilities = {"KeyControl.Up", "KeyControl.Down", "KeyControl.Left", "KeyControl.Right", "KeyControl.OK", "KeyControl.Back", "KeyControl.Home", "KeyControl.KeyCode"};
    public static final String Down = "KeyControl.Down";
    public static final String Home = "KeyControl.Home";
    public static final String KeyCodeNew = "KeyControl.KeyCode";
    public static final String Left = "KeyControl.Left";
    public static final String OK = "KeyControl.OK";
    public static final String Right = "KeyControl.Right";
    public static final String Send_Key = "KeyControl.SendKey";
    public static final String Up = "KeyControl.Up";

    public enum KeyCode {
        NUM_0 (0),
        NUM_1 (1),
        NUM_2 (2),
        NUM_3 (3),
        NUM_4 (4),
        NUM_5 (5),
        NUM_6 (6),
        NUM_7 (7),
        NUM_8 (8),
        NUM_9 (9),

        DASH (10),
        ENTER (11);

        private final int code; 

        private static final KeyCode[] codes = {
            NUM_0, NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9, DASH, ENTER
        };

        KeyCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static KeyCode createFromInteger(int keyCode) {
            if (keyCode >= 0 && keyCode < codes.length) {
                return codes[keyCode];
            }
            return null;
        }
    }

    void back(ResponseListener<Object> responseListener);

    void blue(ResponseListener<Object> responseListener);

    void down(ResponseListener<Object> responseListener);

    void exit_lg(ResponseListener<Object> responseListener);

    KeyControl getKeyControl();

    CapabilityMethods.CapabilityPriorityLevel getKeyControlCapabilityLevel();

    void green(ResponseListener<Object> responseListener);

    void guide_lg(ResponseListener<Object> responseListener);

    void home(ResponseListener<Object> responseListener);

    void left(ResponseListener<Object> responseListener);

    void ok(ResponseListener<Object> responseListener);

    void power(ResponseListener<Object> responseListener);

    void red(ResponseListener<Object> responseListener);

    void right(ResponseListener<Object> responseListener);

    void sendKeyCode(KeyCode keyCode, ResponseListener<Object> responseListener);

    void sendKeyString(String str, ResponseListener<Object> responseListener);

    void sendVizeoKeyCode(VizioKeyCode vizioKeyCode, ResponseListener<Object> responseListener);

    void setting_lg(ResponseListener<Object> responseListener);

    void up(ResponseListener<Object> responseListener);

    void voice_lg(ResponseListener<Object> responseListener);

    void yellow(ResponseListener<Object> responseListener);
}

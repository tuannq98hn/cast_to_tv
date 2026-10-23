package com.evgenii.jsevaluator;

import android.os.Handler;
import com.evgenii.jsevaluator.interfaces.HandlerWrapperInterface;

/* JADX INFO: loaded from: classes3.dex */
public class HandlerWrapper implements HandlerWrapperInterface {
    private final Handler mHandler = new Handler();

    @Override // com.evgenii.jsevaluator.interfaces.HandlerWrapperInterface
    public void post(Runnable runnable) {
        this.mHandler.post(runnable);
    }
}

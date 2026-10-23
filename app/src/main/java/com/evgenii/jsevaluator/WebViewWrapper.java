package com.evgenii.jsevaluator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Base64;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.evgenii.jsevaluator.interfaces.CallJavaResultInterface;
import com.evgenii.jsevaluator.interfaces.WebViewWrapperInterface;
import java.io.UnsupportedEncodingException;

/* JADX INFO: loaded from: classes3.dex */
@SuppressLint({"SetJavaScriptEnabled"})
public class WebViewWrapper implements WebViewWrapperInterface {
    protected WebView mWebView;

    public WebViewWrapper(Context context, CallJavaResultInterface callJavaResultInterface) {
        WebView webView = new WebView(context);
        this.mWebView = webView;
        webView.setWillNotDraw(true);
        WebSettings settings = this.mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDefaultTextEncodingName("utf-8");
        this.mWebView.addJavascriptInterface(new JavaScriptInterface(callJavaResultInterface), "evgeniiJsEvaluator");
    }

    @Override // com.evgenii.jsevaluator.interfaces.WebViewWrapperInterface
    public void destroy() {
        WebView webView = this.mWebView;
        if (webView != null) {
            webView.removeJavascriptInterface("evgeniiJsEvaluator");
            this.mWebView.loadUrl("about:blank");
            this.mWebView.stopLoading();
            this.mWebView.clearHistory();
            this.mWebView.removeAllViews();
            this.mWebView.destroyDrawingCache();
            this.mWebView.destroy();
            this.mWebView = null;
        }
    }

    @Override // com.evgenii.jsevaluator.interfaces.WebViewWrapperInterface
    public WebView getWebView() {
        return this.mWebView;
    }

    @Override // com.evgenii.jsevaluator.interfaces.WebViewWrapperInterface
    public void loadJavaScript(String str) {
        try {
            String strEncodeToString = Base64.encodeToString(("<script>" + str + "</script>").getBytes("UTF-8"), 0);
            this.mWebView.loadUrl("data:text/html;charset=utf-8;base64," + strEncodeToString);
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
        }
    }
}

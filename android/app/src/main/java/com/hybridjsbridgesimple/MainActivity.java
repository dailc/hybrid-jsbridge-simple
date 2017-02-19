package com.hybridjsbridgesimple;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.JsPromptResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hybridjsbridgesimple.jsbridge.BridgeImpl;
import com.hybridjsbridgesimple.jsbridge.JSBridge;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by dailc on 2017/2/19 0019.
 *
 * 为了方便，直接在mainActivity中进行简单的JSBridge通信测试
 */
public class MainActivity extends AppCompatActivity {
    public WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        wv=(WebView) findViewById(R.id.wv);
        //初始化webview控件
        setUpWebViewDefaults();

        //注册JSBridge api
        JSBridge.register("simple_bridge", BridgeImpl.class);


        //加载页面 平时一般是加载远程网络页面的
        wv.loadUrl("file:///android_asset/index.html");
    }

    /**
     * 设置webview  可以根据不同需要进行定义
     */
    private void setUpWebViewDefaults() {

        WebSettings settings = wv.getSettings();

        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + "SimleJsbridgeScheme/1.0");
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);  //设置 缓存模式
        settings.setDomStorageEnabled(true);


        String appCachePath = this.getApplicationContext().getCacheDir().getAbsolutePath();
        settings.setAppCachePath(appCachePath);
        settings.setAllowFileAccess(true);
        settings.setAppCacheEnabled(true);
        settings.setDisplayZoomControls(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }


        wv.setWebViewClient(new WebViewClient() {

/*            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }*/

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);


            }

            @Override
            public void onPageStarted(final WebView view, final String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

            }

        });

        wv.setWebChromeClient(new WebChromeClient() {

            /**
             * 本示例没有采用在shouldOverrideUrlLoading中拦截，而是通过拦截这个confirm
             * @param view
             * @param url
             * @param message
             * @param defaultValue
             * @param result
             * @return
             */
            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                result.confirm(JSBridge.callJava(MainActivity.this, view, message));
                return true;
            }
        });


    }
}

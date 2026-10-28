package io.github.ihatobatsugui.firefightfont;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class MainActivity extends Activity {

    // 通过 AssetLoader 用 https 源加载内置页面，localStorage / IndexedDB 才能正常持久化
    private static final String START_URL = "https://appassets.androidplatform.net/assets/index.html";
    private static final int REQUEST_FONT_FILE = 1001;

    private WebView webView;
    private ValueCallback<Uri[]> pendingFileCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(false);
        settings.setTextZoom(100);

        webView.addJavascriptInterface(new FileBridge(this), "AndroidSaver");
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                // 内置页面继续交给 AssetLoader
                if (uri != null && "appassets.androidplatform.net".equals(uri.getHost())) {
                    return false;
                }
                // 其余链接（GitHub 等）交给系统浏览器，避免 WebView 报 ERR_CACHE_MISS
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            }
        });
        // 没有 WebChromeClient 的 onShowFileChooser，<input type="file"> 点了不会弹选择器
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingFileCallback != null) {
                    pendingFileCallback.onReceiveValue(null);
                }
                pendingFileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(intent, "选择字体文件"), REQUEST_FONT_FILE);
                    return true;
                } catch (ActivityNotFoundException error) {
                    pendingFileCallback = null;
                    return false;
                }
            }
        });

        setContentView(webView);
        webView.loadUrl(START_URL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FONT_FILE) {
            if (pendingFileCallback == null) {
                return;
            }
            Uri[] uris = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getData() != null) {
                    uris = new Uri[]{ data.getData() };
                } else if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    uris = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        uris[i] = data.getClipData().getItemAt(i).getUri();
                    }
                }
            }
            pendingFileCallback.onReceiveValue(uris);
            pendingFileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}

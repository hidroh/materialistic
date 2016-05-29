package io.github.hidroh.materialistic.test;

import android.webkit.DownloadListener;
import android.webkit.WebView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = WebView.class, inheritImplementationMethods = true)
public class ShadowWebView extends org.robolectric.shadows.ShadowWebView {
    private DownloadListener downloadListener;
    public static String lastGlobalLoadedUrl;

    @Implementation
    public void setDownloadListener(DownloadListener listener) {
        downloadListener = listener;
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    @Override
    public void loadUrl(String url) {
        super.loadUrl(url);
        lastGlobalLoadedUrl = url;
    }

    public static String getLastGlobalLoadedUrl() {
        String lastLoaded = lastGlobalLoadedUrl;
        lastGlobalLoadedUrl = null;
        return lastLoaded;
    }
}

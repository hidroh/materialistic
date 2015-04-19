package io.github.hidroh.materialistic.test;

import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.fakes.RoboWebSettings;

@Implements(value = WebView.class, inheritImplementationMethods = true)
public class ShadowWebView extends org.robolectric.shadows.ShadowWebView {
    private DownloadListener downloadListener;

    @Override @Implementation
    public WebSettings getSettings() {
        return new RoboWebSettings();
    }

    @Implementation
    public void setDownloadListener(DownloadListener listener) {
        downloadListener = listener;
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }
}

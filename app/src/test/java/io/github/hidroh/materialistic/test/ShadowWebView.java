package io.github.hidroh.materialistic.test;

import android.annotation.TargetApi;
import android.os.Build;
import android.webkit.DownloadListener;
import android.webkit.WebView;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(value = WebView.class, inheritImplementationMethods = true)
public class ShadowWebView extends org.robolectric.shadows.ShadowWebView {
    private DownloadListener downloadListener;
    private WebView.FindListener findListener;
    public static String lastGlobalLoadedUrl;
    private int findCount;

    @Implementation
    public void setDownloadListener(DownloadListener listener) {
        downloadListener = listener;
    }

    @Implementation
    public void setFindListener(WebView.FindListener listener) {
        findListener = listener;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Implementation
    public void findAllAsync(String find) {
        if (findListener != null) {
            findListener.onFindResultReceived(0, findCount, true);
        }
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    public void setFindCount(int findCount) {
        this.findCount = findCount;
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

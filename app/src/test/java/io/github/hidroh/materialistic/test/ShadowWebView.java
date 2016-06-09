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
    private int findIndex;
    private int progress;
    private int pageIndex;
    private int zoomDegree;
    private int scrollY = -1;

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

    @Implementation
    public void findNext(boolean forward) {
        if (forward) {
            findIndex++;
        } else {
            findIndex--;
        }
    }

    @Implementation
    public int getProgress() {
        return progress;
    }

    @Implementation
    public void goBack() {
        pageIndex--;
    }

    @Implementation
    public void goForward() {
        pageIndex++;
    }

    @Implementation
    public void zoomIn() {
        zoomDegree++;
    }

    @Implementation
    public void zoomOut() {
        zoomDegree--;
    }

    @Implementation
    public boolean pageUp(boolean top) {
        if (top) {
            scrollY = 0;
        } else {
            scrollY--;
        }
        return true;
    }

    @Implementation
    public boolean pageDown(boolean bottom) {
        scrollY++;
        return true;
    }

    public int getFindIndex() {
        return findIndex;
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

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getZoomDegree() {
        return zoomDegree;
    }

    @Override
    public int getScrollY() {
        return scrollY;
    }
}

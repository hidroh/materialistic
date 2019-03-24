/*
 * Copyright (c) 2016 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.CallSuper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

import java.io.File;
import java.util.Map;

import io.github.hidroh.materialistic.AppUtils;

public class CacheableWebView extends WebView {
    private static final String CACHE_PREFIX = "webarchive-";
    private static final String CACHE_EXTENSION = ".mht";
    private ArchiveClient mArchiveClient = new ArchiveClient();

    public CacheableWebView(Context context) {
        this(context, null);
    }

    public CacheableWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CacheableWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public void reloadUrl(String url) {
        super.reloadUrl(getCacheableUrl(url));
    }

    @Override
    public void loadUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        mArchiveClient.lastProgress = 0;
        super.loadUrl(getCacheableUrl(url));
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        mArchiveClient.lastProgress = 0;
        super.loadUrl(getCacheableUrl(url), additionalHttpHeaders);
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        if (!(client instanceof ArchiveClient)) {
            throw new IllegalArgumentException("client should be an instance of " +
                    ArchiveClient.class.getName());
        }
        mArchiveClient = (ArchiveClient) client;
        super.setWebChromeClient(mArchiveClient);
    }

    private void init() {
        enableCache();
        setLoadSettings();
        setWebViewClient(new WebViewClient());
        setWebChromeClient(mArchiveClient);
    }

    private void enableCache() {
        WebSettings webSettings = getSettings();
        webSettings.setAppCacheEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCachePath(getContext().getApplicationContext()
                .getCacheDir().getAbsolutePath());
        setCacheModeInternal();
    }

    private void setCacheModeInternal() {
        getSettings().setCacheMode(AppUtils.hasConnection(getContext()) ?
                WebSettings.LOAD_CACHE_ELSE_NETWORK : WebSettings.LOAD_CACHE_ONLY);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setLoadSettings() {
        WebSettings webSettings = getSettings();
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptEnabled(true);
    }

    private String getCacheableUrl(String url) {
        if (TextUtils.equals(url, BLANK) || TextUtils.equals(url, FILE)) {
            return url;
        }
        mArchiveClient.cacheFileName = generateCacheFilename(url);
        setCacheModeInternal();
        if (getSettings().getCacheMode() != WebSettings.LOAD_CACHE_ONLY) {
            return url;
        }
        File cacheFile = new File(mArchiveClient.cacheFileName);
        return cacheFile.exists() ? Uri.fromFile(cacheFile).toString() : url;
    }

    private String generateCacheFilename(String url) {
        return getContext().getApplicationContext().getCacheDir().getAbsolutePath() +
                File.separator +
                CACHE_PREFIX +
                url.hashCode() +
                CACHE_EXTENSION;
    }

    public static class ArchiveClient extends WebChromeClient {
        int lastProgress = 0;
        String cacheFileName = null;

        @CallSuper
        @Override
        public void onProgressChanged(android.webkit.WebView view, int newProgress) {
            if (view.getSettings().getCacheMode() == WebSettings.LOAD_CACHE_ONLY) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                    cacheFileName != null && lastProgress != 100 && newProgress == 100) {
                lastProgress = newProgress;
                view.saveWebArchive(cacheFileName);
            }
        }

    }
}

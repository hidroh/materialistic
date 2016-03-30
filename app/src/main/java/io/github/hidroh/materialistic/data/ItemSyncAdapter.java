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

package io.github.hidroh.materialistic.data;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.IOException;
import java.util.Set;

import io.github.hidroh.materialistic.Preferences;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Simple sync adapter that triggers OkHttp requests so their responses become available in
 * cache for subsequent requests
 */
public class ItemSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String EXTRA_ID = ItemSyncAdapter.class.getName() + ".EXTRA_ID";
    static final String SYNC_PREFERENCES_FILE = "_syncpreferences";
    private boolean mConnectionEnabled;
    private boolean mReadabilityEnabled;
    private boolean mCommentsEnabled;

    /**
     * Triggers a {@link WebView#loadUrl(String)} without actual UI
     * to save content to app cache if available
     * @param context    context
     * @param url        url to load
     */
    @UiThread
    public static void saveWebCache(Context context, String url) {
        if (!TextUtils.isEmpty(url) &&
                Preferences.Offline.currentConnectionEnabled(context) &&
                Preferences.Offline.isArticleEnabled(context)) {
            WebView webView = new WebView(context);
            enableCache(context, webView.getSettings());
            webView.loadUrl(url);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void enableCache(Context context, WebSettings webSettings) {
        webSettings.setAppCacheEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCachePath(context.getApplicationContext()
                .getCacheDir().getAbsolutePath());
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setJavaScriptEnabled(true);
    }

    private final HackerNewsClient.RestService mHnRestService;
    private final ReadabilityClient mReadabilityClient;
    private final SharedPreferences mSharedPreferences;

    public ItemSyncAdapter(Context context, RestServiceFactory factory,
                           ReadabilityClient readabilityClient) {
        super(context, true);
        mSharedPreferences = context.getSharedPreferences(
                context.getPackageName() + SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
        mHnRestService = factory.create(HackerNewsClient.BASE_API_URL,
                HackerNewsClient.RestService.class);
        mReadabilityClient = readabilityClient;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        if (!Preferences.Offline.isEnabled(getContext())) {
            return;
        }
        // assume that connection wouldn't change until we finish syncing
        mConnectionEnabled = Preferences.Offline.currentConnectionEnabled(getContext());
        mReadabilityEnabled = Preferences.Offline.isReadabilityEnabled(getContext());
        mCommentsEnabled = Preferences.Offline.isCommentsEnabled(getContext());
        if (extras.containsKey(EXTRA_ID)) {
            sync(extras.getString(EXTRA_ID));
        } else {
            syncDeferredItems();
        }
    }

    private void syncDeferredItems() {
        Set<String> itemIds = mSharedPreferences.getAll().keySet();
        for (String itemId : itemIds) {
            sync(itemId);
        }
    }

    private void sync(final String itemId) {
        HackerNewsItem cachedItem;
        if ((cachedItem = getFromCache(itemId)) != null) {
            syncReadability(cachedItem);
            syncChildren(cachedItem);
        } else if (mConnectionEnabled) {
            // TODO defer on low battery as well?
            mHnRestService.networkItem(itemId).enqueue(new Callback<HackerNewsItem>() {
                @Override
                public void onResponse(Call<HackerNewsItem> call,
                                       retrofit2.Response<HackerNewsItem> response) {
                    mSharedPreferences.edit().remove(itemId).apply();
                    HackerNewsItem item;
                    if ((item = response.body()) != null) {
                        syncReadability(item);
                        syncChildren(item);
                    }
                }

                @Override
                public void onFailure(Call<HackerNewsItem> call, Throwable t) {
                    // no op
                }
            });
        } else {
            defer(itemId);
        }
    }

    private void syncReadability(@NonNull HackerNewsItem item) {
        if (mConnectionEnabled && mReadabilityEnabled && item.isStoryType()) {
            mReadabilityClient.parse(item.getId(), item.getRawUrl(),
                    new ReadabilityClient.Callback() {
                        @Override
                        public void onResponse(String content) {
                            // no op
                        }
                    });
        }
    }

    private void syncChildren(@NonNull HackerNewsItem item) {
        if (mCommentsEnabled && item.getKids() != null) {
            for (long id : item.getKids()) {
                sync(String.valueOf(id));
            }
        }
    }

    private void defer(String itemId) {
        mSharedPreferences.edit().putBoolean(itemId, true).apply();
    }

    private HackerNewsItem getFromCache(String itemId) {
        try {
            return mHnRestService.cachedItem(itemId).execute().body();
        } catch (IOException e) {
            return null;
        }
    }
}

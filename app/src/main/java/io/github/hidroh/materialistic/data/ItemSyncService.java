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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.webkit.WebView;

import javax.inject.Inject;

import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.accounts.EmptyAccountAuthenticator;
import io.github.hidroh.materialistic.widget.AdBlockWebViewClient;
import io.github.hidroh.materialistic.widget.CacheableWebView;

public class ItemSyncService extends Service {

    private static ItemSyncAdapter sItemSyncAdapter = null;
    private static final Object sItemSyncAdapterLock = new Object();
    @Inject RestServiceFactory mFactory;
    @Inject ReadabilityClient mReadabilityClient;
    private final BroadcastReceiver mReceiver = new WebCacheReceiver();

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication())
                .getApplicationGraph()
                .plus(new ActivityModule(this)) // TODO split to network module
                .inject(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(WebCacheReceiver.ACTION));
        synchronized (sItemSyncAdapterLock) {
            if (sItemSyncAdapter == null) {
                sItemSyncAdapter = new ItemSyncAdapter(getApplicationContext(),
                        mFactory, mReadabilityClient);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sItemSyncAdapter.getSyncAdapterBinder();
    }

    public static class DummyAuthenticatorService extends Service {
        private EmptyAccountAuthenticator mAuthenticator;

        @Override
        public void onCreate() {
            super.onCreate();
            mAuthenticator = new EmptyAccountAuthenticator(this);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return mAuthenticator.getIBinder();
        }
    }

    static class WebCacheReceiver extends BroadcastReceiver {
        static final String ACTION = "io.github.hidroh.materialistic.WEB_CACHE_REQUEST";
        static final String EXTRA_URL = WebCacheReceiver.class.getName() + ".EXTRA_URL";

        @Override
        public void onReceive(Context context, Intent intent) {
            String url = intent.getStringExtra(EXTRA_URL);
            if (TextUtils.isEmpty(url)) {
                return;
            }
            if (Preferences.Offline.currentConnectionEnabled(context) &&
                    Preferences.Offline.isArticleEnabled(context)) {
                WebView webView = new CacheableWebView(context);
                webView.setWebViewClient(new AdBlockWebViewClient(Preferences.adBlockEnabled(context)));
                webView.loadUrl(url);
            }
        }

        static void initSave(Context context, String url) {
            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(new Intent(ACTION).putExtra(EXTRA_URL, url));
        }
    }
}

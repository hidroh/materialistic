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
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Set;

import io.github.hidroh.materialistic.AppUtils;
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
        if (!Preferences.isSavedItemSyncEnabled(getContext())) {
            return;
        }
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
        } else if (AppUtils.isOnWiFi(getContext())) { // TODO defer on low battery as well
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
        if (item.isStoryType() && AppUtils.isOnWiFi(getContext())) {
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
        if (item.getKids() != null) {
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

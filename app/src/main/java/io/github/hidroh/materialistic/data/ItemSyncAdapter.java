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
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

/**
 * Simple sync adapter that triggers OkHttp requests so their responses become available in
 * cache for subsequent requests
 */
class ItemSyncAdapter extends AbstractThreadedSyncAdapter {
    private final RestServiceFactory mFactory;
    private final ReadabilityClient mReadabilityClient;

    ItemSyncAdapter(Context context, RestServiceFactory factory,
                           ReadabilityClient readabilityClient) {
        super(context, true);
        mFactory = factory;
        mReadabilityClient = readabilityClient;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        String itemId = extras.getString(SyncDelegate.EXTRA_ID);
        createSyncDelegate()
                .performSync(new SyncDelegate.JobBuilder(itemId)
                .setConnectionEnabled(extras.getBoolean(SyncDelegate.EXTRA_CONNECTION_ENABLED))
                .setReadabilityEnabled(extras.getBoolean(SyncDelegate.EXTRA_READABILITY_ENABLED))
                .setArticleEnabled(extras.getBoolean(SyncDelegate.EXTRA_ARTICLE_ENABLED))
                .setCommentsEnabled(extras.getBoolean(SyncDelegate.EXTRA_COMMENTS_ENABLED))
                .setNotificationEnabled(extras.getBoolean(SyncDelegate.EXTRA_NOTIFICATION_ENABLED))
                .build());
    }

    @VisibleForTesting
    @NonNull
    SyncDelegate createSyncDelegate() {
        return new SyncDelegate(getContext(), mFactory, mReadabilityClient);
    }
}

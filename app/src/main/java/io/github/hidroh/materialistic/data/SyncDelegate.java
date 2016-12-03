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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.webkit.WebView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.widget.AdBlockWebViewClient;
import io.github.hidroh.materialistic.widget.CacheableWebView;
import retrofit2.Call;
import retrofit2.Callback;

public class SyncDelegate {
    static final String SYNC_PREFERENCES_FILE = "_syncpreferences";
    static final String EXTRA_ID = ItemSyncAdapter.class.getName() + ".EXTRA_ID";
    private static final String NOTIFICATION_GROUP_KEY = "group";
    static final String EXTRA_CONNECTION_ENABLED = "extra:connectionEnabled";
    static final String EXTRA_READABILITY_ENABLED = "extra:readabilityEnabled";
    static final String EXTRA_ARTICLE_ENABLED = "extra:articleEnabled";
    static final String EXTRA_COMMENTS_ENABLED = "extra:commentsEnabled";
    static final String EXTRA_NOTIFICATION_ENABLED = "extra:notificationEnabled";
    private final HackerNewsClient.RestService mHnRestService;
    private final ReadabilityClient mReadabilityClient;
    private final SharedPreferences mSharedPreferences;
    private final NotificationManager mNotificationManager;
    private final NotificationCompat.Builder mNotificationBuilder;
    private final Map<String, SyncProgress> mSyncProgresses = new HashMap<>();
    private final Context mContext;
    private ProgressListener mListener;
    private Job mJob;
    @VisibleForTesting CacheableWebView mWebView;

    @Inject
    SyncDelegate(Context context, RestServiceFactory factory,
                 ReadabilityClient readabilityClient) {
        mContext = context;
        mSharedPreferences = context.getSharedPreferences(
                context.getPackageName() + SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
        mHnRestService = factory.create(HackerNewsClient.BASE_API_URL,
                HackerNewsClient.RestService.class, new BackgroundThreadExecutor());
        mReadabilityClient = readabilityClient;
        mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(context)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.ic_notification)
                .setGroup(NOTIFICATION_GROUP_KEY)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setAutoCancel(true);
    }

    @UiThread
    static void initSync(Context context, @Nullable String itemId) {
        if (!Preferences.Offline.isEnabled(context)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !TextUtils.isEmpty(itemId)) {
            PersistableBundle extras = new PersistableBundle();
            extras.putString(ItemSyncJobService.EXTRA_ID, itemId);
            JobInfo.Builder builder = new JobInfo.Builder(Long.valueOf(itemId).intValue(),
                    new ComponentName(context.getPackageName(),
                            ItemSyncJobService.class.getName()))
                    .setRequiredNetworkType(Preferences.Offline.isWifiOnly(context) ?
                            JobInfo.NETWORK_TYPE_UNMETERED :
                            JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(extras);
            if (Preferences.Offline.currentConnectionEnabled(context)) {
                builder.setOverrideDeadline(0);
            }
            ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE))
                    .schedule(builder.build());
        } else {
            Bundle extras = new Bundle();
            if (itemId != null) {
                extras.putString(EXTRA_ID, itemId);
            }
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            extras.putBoolean(EXTRA_CONNECTION_ENABLED, Preferences.Offline.currentConnectionEnabled(context));
            extras.putBoolean(EXTRA_READABILITY_ENABLED, Preferences.Offline.isReadabilityEnabled(context));
            extras.putBoolean(EXTRA_ARTICLE_ENABLED, Preferences.Offline.isArticleEnabled(context));
            extras.putBoolean(EXTRA_COMMENTS_ENABLED, Preferences.Offline.isCommentsEnabled(context));
            extras.putBoolean(EXTRA_NOTIFICATION_ENABLED, Preferences.Offline.isNotificationEnabled(context));
            ContentResolver.requestSync(Application.createSyncAccount(),
                    MaterialisticProvider.PROVIDER_AUTHORITY, extras);
        }
    }

    void subscribe(ProgressListener listener) {
        mListener = listener;
    }

    void performSync(@NonNull Job job) {
        // assume that connection wouldn't change until we finish syncing
        mJob = job;
        if (!TextUtils.isEmpty(mJob.id)) {
            mSyncProgresses.put(mJob.id, new SyncProgress(mJob));
            sync(mJob.id, mJob.id);
        } else {
            syncDeferredItems();
        }
    }

    private void syncDeferredItems() {
        Set<String> itemIds = mSharedPreferences.getAll().keySet();
        for (String itemId : itemIds) {
            sync(itemId, null); // do not show notifications for deferred items
        }
    }

    private void sync(String itemId, final String progressId) {
        if (!mJob.connectionEnabled) {
            defer(itemId);
            return;
        }
        HackerNewsItem cachedItem;
        if ((cachedItem = getFromCache(itemId)) != null) {
            sync(cachedItem, progressId);
        } else {
            showNotification(progressId);
            // TODO defer on low battery as well?
            mHnRestService.networkItem(itemId).enqueue(new Callback<HackerNewsItem>() {
                @Override
                public void onResponse(Call<HackerNewsItem> call,
                                       retrofit2.Response<HackerNewsItem> response) {
                    HackerNewsItem item;
                    if ((item = response.body()) != null) {
                        sync(item, progressId);
                    }
                }

                @Override
                public void onFailure(Call<HackerNewsItem> call, Throwable t) {
                    notifyItem(progressId, itemId, null);
                }
            });
        }
    }

    @Synthetic
    void sync(@NonNull HackerNewsItem item, String progressId) {
        mSharedPreferences.edit().remove(item.getId()).apply();
        notifyItem(progressId, item.getId(), item);
        syncReadability(item);
        syncArticle(item);
        syncChildren(item);
    }

    private void syncReadability(@NonNull HackerNewsItem item) {
        if (mJob.readabilityEnabled && item.isStoryType()) {
            final String itemId = item.getId();
            mReadabilityClient.parse(itemId, item.getRawUrl(), content -> notifyReadability(itemId));
        }
    }

    private void syncArticle(@NonNull HackerNewsItem item) {
        if (mJob.articleEnabled && item.isStoryType() && !TextUtils.isEmpty(item.getUrl())) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                loadArticle(item);
            } else {
                mContext.startService(new Intent(mContext, WebCacheService.class)
                        .putExtra(WebCacheService.EXTRA_URL, item.getUrl()));
                notifyArticle(item.getId(), 100);
            }
        }
    }

    private void loadArticle(@NonNull final HackerNewsItem item) {
        mWebView = new CacheableWebView(mContext);
        mWebView.setWebViewClient(new AdBlockWebViewClient(Preferences.adBlockEnabled(mContext)));
        mWebView.setWebChromeClient(new CacheableWebView.ArchiveClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                notifyArticle(item.getId(), newProgress);
            }
        });
        notifyArticle(item.getId(), 0);
        mWebView.loadUrl(item.getUrl());
    }

    private void syncChildren(@NonNull HackerNewsItem item) {
        if (mJob.commentsEnabled && item.getKids() != null) {
            for (long id : item.getKids()) {
                sync(String.valueOf(id), item.getId());
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

    @Synthetic
    void notifyItem(@Nullable String progressId, @NonNull String id,
                    @Nullable HackerNewsItem item) {
        if (!mSyncProgresses.containsKey(progressId)) {
            return;
        }
        mSyncProgresses.get(progressId).finishItem(id, item,
                mJob.commentsEnabled && mJob.connectionEnabled,
                mJob.readabilityEnabled && mJob.connectionEnabled);
        showNotification(progressId);
    }

    void notifyReadability(@Nullable String progressId) {
        if (!mSyncProgresses.containsKey(progressId)) {
            return;
        }
        mSyncProgresses.get(progressId).finishReadability();
        showNotification(progressId);
    }

    void notifyArticle(String progressId, int newProgress) {
        if (!mSyncProgresses.containsKey(progressId)) {
            return;
        }
        mSyncProgresses.get(progressId).updateArticle(newProgress, 100);
        showNotification(progressId);
    }

    private void showNotification(String progressId) {
        SyncProgress syncProgress = mSyncProgresses.get(progressId);
        if (syncProgress == null) {
            return;
        }
        if (mListener != null) {
            if (syncProgress.getProgress() >= syncProgress.getMax()) { // TODO may never done
                mListener.onDone(progressId);
                mListener = null;
            }
        }
        if (syncProgress.getProgress() >= syncProgress.getMax()) {
            mSyncProgresses.remove(progressId);
            if (mJob.notificationEnabled) {
                mNotificationManager.cancel(Integer.valueOf(progressId));
            }
        } else if (mJob.notificationEnabled) {
            mNotificationManager.notify(Integer.valueOf(progressId), mNotificationBuilder
                    .setContentTitle(mContext.getString(R.string.download_in_progress))
                    .setContentText(syncProgress.title)
                    .setContentIntent(getItemActivity(progressId))
                    .setProgress(syncProgress.getMax(), syncProgress.getProgress(), false)
                    .setSortKey(progressId)
                    .build());
        }
    }

    private PendingIntent getItemActivity(String itemId) {
        return PendingIntent.getActivity(mContext, 0,
                new Intent(Intent.ACTION_VIEW)
                        .setData(AppUtils.createItemUri(itemId))
                        .putExtra(ItemActivity.EXTRA_CACHE_MODE, ItemManager.MODE_CACHE)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_ONE_SHOT);
    }

    static class SyncProgress {
        private final String id;
        private Boolean self;
        private int totalKids, finishedKids, webProgress, maxWebProgress;
        private Boolean readability;
        String title;

        @Synthetic
        SyncProgress(Job job) {
            this.id = job.id;
            if (job.commentsEnabled) {
                totalKids = 1;
            }
            if (job.articleEnabled) {
                maxWebProgress = 100;
            }
            if (job.readabilityEnabled) {
                readability = false;
            }
        }

        int getMax() {
            return 1 + totalKids + (readability != null ? 1 : 0) + maxWebProgress;
        }

        int getProgress() {
            return (self != null ? 1 : 0) + finishedKids + (readability != null && readability ? 1 :0) + webProgress;
        }

        @Synthetic
        void finishItem(@NonNull String id, @Nullable HackerNewsItem item,
                        boolean kidsEnabled, boolean readabilityEnabled) {
            if (TextUtils.equals(id, this.id)) {
                finishSelf(item, kidsEnabled, readabilityEnabled);
            } else {
                finishKid();
            }
        }

        @Synthetic
        void finishReadability() {
            readability = true;
        }

        @Synthetic
        void updateArticle(int webProgress, int maxWebProgress) {
            this.webProgress = webProgress;
            this.maxWebProgress = maxWebProgress;
        }

        private void finishSelf(@Nullable HackerNewsItem item, boolean kidsEnabled,
                                boolean readabilityEnabled) {
            self = item != null;
            title = item != null ? item.getTitle() : null;
            if (kidsEnabled && item != null && item.getKids() != null) {
                // fetch recursively but only notify for immediate children
                totalKids = item.getKids().length;
            } else {
                totalKids = 0;
            }
            if (readabilityEnabled) {
                readability = false;
            }
        }

        private void finishKid() {
            finishedKids++;
        }
    }

    static class BackgroundThreadExecutor implements Executor {

        @Override
        public void execute(@NonNull Runnable r) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            r.run();
        }
    }

    interface ProgressListener {
        void onDone(String token);
    }

    static class Job {
        String id;
        boolean connectionEnabled;
        boolean readabilityEnabled;
        boolean articleEnabled;
        boolean commentsEnabled;
        boolean notificationEnabled;
    }

    static class JobBuilder {
        private final Job job;

        JobBuilder(String id) {
            job = new Job();
            job.id = id;
        }

        JobBuilder setConnectionEnabled(boolean connectionEnabled) {
            job.connectionEnabled = connectionEnabled;
            return this;
        }

        JobBuilder setReadabilityEnabled(boolean readabilityEnabled) {
            job.readabilityEnabled = readabilityEnabled;
            return this;
        }

        JobBuilder setArticleEnabled(boolean articleEnabled) {
            job.articleEnabled = articleEnabled;
            return this;
        }

        JobBuilder setCommentsEnabled(boolean commentsEnabled) {
            job.commentsEnabled = commentsEnabled;
            return this;
        }

        JobBuilder setNotificationEnabled(boolean notificationEnabled) {
            job.notificationEnabled = notificationEnabled;
            return this;
        }

        Job build() {
            return job;
        }
    }
}

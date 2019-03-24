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
import android.accounts.AccountManager;
import android.app.NotificationChannel;
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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.webkit.WebView;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.BuildConfig;
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
    private static final String NOTIFICATION_GROUP_KEY = "group";
    private static final String SYNC_ACCOUNT_NAME = "Materialistic";
    private static final long TIMEOUT_MILLIS = DateUtils.MINUTE_IN_MILLIS;
    private static final String DOWNLOADS_CHANNEL_ID = "downloads";

    private final HackerNewsClient.RestService mHnRestService;
    private final ReadabilityClient mReadabilityClient;
    private final SharedPreferences mSharedPreferences;
    private final NotificationManager mNotificationManager;
    private final NotificationCompat.Builder mNotificationBuilder;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SyncProgress mSyncProgress;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(DOWNLOADS_CHANNEL_ID,
                    context.getString(R.string.notification_channel_downloads),
                    NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(channel);
            mNotificationBuilder = new NotificationCompat.Builder(context, DOWNLOADS_CHANNEL_ID);
        } else {
            //noinspection deprecation
            mNotificationBuilder = new NotificationCompat.Builder(context);
        }
        mNotificationBuilder
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.ic_notification)
                .setGroup(NOTIFICATION_GROUP_KEY)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setAutoCancel(true);
    }

    @UiThread
    static void scheduleSync(Context context, Job job) {
        if (!Preferences.Offline.isEnabled(context)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !TextUtils.isEmpty(job.id)) {
            JobInfo.Builder builder = new JobInfo.Builder(Long.valueOf(job.id).intValue(),
                    new ComponentName(context.getPackageName(),
                            ItemSyncJobService.class.getName()))
                    .setRequiredNetworkType(Preferences.Offline.isWifiOnly(context) ?
                            JobInfo.NETWORK_TYPE_UNMETERED :
                            JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(job.toPersistableBundle());
            if (Preferences.Offline.currentConnectionEnabled(context)) {
                builder.setOverrideDeadline(0);
            }
            ((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE))
                    .schedule(builder.build());
        } else {
            Bundle extras = new Bundle(job.toBundle());
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            Account syncAccount;
            AccountManager accountManager = AccountManager.get(context);
            Account[] accounts = accountManager.getAccountsByType(BuildConfig.APPLICATION_ID);
            if (accounts.length == 0) {
                syncAccount = new Account(SYNC_ACCOUNT_NAME, BuildConfig.APPLICATION_ID);
                accountManager.addAccountExplicitly(syncAccount, null, null);
            } else {
                syncAccount = accounts[0];
            }
            ContentResolver.requestSync(syncAccount, SyncContentProvider.PROVIDER_AUTHORITY, extras);
        }
    }

    void subscribe(ProgressListener listener) {
        mListener = listener;
    }

    void performSync(@NonNull Job job) {
        // assume that connection wouldn't change until we finish syncing
        mJob = job;
        if (!TextUtils.isEmpty(mJob.id)) {
            Message message = Message.obtain(mHandler, this::stopSync);
            message.what = Integer.valueOf(mJob.id);
            mHandler.sendMessageDelayed(message, TIMEOUT_MILLIS);
            mSyncProgress = new SyncProgress(mJob);
            sync(mJob.id);
        } else {
            syncDeferredItems();
        }
    }

    private void syncDeferredItems() {
        Set<String> itemIds = mSharedPreferences.getAll().keySet();
        for (String itemId : itemIds) {
            scheduleSync(mContext, new JobBuilder(mContext, itemId).setNotificationEnabled(false).build());
        }
    }

    private void sync(String itemId) {
        if (!mJob.connectionEnabled) {
            defer(itemId);
            return;
        }
        HackerNewsItem cachedItem;
        if ((cachedItem = getFromCache(itemId)) != null) {
            sync(cachedItem);
        } else {
            updateProgress();
            // TODO defer on low battery as well?
            mHnRestService.networkItem(itemId).enqueue(new Callback<HackerNewsItem>() {
                @Override
                public void onResponse(Call<HackerNewsItem> call,
                                       retrofit2.Response<HackerNewsItem> response) {
                    HackerNewsItem item;
                    if ((item = response.body()) != null) {
                        sync(item);
                    }
                }

                @Override
                public void onFailure(Call<HackerNewsItem> call, Throwable t) {
                    notifyItem(itemId, null);
                }
            });
        }
    }

    @Synthetic
    void sync(@NonNull HackerNewsItem item) {
        mSharedPreferences.edit().remove(item.getId()).apply();
        notifyItem(item.getId(), item);
        syncReadability(item);
        syncArticle(item);
        syncChildren(item);
    }

    private void syncReadability(@NonNull HackerNewsItem item) {
        if (mJob.readabilityEnabled && item.isStoryType()) {
            final String itemId = item.getId();
            mReadabilityClient.parse(itemId, item.getRawUrl(), content -> notifyReadability());
        }
    }

    private void syncArticle(@NonNull HackerNewsItem item) {
        if (mJob.articleEnabled && item.isStoryType() && !TextUtils.isEmpty(item.getUrl())) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                loadArticle(item);
            } else {
                mContext.startService(new Intent(mContext, WebCacheService.class)
                        .putExtra(WebCacheService.EXTRA_URL, item.getUrl()));
                notifyArticle(100);
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
                notifyArticle(newProgress);
            }
        });
        notifyArticle(0);
        mWebView.loadUrl(item.getUrl());
    }

    private void syncChildren(@NonNull HackerNewsItem item) {
        if (mJob.commentsEnabled && item.getKids() != null) {
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

    @Synthetic
    void notifyItem(@NonNull String id, @Nullable HackerNewsItem item) {
        mSyncProgress.finishItem(id, item,
                mJob.commentsEnabled && mJob.connectionEnabled,
                mJob.readabilityEnabled && mJob.connectionEnabled);
        updateProgress();
    }

    private void notifyReadability() {
        mSyncProgress.finishReadability();
        updateProgress();
    }

    @Synthetic
    void notifyArticle(int newProgress) {
        mSyncProgress.updateArticle(newProgress, 100);
        updateProgress();
    }

    private void updateProgress() {
        if (mSyncProgress.getProgress() >= mSyncProgress.getMax()) { // TODO may never done
            finish(); // TODO finish once only
        } else if (mJob.notificationEnabled) {
            showProgress();
        }
    }

    private void showProgress() {
        mNotificationManager.notify(Integer.valueOf(mJob.id), mNotificationBuilder
                .setContentTitle(mSyncProgress.title)
                .setContentText(mContext.getString(R.string.download_in_progress))
                .setContentIntent(getItemActivity(mJob.id))
                .setOnlyAlertOnce(true)
                .setProgress(mSyncProgress.getMax(), mSyncProgress.getProgress(), false)
                .setSortKey(mJob.id)
                .build());
    }

    private void finish() {
        if (mListener != null) {
            mListener.onDone(mJob.id);
            mListener = null;
        }
        stopSync();
    }

    void stopSync() {
        // TODO
        mJob.connectionEnabled = false;
        int id = Integer.valueOf(mJob.id);
        mNotificationManager.cancel(id);
        mHandler.removeMessages(id);
    }

    private PendingIntent getItemActivity(String itemId) {
        return PendingIntent.getActivity(mContext, 0,
                new Intent(Intent.ACTION_VIEW)
                        .setData(AppUtils.createItemUri(itemId))
                        .putExtra(ItemActivity.EXTRA_CACHE_MODE, ItemManager.MODE_CACHE)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_ONE_SHOT);
    }

    private static class SyncProgress {
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

    private static class BackgroundThreadExecutor implements Executor {

        @Synthetic BackgroundThreadExecutor() { }

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
        private static final String EXTRA_ID = "extra:id";
        private static final String EXTRA_CONNECTION_ENABLED = "extra:connectionEnabled";
        private static final String EXTRA_READABILITY_ENABLED = "extra:readabilityEnabled";
        private static final String EXTRA_ARTICLE_ENABLED = "extra:articleEnabled";
        private static final String EXTRA_COMMENTS_ENABLED = "extra:commentsEnabled";
        private static final String EXTRA_NOTIFICATION_ENABLED = "extra:notificationEnabled";
        final String id;
        boolean connectionEnabled;
        boolean readabilityEnabled;
        boolean articleEnabled;
        boolean commentsEnabled;
        boolean notificationEnabled;

        Job(String id) {
            this.id = id;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        Job(PersistableBundle bundle) {
            id = bundle.getString(EXTRA_ID);
            connectionEnabled = bundle.getInt(EXTRA_CONNECTION_ENABLED) == 1;
            readabilityEnabled = bundle.getInt(EXTRA_READABILITY_ENABLED) == 1;
            articleEnabled = bundle.getInt(EXTRA_ARTICLE_ENABLED) == 1;
            commentsEnabled = bundle.getInt(EXTRA_COMMENTS_ENABLED) == 1;
            notificationEnabled = bundle.getInt(EXTRA_NOTIFICATION_ENABLED) == 1;
        }

        Job(Bundle bundle) {
            id = bundle.getString(EXTRA_ID);
            connectionEnabled = bundle.getBoolean(EXTRA_CONNECTION_ENABLED);
            readabilityEnabled = bundle.getBoolean(EXTRA_READABILITY_ENABLED);
            articleEnabled = bundle.getBoolean(EXTRA_ARTICLE_ENABLED);
            commentsEnabled = bundle.getBoolean(EXTRA_COMMENTS_ENABLED);
            notificationEnabled = bundle.getBoolean(EXTRA_NOTIFICATION_ENABLED);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Synthetic PersistableBundle toPersistableBundle() {
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(EXTRA_ID, id);
            bundle.putInt(EXTRA_CONNECTION_ENABLED, connectionEnabled ? 1 : 0);
            bundle.putInt(EXTRA_READABILITY_ENABLED, readabilityEnabled ? 1 : 0);
            bundle.putInt(EXTRA_ARTICLE_ENABLED, articleEnabled ? 1 : 0);
            bundle.putInt(EXTRA_COMMENTS_ENABLED, commentsEnabled ? 1 : 0);
            bundle.putInt(EXTRA_NOTIFICATION_ENABLED, notificationEnabled ? 1 : 0);
            return bundle;
        }

        @Synthetic Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_ID, id);
            bundle.putBoolean(EXTRA_CONNECTION_ENABLED, connectionEnabled);
            bundle.putBoolean(EXTRA_READABILITY_ENABLED, readabilityEnabled);
            bundle.putBoolean(EXTRA_ARTICLE_ENABLED, articleEnabled);
            bundle.putBoolean(EXTRA_COMMENTS_ENABLED, commentsEnabled);
            bundle.putBoolean(EXTRA_NOTIFICATION_ENABLED, notificationEnabled);
            return bundle;
        }
    }

    public static class JobBuilder {
        private final Job job;

        public JobBuilder(Context context, String id) {
            job = new Job(id);
            setConnectionEnabled(Preferences.Offline.currentConnectionEnabled(context));
            setReadabilityEnabled(Preferences.Offline.isReadabilityEnabled(context));
            setArticleEnabled(Preferences.Offline.isArticleEnabled(context));
            setCommentsEnabled(Preferences.Offline.isCommentsEnabled(context));
            setNotificationEnabled(Preferences.Offline.isNotificationEnabled(context));
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

        public JobBuilder setNotificationEnabled(boolean notificationEnabled) {
            job.notificationEnabled = notificationEnabled;
            return this;
        }

        public Job build() {
            return job;
        }
    }
}

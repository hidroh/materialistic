/*
 * Copyright (c) 2015 Ha Duy Trung
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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.CursorWrapper;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.DataModule;
import io.github.hidroh.materialistic.FavoriteActivity;
import io.github.hidroh.materialistic.R;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Data repository for {@link Favorite}
 */
@Singleton
public class FavoriteManager implements LocalItemManager<Favorite> {

    private static final String URI_PATH_ADD = "add";
    private static final String URI_PATH_REMOVE = "remove";
    private static final String URI_PATH_CLEAR = "clear";
    @VisibleForTesting static final String FILE_AUTHORITY = "io.github.hidroh.materialistic.fileprovider";
    private static final String PATH_SAVED = "saved";
    private static final String FILENAME_EXPORT = "materialistic-export.txt";
    private final LocalCache mCache;
    private final Scheduler mIoScheduler;
    private final MaterialisticDatabase.SavedStoriesDao mDao;
    private FavoriteRoomLoader mLoader;
    private Cursor mCursor;
    private final int mNotificationId = Long.valueOf(System.currentTimeMillis()).intValue();
    private final SyncScheduler mSyncScheduler = new SyncScheduler();

    @Inject
    public FavoriteManager(LocalCache cache,
                           @Named(DataModule.IO_THREAD) Scheduler ioScheduler,
                           MaterialisticDatabase.SavedStoriesDao dao) {
        mCache = cache;
        mIoScheduler = ioScheduler;
        mDao = dao;
    }

    @Override
    public int getSize() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    @Override
    public Favorite getItem(int position) {
        return mCursor.moveToPosition(position) ? mCursor.getFavorite() : null;
    }

    @Override
    public void attach(@NonNull Observer observer, String filter) {
        mLoader = new FavoriteRoomLoader(filter, observer);
        mLoader.load();
    }

    @Override
    public void detach() {
        if (mCursor != null) {
            mCursor.close();
        }
        mLoader = null;
    }

    /**
     * Exports all favorites matched given query to file
     * @param context   an instance of {@link android.content.Context}
     * @param query     query to filter stories to be retrieved
     */
    public void export(Context context, String query) {
        Context appContext = context.getApplicationContext();
        notifyExportStart(appContext);
        Observable.defer(() -> Observable.just(query))
                .map(filter -> query(filter))
                .filter(cursor -> cursor != null && cursor.moveToFirst())
                .map(cursor -> {
                    try {
                        return toFile(appContext, new Cursor(cursor));
                    } catch (IOException e) {
                        return null;
                    } finally {
                        cursor.close();
                    }
                })
                .onErrorReturn(throwable -> null)
                .defaultIfEmpty(null)
                .subscribeOn(mIoScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> notifyExportDone(appContext, uri));
    }

    /**
     * Adds given story as favorite
     * @param context   an instance of {@link android.content.Context}
     * @param story     story to be added as favorite
     */
    public void add(Context context, WebItem story) {
        Observable.defer(() -> Observable.just(story))
                .map(item -> {
                    insert(item);
                    return item.getId();
                })
                .map(itemId -> buildAdded().appendPath(story.getId()).build())
                .subscribeOn(mIoScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> MaterialisticDatabase.getInstance(context).setLiveValue(uri));
        mSyncScheduler.scheduleSync(context, story.getId());
    }

    /**
     * Clears all stories matched given query from favorites
     * will be sent upon completion
     * @param context   an instance of {@link android.content.Context}
     * @param query     query to filter stories to be cleared
     */
    public void clear(Context context, String query) {
        Observable.defer(() -> Observable.just(query))
                .map(filter -> deleteMultiple(filter))
                .subscribeOn(mIoScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> MaterialisticDatabase.getInstance(context)
                        .setLiveValue(buildCleared().build()));
    }

    /**
     * Removes story with given ID from favorites
     * upon completion
     * @param context   an instance of {@link android.content.Context}
     * @param itemId    story ID to be removed from favorites
     */
    public void remove(Context context, String itemId) {
        if (itemId == null) {
            return;
        }
        Observable.defer(() -> Observable.just(itemId))
                .map(id -> {
                    delete(id);
                    return id;
                })
                .map(id -> buildRemoved().appendPath(itemId).build())
                .subscribeOn(mIoScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> MaterialisticDatabase.getInstance(context).setLiveValue(uri));
    }

    /**
     * Removes multiple stories with given IDs from favorites
     * be sent upon completion
     * @param context   an instance of {@link android.content.Context}
     * @param itemIds   array of story IDs to be removed from favorites
     */
    public void remove(Context context, Collection<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        Observable.defer(() -> Observable.from(itemIds))
                .subscribeOn(mIoScheduler)
                .map(itemId -> {
                    delete(itemId);
                    return itemId;
                })
                .map(itemId -> buildRemoved().appendPath(itemId).build())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> MaterialisticDatabase.getInstance(context).setLiveValue(uri));
    }

    @WorkerThread
    @NonNull
    Observable<Boolean> check(String itemId) {
        if (TextUtils.isEmpty(itemId)) {
            return Observable.just(false);
        }
        return Observable.just(mCache.isFavorite(itemId));
    }

    @WorkerThread
    private android.database.Cursor query(String filter) {
        if (TextUtils.isEmpty(filter)) {
            return mDao.selectAllToCursor();
        } else {
            return mDao.searchToCursor(filter);
        }
    }

    @WorkerThread
    private void insert(WebItem story) {
        mDao.insert(MaterialisticDatabase.SavedStory.from(story));
        if (mLoader != null) {
            mLoader.load();
        }
    }

    @WorkerThread
    private void delete(String itemId) {
        mDao.deleteByItemId(itemId);
        if (mLoader != null) {
            mLoader.load();
        }
    }

    @WorkerThread
    private int deleteMultiple(String query) {
        int deleted;
        if (TextUtils.isEmpty(query)) {
            deleted = mDao.deleteAll();
        } else {
            deleted = mDao.deleteByTitle(query);
        }
        if (mLoader != null) {
            mLoader.load();
        }
        return deleted;
    }

    @WorkerThread
    private Uri toFile(Context context, Cursor cursor) throws IOException {
        if (cursor.getCount() == 0) {
            return null;
        }
        File dir = new File(context.getFilesDir(), PATH_SAVED);
        if (!dir.exists() && !dir.mkdir()) {
            return null;
        }
        File file = new File(dir, FILENAME_EXPORT);
        if (!file.exists() && !file.createNewFile()) {
            return null;
        }
        BufferedSink bufferedSink = Okio.buffer(Okio.sink(file));
        do {
            Favorite item = cursor.getFavorite();
            bufferedSink.writeUtf8(item.getDisplayedTitle())
                    .writeByte('\n')
                    .writeUtf8(item.getUrl())
                    .writeByte('\n')
                    .writeUtf8(String.format(HackerNewsClient.WEB_ITEM_PATH, item.getId()));
            if (!cursor.isLast()) {
                bufferedSink.writeByte('\n')
                        .writeByte('\n');
            }
        } while (cursor.moveToNext());
        Util.closeQuietly(bufferedSink);
        return getUriForFile(context, file);
    }

    @VisibleForTesting
    protected Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context, FILE_AUTHORITY, file);
    }

    public static boolean isAdded(Uri uri) {
        return uri.toString().startsWith(buildAdded().toString());
    }

    public static boolean isRemoved(Uri uri) {
        return uri.toString().startsWith(buildRemoved().toString());
    }

    public static boolean isCleared(Uri uri) {
        return uri.toString().startsWith(buildCleared().toString());
    }

    private static Uri.Builder buildAdded() {
        return MaterialisticDatabase.URI_SAVED.buildUpon().appendPath(URI_PATH_ADD);
    }

    private static Uri.Builder buildRemoved() {
        return MaterialisticDatabase.URI_SAVED.buildUpon().appendPath(URI_PATH_REMOVE);
    }

    private static Uri.Builder buildCleared() {
        return MaterialisticDatabase.URI_SAVED.buildUpon().appendPath(URI_PATH_CLEAR);
    }

    private void notifyExportStart(Context context) {
        NotificationManagerCompat.from(context)
                .notify(mNotificationId, createNotificationBuilder(context)
                        .setContentIntent(PendingIntent.getActivity(context, 0,
                                new Intent(context, FavoriteActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .setProgress(0, 0, true)
                        .build());
    }

    private void notifyExportDone(Context context, Uri uri) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.cancel(mNotificationId);
        if (uri == null) {
            return;
        }
        context.grantUriPermission(context.getPackageName(), uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        manager.notify(mNotificationId, createNotificationBuilder(context)
                .setContentText(context.getString(R.string.export_notification))
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        AppUtils.makeSendIntentChooser(context, uri)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[0])
                .build());
    }

    private NotificationCompat.Builder createNotificationBuilder(Context context) {
        // TODO specify notification channel
        return new NotificationCompat.Builder(context)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.export_saved_stories))
                .setAutoCancel(true);
    }

    /**
     * A cursor wrapper to retrieve associated {@link Favorite}
     */
    static class Cursor extends CursorWrapper {
        Cursor(android.database.Cursor cursor) {
            super(cursor);
        }

        Favorite getFavorite() {
            final String itemId = getString(getColumnIndexOrThrow(MaterialisticDatabase.FavoriteEntry.COLUMN_NAME_ITEM_ID));
            final String url = getString(getColumnIndexOrThrow(MaterialisticDatabase.FavoriteEntry.COLUMN_NAME_URL));
            final String title = getString(getColumnIndexOrThrow(MaterialisticDatabase.FavoriteEntry.COLUMN_NAME_TITLE));
            final String time = getString(getColumnIndexOrThrow(MaterialisticDatabase.FavoriteEntry.COLUMN_NAME_TIME));
            return new Favorite(itemId, url, title, Long.valueOf(time));
        }
    }

    class FavoriteRoomLoader {
        private final String mQuery;
        private final Observer mObserver;

        FavoriteRoomLoader(@Nullable String query, Observer observer) {
            mQuery = query;
            mObserver = observer;
        }

        @AnyThread
        void load() {
            Observable.defer(() -> Observable.just(mQuery))
                    .map(queryString -> query(queryString))
                    .subscribeOn(mIoScheduler)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(cursor -> {
                        if (cursor != null) {
                            mCursor = new Cursor(cursor);
                        } else {
                            mCursor = null;
                        }
                        mObserver.onChanged();
                    });
        }
    }
}

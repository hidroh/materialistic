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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Data repository for {@link Favorite}
 */
public class FavoriteManager implements LocalItemManager<Favorite> {

    public static final int LOADER = 0;
    /**
     * {@link android.content.Intent#getAction()} for broadcasting getting favorites matching query
     */
    public static final String ACTION_GET = FavoriteManager.class.getName() + ".ACTION_GET";
    /**
     * {@link android.os.Bundle} key for {@link #ACTION_GET} that contains {@link ArrayList} of
     * {@link Favorite}
     */
    public static final String ACTION_GET_EXTRA_DATA = ACTION_GET + ".EXTRA_DATA";
    private static final String URI_PATH_ADD = "add";
    private static final String URI_PATH_REMOVE = "remove";
    private static final String URI_PATH_CLEAR = "clear";
    private final Scheduler mIoScheduler;
    private Cursor mCursor;

    @Override
    public int getSize() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    @Override
    public Favorite getItem(int position) {
        return mCursor.moveToPosition(position) ? mCursor.getFavorite() : null;
    }

    @Override
    public void attach(@NonNull Context context, @NonNull LoaderManager loaderManager,
                       @NonNull Observer observer, String filter) {
        loaderManager.restartLoader(FavoriteManager.LOADER, null,
                new LoaderManager.LoaderCallbacks<android.database.Cursor>() {
                    @Override
                    public Loader<android.database.Cursor> onCreateLoader(int id, Bundle args) {
                        if (!TextUtils.isEmpty(filter)) {
                            return new FavoriteManager.CursorLoader(context, filter);
                        }
                        return new FavoriteManager.CursorLoader(context);
                    }

                    @Override
                    public void onLoadFinished(Loader<android.database.Cursor> loader,
                                               android.database.Cursor data) {
                        if (data != null) {
                            data.setNotificationUri(context.getContentResolver(),
                                    MaterialisticProvider.URI_FAVORITE);
                            mCursor = new Cursor(data);
                        } else {
                            mCursor = null;
                        }
                        observer.onChanged();
                    }

                    @Override
                    public void onLoaderReset(Loader<android.database.Cursor> loader) {
                        mCursor = null;
                        observer.onChanged();
                    }
                });
    }

    @Override
    public void detach() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Inject
    public FavoriteManager(Scheduler ioScheduler) {
        mIoScheduler = ioScheduler;
    }

    /**
     * Gets all favorites matched given query, a {@link #ACTION_GET} broadcast will be sent upon
     * completion
     * @param context   an instance of {@link android.content.Context}
     * @param query     query to filter stories to be retrieved
     * @see #makeGetIntentFilter()
     */
    public void get(Context context, String query) {
        Observable.defer(() -> Observable.just(query))
                .map(filter -> query(context, filter))
                .filter(cursor -> cursor != null && cursor.moveToFirst())
                .map(cursor -> {
                    ArrayList<Favorite> favorites = new ArrayList<>(cursor.getCount());
                    Cursor favoriteCursor = new Cursor(cursor);
                    do {
                        favorites.add(favoriteCursor.getFavorite());
                    } while (favoriteCursor.moveToNext());
                    favoriteCursor.close();
                    return favorites;
                })
                .defaultIfEmpty(new ArrayList<>())
                .map(FavoriteManager::makeGetBroadcastIntent)
                .subscribeOn(mIoScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(LocalBroadcastManager.getInstance(context)::sendBroadcast);
    }

    /**
     * Adds given story as favorite
     * @param context   an instance of {@link android.content.Context}
     * @param story     story to be added as favorite
     */
    public void add(Context context, WebItem story) {
        Observable.defer(() -> Observable.just(story))
                .map(item -> {
                    insert(context, item);
                    return item.getId();
                })
                .map(itemId -> buildAdded().appendPath(story.getId()).build())
                .subscribeOn(mIoScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> context.getContentResolver().notifyChange(uri, null));
        ItemSyncAdapter.initSync(context, story.getId());
    }

    /**
     * Clears all stories matched given query from favorites
     * will be sent upon completion
     * @param context   an instance of {@link android.content.Context}
     * @param query     query to filter stories to be cleared
     */
    public void clear(Context context, String query) {
        Observable.defer(() -> Observable.just(query))
                .map(filter -> deleteMultiple(context, filter))
                .subscribeOn(mIoScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> context.getContentResolver()
                        .notifyChange(buildCleared().build(), null));
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
                    delete(context, id);
                    return id;
                })
                .map(id -> buildRemoved().appendPath(itemId).build())
                .subscribeOn(mIoScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> context.getContentResolver().notifyChange(uri, null));
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
                    delete(context, itemId);
                    return itemId;
                })
                .map(itemId -> buildRemoved().appendPath(itemId).build())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> context.getContentResolver().notifyChange(uri, null));
    }

    @WorkerThread
    @NonNull
    Observable<Boolean> check(ContentResolver contentResolver, String itemId) {
        if (TextUtils.isEmpty(itemId)) {
            return Observable.just(false);
        }
        android.database.Cursor cursor = contentResolver
                .query(MaterialisticProvider.URI_FAVORITE,
                        null,
                        MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                        new String[]{itemId},
                        null);
        boolean result = false;
        if (cursor != null) {
            result = cursor.getCount() > 0;
            cursor.close();
        }
        return Observable.just(result);
    }

    @WorkerThread
    private android.database.Cursor query(Context context, String filter) {
        String selection;
        String[] selectionArgs;
        if (TextUtils.isEmpty(filter)) {
            selection = null;
            selectionArgs = null;

        } else {
            selection = MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE + " LIKE ?";
            selectionArgs = new String[]{"%" + filter + "%"};
        }
        return context.getContentResolver()
                .query(MaterialisticProvider.URI_FAVORITE,
                        null,
                        selection,
                        selectionArgs,
                        null);
    }

    @WorkerThread
    private Uri insert(Context context, WebItem story) {
        ContentValues cv = new ContentValues();
        cv.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID, story.getId());
        cv.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_URL, story.getUrl());
        cv.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE, story.getDisplayedTitle());
        cv.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TIME,
                story instanceof Favorite ?
                        String.valueOf(((Favorite) story).getTime()) :
                        String.valueOf(System.currentTimeMillis()));
        return context.getContentResolver().
                insert(MaterialisticProvider.URI_FAVORITE, cv);
    }

    @WorkerThread
    private int delete(Context context, String itemId) {
        return context.getContentResolver()
                .delete(MaterialisticProvider.URI_FAVORITE,
                        MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                        new String[]{itemId});
    }

    @WorkerThread
    private int deleteMultiple(Context context, String query) {
        String selection;
        String[] selectionArgs;
        if (TextUtils.isEmpty(query)) {
            selection = null;
            selectionArgs = null;
        } else {
            selection = MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%"};
        }
        return context.getContentResolver()
                .delete(MaterialisticProvider.URI_FAVORITE, selection, selectionArgs);
    }

    /**
     * Creates an intent filter for get action broadcast
     * @return get intent filter
     * @see #get(android.content.Context, String)
     */
    public static IntentFilter makeGetIntentFilter() {
        return new IntentFilter(ACTION_GET);
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
        return MaterialisticProvider.URI_FAVORITE.buildUpon().appendPath(URI_PATH_ADD);
    }

    private static Uri.Builder buildRemoved() {
        return MaterialisticProvider.URI_FAVORITE.buildUpon().appendPath(URI_PATH_REMOVE);
    }

    private static Uri.Builder buildCleared() {
        return MaterialisticProvider.URI_FAVORITE.buildUpon().appendPath(URI_PATH_CLEAR);
    }

    private static Intent makeGetBroadcastIntent(ArrayList<Favorite> favorites) {
        final Intent intent = new Intent(ACTION_GET);
        intent.putExtra(ACTION_GET_EXTRA_DATA, favorites);
        return intent;
    }

    /**
     * A cursor wrapper to retrieve associated {@link Favorite}
     */
    static class Cursor extends CursorWrapper {
        Cursor(android.database.Cursor cursor) {
            super(cursor);
        }

        Favorite getFavorite() {
            final String itemId = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID));
            final String url = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_URL));
            final String title = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE));
            final String time = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TIME));
            return new Favorite(itemId, url, title, Long.valueOf(time));
        }
    }

    /**
     * A {@link android.support.v4.content.CursorLoader} to query {@link Favorite}
     */
    static class CursorLoader extends android.support.v4.content.CursorLoader {
        /**
         * Constructs a cursor loader to query all {@link Favorite}
         * @param context    an instance of {@link android.content.Context}
         */
        CursorLoader(Context context) {
            super(context, MaterialisticProvider.URI_FAVORITE, null, null, null, null);
        }

        /**
         * Constructs a cursor loader to query {@link Favorite}
         * with title matching given query
         * @param context   an instance of {@link android.content.Context}
         * @param query     query to filter
         */
        CursorLoader(Context context, String query) {
            super(context, MaterialisticProvider.URI_FAVORITE, null,
                    MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE + " LIKE ?",
                    new String[]{"%" + query + "%"}, null);
        }
    }
}

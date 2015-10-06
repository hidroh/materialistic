package io.github.hidroh.materialistic.data;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.util.Set;

import io.github.hidroh.materialistic.R;

/**
 * Data repository for {@link io.github.hidroh.materialistic.data.FavoriteManager.Favorite}
 */
public class FavoriteManager {

    public static final int LOADER = 0;
    /**
     * {@link android.content.Intent#getAction()} for broadcasting clearing all favorites
     */
    public static final String ACTION_CLEAR = FavoriteManager.class.getName() + ".ACTION_CLEAR";
    /**
     * {@link android.content.Intent#getAction()} for broadcasting getting favorites matching query
     */
    public static final String ACTION_GET = FavoriteManager.class.getName() + ".ACTION_GET";
    /**
     * {@link android.content.Intent#getAction()} for broadcasting adding favorites
     */
    public static final String ACTION_ADD = FavoriteManager.class.getName() + ".ACTION_ADD";
    /**
     * {@link android.content.Intent#getAction()} for broadcasting removing favorites
     */
    public static final String ACTION_REMOVE = FavoriteManager.class.getName() + ".ACTION_REMOVE";
    /**
     * {@link android.os.Bundle} key for {@link #ACTION_GET} that contains array of
     * {@link io.github.hidroh.materialistic.data.FavoriteManager.Favorite}
     */
    public static final String ACTION_GET_EXTRA_DATA = ACTION_GET + ".EXTRA_DATA";
    /**
     * {@link android.os.Bundle} key for {@link #ACTION_ADD} that contains added favorite item ID string
     */
    public static final String ACTION_ADD_EXTRA_DATA = ACTION_ADD + ".EXTRA_DATA";
    /**
     * {@link android.os.Bundle} key for {@link #ACTION_REMOVE} that contains array of
     * {@link io.github.hidroh.materialistic.data.FavoriteManager.Favorite}
     */
    public static final String ACTION_REMOVE_EXTRA_DATA = ACTION_REMOVE + ".EXTRA_DATA";

    /**
     * Gets all favorites matched given query, a {@link #ACTION_GET} broadcast will be sent upon
     * completion
     * @param context   an instance of {@link android.content.Context}
     * @param query     query to filter stories to be retrieved
     * @see #makeGetIntentFilter()
     */
    public void get(Context context, String query) {
        final String selection;
        final String[] selectionArgs;
        if (TextUtils.isEmpty(query)) {
            selection = null;
            selectionArgs = null;

        } else {
            selection = MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%"};
        }

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        new FavoriteHandler(context.getContentResolver(), new FavoriteCallback() {
            @Override
            void onQueryComplete(Favorite[] favorites) {
                broadcastManager.sendBroadcast(makeGetBroadcastIntent(favorites));
            }
        }).startQuery(0, null, MaterialisticProvider.URI_FAVORITE,
                null, selection, selectionArgs, null);
    }

    /**
     * Adds given story as favorite, a {@link #ACTION_ADD} broadcast will be sent upon completion
     * @param context   an instance of {@link android.content.Context}
     * @param story     story to be added as favorite
     * @see #makeAddIntentFilter()
     */
    public void add(Context context, final ItemManager.WebItem story) {
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID, story.getId());
        contentValues.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_URL, story.getUrl());
        contentValues.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE, story.getDisplayedTitle());
        contentValues.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TIME, String.valueOf(System.currentTimeMillis()));
        new FavoriteHandler(context.getContentResolver(), new FavoriteCallback() {
            @Override
            void onInsertComplete() {
                broadcastManager.sendBroadcast(makeAddBroadcastIntent(story.getId()));
            }
        }).startInsert(0, story.getId(), MaterialisticProvider.URI_FAVORITE, contentValues);
    }

    /**
     * Clears all stories matched given query from favorites, a {@link #ACTION_CLEAR} broadcast
     * will be sent upon completion
     * @param context   an instance of {@link android.content.Context}
     * @param query     query to filter stories to be cleared
     * @see #makeClearIntentFilter()
     */
    public void clear(Context context, String query) {
        final String selection;
        final String[] selectionArgs;
        if (TextUtils.isEmpty(query)) {
            selection = null;
            selectionArgs = null;

        } else {
            selection = MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%"};
        }

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        new FavoriteHandler(context.getContentResolver(), new FavoriteCallback() {
            @Override
            void onClearComplete() {
                broadcastManager.sendBroadcast(makeClearBroadcastIntent());
            }
        }).startDelete(0, null, MaterialisticProvider.URI_FAVORITE, selection, selectionArgs);
    }

    /**
     * Checks if a story with given ID is a favorite
     * @param context   an instance of {@link android.content.Context}
     * @param itemId    story ID to check
     * @param callbacks listener to be informed upon checking completed
     */
    public void check(Context context, final String itemId, final OperationCallbacks callbacks) {
        if (itemId == null) {
            return;
        }

        if (callbacks == null) {
            return;
        }

        new FavoriteHandler(context.getContentResolver(), new FavoriteCallback() {
            @Override
            void onCheckComplete(boolean isFavorite) {
                callbacks.onCheckComplete(isFavorite);
            }
        }).startQuery(0, itemId, MaterialisticProvider.URI_FAVORITE, null,
                MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                new String[]{itemId}, null);
    }

    /**
     * Removes story with given ID from favorites, a {@link #ACTION_REMOVE} broadcast will be sent
     * upon completion
     * @param context   an instance of {@link android.content.Context}
     * @param itemId    story ID to be removed from favorites
     * @see #makeRemoveIntentFilter()
     */
    public void remove(Context context, final String itemId) {
        if (itemId == null) {
            return;
        }

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        new FavoriteHandler(context.getContentResolver(), new FavoriteCallback() {
            @Override
            void onDeleteComplete() {
                broadcastManager.sendBroadcast(makeRemoveBroadcastIntent(itemId));
            }
        }).startDelete(0, itemId, MaterialisticProvider.URI_FAVORITE,
                MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                new String[]{itemId});
    }

    /**
     * Removes multiple stories with given IDs from favorites, a {@link #ACTION_CLEAR} broadcast will
     * be sent upon completion
     * @param context   an instance of {@link android.content.Context}
     * @param itemIds   array of story IDs to be removed from favorites
     * @see #makeClearIntentFilter()
     */
    public void remove(Context context, Set<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }

        final ContentResolver contentResolver = context.getContentResolver();
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        new AsyncTask<String, Integer, Integer>() {
            @Override
            protected Integer doInBackground(String... params) {
                int deleted = 0;
                for (String param : params) {
                    deleted += contentResolver.delete(MaterialisticProvider.URI_FAVORITE,
                            MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                            new String[]{param});
                }

                return deleted;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                broadcastManager.sendBroadcast(makeClearBroadcastIntent());
            }
        }.execute(itemIds.toArray(new String[itemIds.size()]));
    }

    /**
     * Creates an intent filter for clear action broadcast
     * @return clear intent filter
     * @see #remove(android.content.Context, java.util.Set)
     * @see #clear(android.content.Context, String)
     */
    public static IntentFilter makeClearIntentFilter() {
        return new IntentFilter(ACTION_CLEAR);
    }

    /**
     * Creates an intent filter for get action broadcast
     * @return get intent filter
     * @see #get(android.content.Context, String)
     */
    public static IntentFilter makeGetIntentFilter() {
        return new IntentFilter(ACTION_GET);
    }

    /**
     * Creates an intent filter for add action broadcast
     * @return add intent filter
     * @see #add(android.content.Context, io.github.hidroh.materialistic.data.ItemManager.WebItem)
     */
    public static IntentFilter makeAddIntentFilter() {
        return new IntentFilter(ACTION_ADD);
    }

    /**
     * Creates an intent filter for remove action broadcast
     * @return remove intent filter
     * @see #remove(android.content.Context, String)
     */
    public static IntentFilter makeRemoveIntentFilter() {
        return new IntentFilter(ACTION_REMOVE);
    }

    private static Intent makeClearBroadcastIntent() {
        return new Intent(ACTION_CLEAR);
    }

    private static Intent makeGetBroadcastIntent(Favorite[] favorites) {
        final Intent intent = new Intent(ACTION_GET);
        intent.putExtra(ACTION_GET_EXTRA_DATA, favorites);
        return intent;
    }

    private static Intent makeAddBroadcastIntent(String itemId) {
        final Intent intent = new Intent(ACTION_ADD);
        intent.putExtra(ACTION_ADD_EXTRA_DATA, itemId);
        return intent;
    }

    private static Intent makeRemoveBroadcastIntent(String itemId) {
        final Intent intent = new Intent(ACTION_REMOVE);
        intent.putExtra(ACTION_REMOVE_EXTRA_DATA, itemId);
        return intent;
    }

    /**
     * Represents a favorite item
     */
    public static class Favorite implements ItemManager.WebItem {
        private String itemId;
        private String url;
        private String title;
        private long time;

        public static final Creator<Favorite> CREATOR = new Creator<Favorite>() {
            @Override
            public Favorite createFromParcel(Parcel source) {
                return new Favorite(source);
            }

            @Override
            public Favorite[] newArray(int size) {
                return new Favorite[size];
            }
        };

        private Favorite(String itemId, String url, String title, long time) {
            this.itemId = itemId;
            this.url = url;
            this.title = title;
            this.time = time;
        }

        private Favorite(Parcel source) {
            itemId = source.readString();
            url = source.readString();
            title = source.readString();
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public boolean isShareable() {
            return true;
        }

        @Override
        public String getId() {
            return itemId;
        }

        @Override
        public String getDisplayedTitle() {
            return title;
        }

        @Override
        public Spannable getDisplayedTime(Context context, boolean abbreviate) {
            return new SpannableString(context.getString(R.string.saved,
                    DateUtils.getRelativeDateTimeString(context, time,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.YEAR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_MONTH)));
        }

        @Override
        public String getSource() {
            return TextUtils.isEmpty(url) ? null : Uri.parse(url).getHost();
        }

        @NonNull
        @Override
        public String getType() {
            // TODO treating all saved items as stories for now
            return STORY_TYPE;
        }

        @Override
        public String toString() {
            return String.format("%s - %s", title, url);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(itemId);
            dest.writeString(url);
            dest.writeString(title);
        }
    }

    /**
     * A cursor wrapper to retrieve associated {@link io.github.hidroh.materialistic.data.FavoriteManager.Favorite}
     */
    public static class Cursor extends CursorWrapper {
        public Cursor(android.database.Cursor cursor) {
            super(cursor);
        }

        public Favorite getFavorite() {
            final String itemId = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID));
            final String url = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_URL));
            final String title = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE));
            final String time = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TIME));
            return new Favorite(itemId, url, title, Long.valueOf(time));
        }
    }

    /**
     * A {@link android.support.v4.content.CursorLoader} to query {@link io.github.hidroh.materialistic.data.FavoriteManager.Favorite}
     */
    public static class CursorLoader extends android.support.v4.content.CursorLoader {
        /**
         * Constructs a cursor loader to query all {@link io.github.hidroh.materialistic.data.FavoriteManager.Favorite}
         * @param context    an instance of {@link android.content.Context}
         */
        public CursorLoader(Context context) {
            super(context, MaterialisticProvider.URI_FAVORITE, null, null, null, null);
        }

        /**
         * Constructs a cursor loader to query {@link io.github.hidroh.materialistic.data.FavoriteManager.Favorite}
         * with title matching given query
         * @param context   an instance of {@link android.content.Context}
         * @param query     query to filter
         */
        public CursorLoader(Context context, String query) {
            super(context, MaterialisticProvider.URI_FAVORITE, null,
                    MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE + " LIKE ?",
                    new String[]{"%" + query + "%"}, null);
        }
    }

    /**
     * Callback interface for asynchronous favorite CRUD operations
     */
    public static abstract class OperationCallbacks {
        /**
         * Fired when checking of favorite status is completed
         * @param isFavorite    true if is favorite, false otherwise
         */
        public void onCheckComplete(boolean isFavorite) { }
    }

    private static class FavoriteHandler extends AsyncQueryHandler {
        private FavoriteCallback mCallback;

        public FavoriteHandler(ContentResolver cr, FavoriteCallback callback) {
            super(cr);
            mCallback = callback;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, android.database.Cursor cursor) {
            if (cursor == null) {
                mCallback = null;
                return;
            }
            // cookie represents itemId
            if (cookie != null) {
                mCallback.onCheckComplete(cursor.getCount() > 0);
            } else {
                Favorite[] favorites = new Favorite[cursor.getCount()];
                int count = 0;
                Cursor favoriteCursor = new Cursor(cursor);
                boolean any = favoriteCursor.moveToFirst();
                if (any) {
                    do {
                        favorites[count] = favoriteCursor.getFavorite();
                        count++;
                    } while (favoriteCursor.moveToNext());

                }
                mCallback.onQueryComplete(favorites);
            }
            mCallback = null;
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            mCallback.onInsertComplete();
            mCallback = null;
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            // cookie represents itemId
            if (cookie != null) {
                mCallback.onDeleteComplete();
            } else {
                mCallback.onClearComplete();
            }
            mCallback = null;
        }
    }

    private static abstract class FavoriteCallback {
        void onQueryComplete(Favorite[] favorites) {}
        void onCheckComplete(boolean isFavorite) {}
        void onInsertComplete() {}
        void onClearComplete() {}
        void onDeleteComplete() {}
    }
}

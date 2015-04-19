package io.github.hidroh.materialistic.data;

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

/**
 * Data repository for session state
 */
public class SessionManager {

    /**
     * {@link android.content.Intent#getAction()} for broadcasting adding viewed item
     */
    public static final String ACTION_ADD = SessionManager.class.getName() + ".ACTION_ADD";
    /**
     * {@link android.os.Bundle} key for {@link #ACTION_ADD} that contains added viewed item ID string
     */
    public static final String ACTION_ADD_EXTRA_DATA = ACTION_ADD + ".EXTRA_DATA";

    /**
     * Checks if an item has been viewed previously
     * @param context   an instance of {@link Context}
     * @param itemId    item ID to check
     * @param callbacks listener to be informed upon checking completed
     */
    public void isViewed(Context context, final String itemId, final OperationCallbacks callbacks) {
        if (TextUtils.isEmpty(itemId)) {
            return;
        }

        if (callbacks == null) {
            return;
        }

        new AsyncQueryHandler(context.getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, android.database.Cursor cursor) {
                super.onQueryComplete(token, cookie, cursor);
                if (cookie == null) {
                    return;
                }

                if (itemId.equals(cookie)) {
                    callbacks.onCheckComplete(cursor.getCount() > 0);
                }
            }
        }.startQuery(0, itemId, MaterialisticProvider.URI_VIEWED, null,
                MaterialisticProvider.ViewedEntry.COLUMN_NAME_ITEM_ID + " = ?",
                new String[]{itemId}, null);
    }

    /**
     * Marks an item as already being viewed
     * @param context   an instance of {@link Context}
     * @param itemId    item ID that has been viewed
     */
    public void view(Context context, final String itemId) {
        if (TextUtils.isEmpty(itemId)) {
            return;
        }

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MaterialisticProvider.ViewedEntry.COLUMN_NAME_ITEM_ID, itemId);
        new AsyncQueryHandler(context.getContentResolver()) {
            @Override
            protected void onInsertComplete(int token, Object cookie, Uri uri) {
                super.onInsertComplete(token, cookie, uri);
                if (cookie == null || itemId == null) {
                    return;
                }

                if (cookie.equals(itemId)) {
                    broadcastManager.sendBroadcast(makeAddBroadcastIntent(itemId));
                }
            }
        }.startInsert(0, itemId, MaterialisticProvider.URI_VIEWED, contentValues);
    }

    /**
     * Creates an {@link IntentFilter} for new item viewed event
     * @return  item viewed added intent filter
     */
    public static IntentFilter makeAddIntentFilter() {
        return new IntentFilter(ACTION_ADD);
    }

    private static Intent makeAddBroadcastIntent(String itemId) {
        final Intent intent = new Intent(ACTION_ADD);
        intent.putExtra(ACTION_ADD_EXTRA_DATA, itemId);
        return intent;
    }

    /**
     * Callback interface for asynchronous session operations
     */
    public static abstract class OperationCallbacks {
        /**
         * Fired when checking of view status is completed
         * @param isViewed  true if is viewed, false otherwise
         */
        public void onCheckComplete(boolean isViewed) { }
    }

}

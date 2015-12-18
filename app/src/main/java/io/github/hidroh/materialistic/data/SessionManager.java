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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
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
        new SessionHandler(context.getContentResolver(), itemId, new SessionCallback() {
            @Override
            public void onQueryComplete(boolean isViewed) {
                callbacks.onCheckComplete(isViewed);
            }
        }).startQuery(0, itemId, MaterialisticProvider.URI_VIEWED, null,
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
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MaterialisticProvider.ViewedEntry.COLUMN_NAME_ITEM_ID, itemId);
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        new SessionHandler(context.getContentResolver(), itemId, new SessionCallback() {
            @Override
            public void onInsertComplete() {
                final Intent intent = new Intent(ACTION_ADD);
                intent.putExtra(ACTION_ADD_EXTRA_DATA, itemId);
                broadcastManager.sendBroadcast(intent);
            }
        }).startInsert(0, itemId, MaterialisticProvider.URI_VIEWED, contentValues);
    }

    /**
     * Creates an {@link IntentFilter} for new item viewed event
     * @return  item viewed added intent filter
     */
    public static IntentFilter makeAddIntentFilter() {
        return new IntentFilter(ACTION_ADD);
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

    private static class SessionHandler extends AsyncQueryHandler {
        private final String mItemId;
        private SessionCallback mCallback;

        public SessionHandler(ContentResolver cr, @NonNull String itemId,
                              @NonNull SessionCallback callback) {
            super(cr);
            mItemId = itemId;
            mCallback = callback;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            if (cookie == null) {
                mCallback = null;
                return;
            }
            if (cookie.equals(mItemId)) {
                mCallback.onQueryComplete(cursor.getCount() > 0);
                mCallback = null;
            }
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            super.onInsertComplete(token, cookie, uri);
            if (cookie == null) {
                mCallback = null;
                return;
            }
            if (cookie.equals(mItemId)) {
                mCallback.onInsertComplete();
                mCallback = null;
            }
        }
    }

    private static abstract class SessionCallback {
        void onQueryComplete(boolean isViewed) {}
        void onInsertComplete() {}
    }
}

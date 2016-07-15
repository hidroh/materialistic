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
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import rx.Observable;

/**
 * Data repository for session state
 */
public class SessionManager {

    @WorkerThread
    @NonNull
    Observable<Boolean> isViewed(ContentResolver contentResolver, String itemId) {
        if (TextUtils.isEmpty(itemId)) {
            return Observable.just(false);
        }
        Cursor cursor = contentResolver.query(MaterialisticProvider.URI_VIEWED, null,
                MaterialisticProvider.ViewedEntry.COLUMN_NAME_ITEM_ID + " = ?",
                new String[]{itemId}, null);
        boolean result = false;
        if (cursor != null) {
            result = cursor.getCount() > 0;
            cursor.close();
        }
        return Observable.just(result);
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
        ContentValues contentValues = new ContentValues();
        contentValues.put(MaterialisticProvider.ViewedEntry.COLUMN_NAME_ITEM_ID, itemId);
        ContentResolver cr = context.getContentResolver();
        new SessionHandler(cr, itemId).startInsert(0, itemId,
                MaterialisticProvider.URI_VIEWED, contentValues);
        // optimistically assume insert ok
        cr.notifyChange(MaterialisticProvider.URI_VIEWED
                        .buildUpon()
                        .appendPath(itemId)
                        .build(),
                null);
    }

    /**
     * Callback interface for asynchronous session operations
     */
    interface OperationCallbacks {
        /**
         * Fired when checking of view status is completed
         * @param isViewed  true if is viewed, false otherwise
         */
        void onCheckViewedComplete(boolean isViewed);
    }

    private static class SessionHandler extends AsyncQueryHandler {
        private final String mItemId;
        private OperationCallbacks mCallback;

        SessionHandler(ContentResolver cr, @NonNull String itemId) {
            super(cr);
            mItemId = itemId;
        }

        SessionHandler(ContentResolver cr, @NonNull String itemId,
                              @NonNull OperationCallbacks callback) {
            this(cr, itemId);
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
                mCallback.onCheckViewedComplete(cursor != null && cursor.getCount() > 0);
                mCallback = null;
            }
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

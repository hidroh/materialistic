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
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import javax.inject.Inject;

import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Data repository for session state
 */
public class SessionManager {

    private final Scheduler mIoScheduler;

    @Inject
    public SessionManager(Scheduler ioScheduler) {
        mIoScheduler = ioScheduler;
    }

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
        ContentResolver cr = context.getContentResolver();
        Observable.defer(() -> Observable.just(itemId))
                .map(id -> {
                    insert(cr, itemId);
                    return id;
                })
                .map(id -> MaterialisticProvider.URI_VIEWED.buildUpon().appendPath(itemId).build())
                .subscribeOn(mIoScheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> cr.notifyChange(uri, null));
    }

    @WorkerThread
    private void insert(ContentResolver cr, String itemId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MaterialisticProvider.ViewedEntry.COLUMN_NAME_ITEM_ID, itemId);
        cr.insert(MaterialisticProvider.URI_VIEWED, contentValues);
    }
}

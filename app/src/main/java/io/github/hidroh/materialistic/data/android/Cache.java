/*
 * Copyright (c) 2017 Ha Duy Trung
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

package io.github.hidroh.materialistic.data.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.LocalCache;
import io.github.hidroh.materialistic.data.MaterialisticProvider;

public class Cache implements LocalCache {
    private final ContentResolver mContentResolver;

    @Inject
    public Cache(Context context) {
        mContentResolver = context.getContentResolver();
    }

    @Nullable
    @Override
    public String getReadability(String itemId) {
        Cursor cursor = mContentResolver.query(MaterialisticProvider.URI_READABILITY,
                new String[]{MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_CONTENT},
                MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_ITEM_ID + " = ?",
                new String[]{itemId}, null);
        String content = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                content = cursor.getString(cursor.getColumnIndexOrThrow(
                        MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_CONTENT));
            }
            cursor.close();
        }
        return content;
    }

    @Override
    public void putReadability(String itemId, String content) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_ITEM_ID, itemId);
        contentValues.put(MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_CONTENT, content);
        mContentResolver.insert(MaterialisticProvider.URI_READABILITY, contentValues);
    }

    @Override
    public boolean isViewed(String itemId) {
        Cursor cursor = mContentResolver.query(MaterialisticProvider.URI_VIEWED, null,
                MaterialisticProvider.ViewedEntry.COLUMN_NAME_ITEM_ID + " = ?",
                new String[]{itemId}, null);
        boolean result = false;
        if (cursor != null) {
            result = cursor.getCount() > 0;
            cursor.close();
        }
        return result;
    }

    @Override
    public void setViewed(String itemId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MaterialisticProvider.ViewedEntry.COLUMN_NAME_ITEM_ID, itemId);
        mContentResolver.insert(MaterialisticProvider.URI_VIEWED, contentValues);
        Uri uri = MaterialisticProvider.URI_VIEWED.buildUpon().appendPath(itemId).build();
        mContentResolver.notifyChange(uri, null);
    }

    @Override
    public boolean isFavorite(String itemId) {
        Cursor cursor = mContentResolver.query(MaterialisticProvider.URI_FAVORITE,
                null,
                MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                new String[]{itemId},
                null);
        boolean result = false;
        if (cursor != null) {
            result = cursor.getCount() > 0;
            cursor.close();
        }
        return result;
    }
}

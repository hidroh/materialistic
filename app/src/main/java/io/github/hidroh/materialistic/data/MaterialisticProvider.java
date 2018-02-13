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

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteQueryBuilder;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import javax.inject.Inject;

import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.DataModule;

/**
 * @deprecated to use {@link MaterialisticDatabase}
 */
@Deprecated
public class MaterialisticProvider extends ContentProvider {
    static final String PROVIDER_AUTHORITY = "io.github.hidroh.materialistic.provider";
    private static final Uri BASE_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);
    public static final Uri URI_FAVORITE = BASE_URI.buildUpon()
            .appendPath("favorite")
            .build();
    public static final Uri URI_VIEWED = BASE_URI.buildUpon()
            .appendPath(ViewedEntry.TABLE_NAME)
            .build();
    public static final Uri URI_READABILITY = BASE_URI.buildUpon()
            .appendPath(ReadabilityEntry.TABLE_NAME)
            .build();
    private static final String READABILITY_MAX_ENTRIES = "50";
    private static final String SQL_WHERE_READABILITY_TRUNCATE = ReadabilityEntry._ID + " IN " +
            "(SELECT " + ReadabilityEntry._ID + " FROM " + ReadabilityEntry.TABLE_NAME +
            " ORDER BY " + ReadabilityEntry._ID + " DESC" +
            " LIMIT -1 OFFSET " + READABILITY_MAX_ENTRIES + ")";
    private static final String ORDER_DESC = " DESC";
    @Inject MaterialisticDatabase.SavedStoriesDao mSavedStoriesDao;
    @Inject SupportSQLiteOpenHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        ((Application) getContext().getApplicationContext())
                .getApplicationGraph()
                .plus(new ActivityModule(getContext()), new DataModule())
                .inject(this);
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SupportSQLiteDatabase db = mOpenHelper.getReadableDatabase();
        if (URI_VIEWED.equals(uri)) {
            return db.query(SupportSQLiteQueryBuilder.builder(ViewedEntry.TABLE_NAME)
                    .columns(projection)
                    .selection(selection, selectionArgs)
                    .orderBy(ViewedEntry.COLUMN_NAME_ITEM_ID + ORDER_DESC)
                    .create());
        } else if (URI_READABILITY.equals(uri)) {
            return db.query(SupportSQLiteQueryBuilder.builder(ReadabilityEntry.TABLE_NAME)
                    .columns(projection)
                    .selection(selection, selectionArgs)
                    .orderBy(ReadabilityEntry.COLUMN_NAME_ITEM_ID + ORDER_DESC)
                    .create());
        }
        return null;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        if (URI_VIEWED.equals(uri)) {
            return ViewedEntry.MIME_TYPE;
        } else if (URI_READABILITY.equals(uri)) {
            return ReadabilityEntry.MIME_TYPE;
        }
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SupportSQLiteDatabase db = mOpenHelper.getWritableDatabase();
        if (URI_VIEWED.equals(uri)) {
            int updated = update(uri, values, ViewedEntry.COLUMN_NAME_ITEM_ID + " = ?",
                    new String[]{values.getAsString(ViewedEntry.COLUMN_NAME_ITEM_ID)});
            long id = -1;
            if (updated == 0) {
                id = db.insert(ViewedEntry.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
            }

            return id == -1 ? null : ContentUris.withAppendedId(URI_VIEWED, id);
        } else if (URI_READABILITY.equals(uri)) {
            int updated = update(uri, values, ReadabilityEntry.COLUMN_NAME_ITEM_ID + " = ?",
                    new String[]{values.getAsString(ReadabilityEntry.COLUMN_NAME_ITEM_ID)});
            long id = -1;
            if (updated == 0) {
                id = db.insert(ReadabilityEntry.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                db.delete(ReadabilityEntry.TABLE_NAME, SQL_WHERE_READABILITY_TRUNCATE, null);
            }

            return id == -1 ? null : ContentUris.withAppendedId(URI_READABILITY, id);
        }

        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SupportSQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String table = null;
        if (URI_VIEWED.equals(uri)) {
            table = ViewedEntry.TABLE_NAME;
        } else if (URI_READABILITY.equals(uri)) {
            table = ReadabilityEntry.TABLE_NAME;
        }

        if (TextUtils.isEmpty(table)) {
            return 0;
        }
        return db.delete(table, selection, selectionArgs);
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SupportSQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String table = null;
        if (URI_VIEWED.equals(uri)) {
            table = ViewedEntry.TABLE_NAME;
        } else if (URI_READABILITY.equals(uri)) {
            table = ReadabilityEntry.TABLE_NAME;
        }

        if (TextUtils.isEmpty(table)) {
            return 0;
        }

        return db.update(table, SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
    }

    public interface FavoriteEntry extends BaseColumns {
        String COLUMN_NAME_ITEM_ID = "itemid";
        String COLUMN_NAME_URL = "url";
        String COLUMN_NAME_TITLE = "title";
        String COLUMN_NAME_TIME = "time";
    }

    public interface ViewedEntry extends BaseColumns {
        String TABLE_NAME = "viewed";
        String MIME_TYPE = "vnd.android.cursor.dir/vnd." + PROVIDER_AUTHORITY + "." + TABLE_NAME;
        String COLUMN_NAME_ITEM_ID = "itemid";
    }

    public interface ReadabilityEntry extends BaseColumns {
        String TABLE_NAME = "readability";
        String MIME_TYPE = "vnd.android.cursor.dir/vnd." + PROVIDER_AUTHORITY + "." + TABLE_NAME;
        String COLUMN_NAME_ITEM_ID = "itemid";
        String COLUMN_NAME_CONTENT = "content";
    }

}

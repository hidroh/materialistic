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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import io.github.hidroh.materialistic.annotation.Synthetic;

public class MaterialisticProvider extends ContentProvider {
    static final String PROVIDER_AUTHORITY = "io.github.hidroh.materialistic.provider";
    private static final Uri BASE_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);
    public static final Uri URI_FAVORITE = BASE_URI.buildUpon()
            .appendPath(FavoriteEntry.TABLE_NAME)
            .build();
    public static final Uri URI_VIEWED = BASE_URI.buildUpon()
            .appendPath(ViewedEntry.TABLE_NAME)
            .build();
    public static final Uri URI_READABILITY = BASE_URI.buildUpon()
            .appendPath(ReadabilityEntry.TABLE_NAME)
            .build();
    private static final String READABILITY_MAX_ENTRIES = "50";
    private DbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        if (URI_FAVORITE.equals(uri)) {
            return db.query(FavoriteEntry.TABLE_NAME, projection,
                    selection, selectionArgs,
                    null, null,
                    FavoriteEntry.COLUMN_NAME_TIME + DbHelper.ORDER_DESC);
        } else if (URI_VIEWED.equals(uri)) {
            return db.query(ViewedEntry.TABLE_NAME, projection,
                    selection, selectionArgs,
                    null, null,
                    ViewedEntry.COLUMN_NAME_ITEM_ID + DbHelper.ORDER_DESC);
        } else if (URI_READABILITY.equals(uri)) {
            return db.query(ReadabilityEntry.TABLE_NAME, projection,
                    selection, selectionArgs,
                    null, null,
                    ReadabilityEntry.COLUMN_NAME_ITEM_ID + DbHelper.ORDER_DESC);
        }
        return null;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        if (URI_FAVORITE.equals(uri)) {
            return FavoriteEntry.MIME_TYPE;
        } else if (URI_VIEWED.equals(uri)) {
            return ViewedEntry.MIME_TYPE;
        } else if (URI_READABILITY.equals(uri)) {
            return ReadabilityEntry.MIME_TYPE;
        }
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        if (URI_FAVORITE.equals(uri)) {
            int updated = update(uri, values, FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                    new String[]{values.getAsString(FavoriteEntry.COLUMN_NAME_ITEM_ID)});
            long id = -1;
            if (updated == 0) {
                id = db.insert(FavoriteEntry.TABLE_NAME, null, values);
            }
            return id == -1 ? null : ContentUris.withAppendedId(URI_FAVORITE, id);
        } else if (URI_VIEWED.equals(uri)) {
            int updated = update(uri, values, ViewedEntry.COLUMN_NAME_ITEM_ID + " = ?",
                    new String[]{values.getAsString(ViewedEntry.COLUMN_NAME_ITEM_ID)});
            long id = -1;
            if (updated == 0) {
                id = db.insert(ViewedEntry.TABLE_NAME, null, values);
            }

            return id == -1 ? null : ContentUris.withAppendedId(URI_VIEWED, id);
        } else if (URI_READABILITY.equals(uri)) {
            int updated = update(uri, values, ReadabilityEntry.COLUMN_NAME_ITEM_ID + " = ?",
                    new String[]{values.getAsString(ReadabilityEntry.COLUMN_NAME_ITEM_ID)});
            long id = -1;
            if (updated == 0) {
                id = db.insert(ReadabilityEntry.TABLE_NAME, null, values);
                db.delete(ReadabilityEntry.TABLE_NAME, DbHelper.SQL_WHERE_READABILITY_TRUNCATE, null);
            }

            return id == -1 ? null : ContentUris.withAppendedId(URI_READABILITY, id);
        }

        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String table = null;
        if (URI_FAVORITE.equals(uri)) {
            table = FavoriteEntry.TABLE_NAME;
        } else if (URI_VIEWED.equals(uri)) {
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
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String table = null;
        if (URI_FAVORITE.equals(uri)) {
            table = FavoriteEntry.TABLE_NAME;
        } else if (URI_VIEWED.equals(uri)) {
            table = ViewedEntry.TABLE_NAME;
        } else if (URI_READABILITY.equals(uri)) {
            table = ReadabilityEntry.TABLE_NAME;
        }

        if (TextUtils.isEmpty(table)) {
            return 0;
        }

        return db.update(table, values, selection, selectionArgs);
    }

    public interface FavoriteEntry extends BaseColumns {
        String TABLE_NAME = "favorite";
        String MIME_TYPE = "vnd.android.cursor.dir/vnd." + PROVIDER_AUTHORITY + "." + TABLE_NAME;
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

    private static class DbHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "Materialistic.db";
        private static final int DB_VERSION = 3;
        private static final String TEXT_TYPE = " TEXT";
        private static final String INTEGER_TYPE = " INTEGER";
        private static final String PRIMARY_KEY = " PRIMARY KEY";
        private static final String COMMA_SEP = ",";
        private static final String ORDER_DESC = " DESC";
        private static final String SQL_CREATE_FAVORITE_TABLE =
                "CREATE TABLE " + FavoriteEntry.TABLE_NAME + " (" +
                        FavoriteEntry._ID +                 INTEGER_TYPE +  PRIMARY_KEY + COMMA_SEP +
                        FavoriteEntry.COLUMN_NAME_ITEM_ID + TEXT_TYPE + COMMA_SEP +
                        FavoriteEntry.COLUMN_NAME_URL +     TEXT_TYPE + COMMA_SEP +
                        FavoriteEntry.COLUMN_NAME_TITLE +   TEXT_TYPE + COMMA_SEP +
                        FavoriteEntry.COLUMN_NAME_TIME +    TEXT_TYPE +
                " )";
        private static final String SQL_CREATE_VIEWED_TABLE =
                "CREATE TABLE " + ViewedEntry.TABLE_NAME + " (" +
                        ViewedEntry._ID +                 INTEGER_TYPE +  PRIMARY_KEY + COMMA_SEP +
                        ViewedEntry.COLUMN_NAME_ITEM_ID + TEXT_TYPE +
                        " )";
        private static final String SQL_CREATE_READABILITY_TABLE =
                "CREATE TABLE " + ReadabilityEntry.TABLE_NAME + " (" +
                        ReadabilityEntry._ID +                 INTEGER_TYPE +  PRIMARY_KEY + COMMA_SEP +
                        ReadabilityEntry.COLUMN_NAME_ITEM_ID + TEXT_TYPE + COMMA_SEP +
                        ReadabilityEntry.COLUMN_NAME_CONTENT + TEXT_TYPE +
                        " )";
        private static final String SQL_DROP_FAVORITE_TABLE =
                "DROP TABLE IF EXISTS " + FavoriteEntry.TABLE_NAME;
        private static final String SQL_DROP_VIEWED_TABLE =
                "DROP TABLE IF EXISTS " + ViewedEntry.TABLE_NAME;
        private static final String SQL_DROP_READABILITY_TABLE =
                "DROP TABLE IF EXISTS " + ReadabilityEntry.TABLE_NAME;
        private static final String SQL_WHERE_READABILITY_TRUNCATE = ReadabilityEntry._ID + " IN " +
                "(SELECT " + ReadabilityEntry._ID + " FROM " + ReadabilityEntry.TABLE_NAME +
                " ORDER BY " + ReadabilityEntry._ID + " DESC" +
                " LIMIT -1 OFFSET " + READABILITY_MAX_ENTRIES + ")";

        @Synthetic
        DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_FAVORITE_TABLE);
            db.execSQL(SQL_CREATE_VIEWED_TABLE);
            db.execSQL(SQL_CREATE_READABILITY_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            switch (oldVersion) {
                case 2:
                    db.execSQL(SQL_CREATE_READABILITY_TABLE);
                    break;
                case 1:
                    db.execSQL(SQL_CREATE_VIEWED_TABLE);
                    break;
                default:
                    db.execSQL(SQL_DROP_FAVORITE_TABLE);
                    db.execSQL(SQL_DROP_VIEWED_TABLE);
                    db.execSQL(SQL_DROP_READABILITY_TABLE);
                    onCreate(db);
                    break;
            }
        }
    }
}

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
import android.text.TextUtils;

public class MaterialisticProvider extends ContentProvider {
    private static final String PROVIDER_AUTHORITY = "io.github.hidroh.materialistic.provider";
    private static final Uri BASE_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);
    public static final Uri URI_FAVORITE = BASE_URI.buildUpon().appendPath(FavoriteEntry.TABLE_NAME).build();
    private DbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        if (URI_FAVORITE.equals(uri)) {
            return db.query(FavoriteEntry.TABLE_NAME, null, null, null, null, null,
                    FavoriteEntry.COLUMN_NAME_ITEM_ID + DbHelper.ORDER_DESC);
        }

        return null;
    }

    @Override
    public String getType(Uri uri) {
        if (URI_FAVORITE.equals(uri)) {
            return FavoriteEntry.MIME_TYPE;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        if (URI_FAVORITE.equals(uri)) {
            int updated = update(uri, values, FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                    new String[]{values.getAsString(FavoriteEntry.COLUMN_NAME_ITEM_ID)});
            long id = -1;
            if (updated == 0) {
                id = db.insert(FavoriteEntry.TABLE_NAME, null, values);
            }

            return id == -1 ? null : ContentUris.withAppendedId(URI_FAVORITE, id);
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String table = null;
        if (URI_FAVORITE.equals(uri)) {
            table = FavoriteEntry.TABLE_NAME;
        }

        if (TextUtils.isEmpty(table)) {
            return 0;
        }

        return db.delete(table, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String table = null;
        if (URI_FAVORITE.equals(uri)) {
            table = FavoriteEntry.TABLE_NAME;
        }

        if (TextUtils.isEmpty(table)) {
            return 0;
        }

        return db.update(table, values, selection, selectionArgs);
    }

    interface FavoriteEntry extends BaseColumns {
        static final String TABLE_NAME = "favorite";
        static final String MIME_TYPE = "vnd.android.cursor.dir/vnd." + PROVIDER_AUTHORITY + "." + TABLE_NAME;
        static final String COLUMN_NAME_ITEM_ID = "itemid";
        static final String COLUMN_NAME_URL = "url";
        static final String COLUMN_NAME_TITLE = "title";
        static final String COLUMN_NAME_TIME = "time";
    }

    private static class DbHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "Materialistic.db";
        private static final int DB_VERSION = 1;
        private static final String TEXT_TYPE = " TEXT";
        private static final String INTEGER_TYPE = " INTEGER";
        private static final String PRIMARY_KEY = " PRIMARY KEY";
        private static final String COMMA_SEP = ",";
        private static final String ORDER_DESC = " DESC";
        private static final String SQL_CREATE_DB =
                "CREATE TABLE " + FavoriteEntry.TABLE_NAME + " (" +
                        FavoriteEntry._ID +                 INTEGER_TYPE +  PRIMARY_KEY + COMMA_SEP +
                        FavoriteEntry.COLUMN_NAME_ITEM_ID + TEXT_TYPE + COMMA_SEP +
                        FavoriteEntry.COLUMN_NAME_URL +     TEXT_TYPE + COMMA_SEP +
                        FavoriteEntry.COLUMN_NAME_TITLE +   TEXT_TYPE + COMMA_SEP +
                        FavoriteEntry.COLUMN_NAME_TIME +    TEXT_TYPE +
                " )";
        private static final String SQL_DROP_DB =
                "DROP TABLE IF EXISTS " + FavoriteEntry.TABLE_NAME;

        private DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_DB);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DROP_DB);
            onCreate(db);
        }
    }
}

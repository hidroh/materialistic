package io.github.hidroh.materialistic.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

@Database(entities = {MaterialisticDatabase.SavedStory.class}, version = 4)
public abstract class MaterialisticDatabase extends RoomDatabase {

    private static MaterialisticDatabase sInstance;

    public static synchronized MaterialisticDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = setupBuilder(Room.databaseBuilder(context.getApplicationContext(),
                    MaterialisticDatabase.class,
                    DbConstants.DB_NAME))
                    .build();
        }
        return sInstance;
    }

    @VisibleForTesting
    protected static Builder<MaterialisticDatabase> setupBuilder(Builder<MaterialisticDatabase> builder) {
        return builder
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        db.execSQL(DbConstants.SQL_CREATE_VIEWED_TABLE);
                        db.execSQL(DbConstants.SQL_CREATE_READABILITY_TABLE);
                    }
                })
                .addMigrations(new Migration(1, 2) {
                    @Override
                    public void migrate(@NonNull SupportSQLiteDatabase database) {
                        database.execSQL(DbConstants.SQL_CREATE_VIEWED_TABLE);
                    }
                })
                .addMigrations(new Migration(2, 3) {
                    @Override
                    public void migrate(@NonNull SupportSQLiteDatabase database) {
                        database.execSQL(DbConstants.SQL_CREATE_READABILITY_TABLE);
                    }
                })
                .addMigrations(new Migration(3, 4) {
                    @Override
                    public void migrate(@NonNull SupportSQLiteDatabase database) {
                        database.execSQL(DbConstants.SQL_CREATE_SAVED_TABLE);
                        database.execSQL(DbConstants.SQL_INSERT_FAVORITE_SAVED);
                        database.execSQL(DbConstants.SQL_DROP_FAVORITE_TABLE);
                    }
                });
    }

    public abstract SavedStoriesDao getSavedStoriesDao();

    @Entity(tableName = "saved")
    public static class SavedStory {
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "_id")
        private int id;
        @ColumnInfo(name = "itemid")
        private String itemId;
        private String url;
        private String title;
        private String time;

        static SavedStory from(WebItem story) {
            SavedStory savedStory = new SavedStory();
            savedStory.itemId = story.getId();
            savedStory.url = story.getUrl();
            savedStory.title = story.getDisplayedTitle();
            savedStory.time = String.valueOf(story instanceof Favorite ?
                    ((Favorite) story).getTime() :
                    String.valueOf(System.currentTimeMillis()));
            return savedStory;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }

    @Dao
    public interface SavedStoriesDao {
        @Query("SELECT * FROM saved ORDER BY time DESC")
        Cursor selectAllToCursor();

        @Query("SELECT * FROM saved WHERE title LIKE '%' || :query || '%' ORDER BY time DESC")
        Cursor searchToCursor(String query);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(SavedStory... savedStories);

        @Query("DELETE FROM saved")
        int deleteAll();

        @Query("DELETE FROM saved WHERE itemid = :itemId")
        int deleteByItemId(String itemId);

        @Query("DELETE FROM saved WHERE title LIKE '%' || :query || '%'")
        int deleteByTitle(String query);

        @Query("SELECT * FROM saved WHERE itemid = :itemId")
        @Nullable
        SavedStory selectByItemId(String itemId);
    }

    static class DbConstants {
        static final String DB_NAME = "Materialistic.db";
        static final String SQL_CREATE_VIEWED_TABLE =
                "CREATE TABLE viewed (_id INTEGER PRIMARY KEY,itemid TEXT)";
        static final String SQL_CREATE_READABILITY_TABLE =
                "CREATE TABLE readability (_id INTEGER PRIMARY KEY,itemid TEXT,content TEXT)";
        static final String SQL_CREATE_SAVED_TABLE =
                "CREATE TABLE saved (_id INTEGER NOT NULL PRIMARY KEY, itemid TEXT, url TEXT, title TEXT, time TEXT)";
        static final String SQL_INSERT_FAVORITE_SAVED = "INSERT INTO saved SELECT * FROM favorite";
        static final String SQL_DROP_FAVORITE_TABLE = "DROP TABLE IF EXISTS favorite";
    }
}

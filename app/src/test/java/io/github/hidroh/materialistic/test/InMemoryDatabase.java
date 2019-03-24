package io.github.hidroh.materialistic.test;

import androidx.room.Room;
import android.content.Context;

import io.github.hidroh.materialistic.data.MaterialisticDatabase;

public abstract class InMemoryDatabase extends MaterialisticDatabase {

    private static MaterialisticDatabase sInstance;

    public static synchronized MaterialisticDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = setupBuilder(Room.inMemoryDatabaseBuilder(context.getApplicationContext(),
                    MaterialisticDatabase.class))
                    .allowMainThreadQueries()
                    .build();
        }
        return sInstance;
    }

    public static synchronized void reset() {
        sInstance = null;
    }
}

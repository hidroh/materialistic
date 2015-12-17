package io.github.hidroh.materialistic.data;

import android.os.Parcelable;
import android.support.annotation.NonNull;

public interface UserManager {
    void getUser(String username, final ResponseListener<User> listener);

    interface User extends Parcelable {
        String getId();
        String getAbout();
        long getKarma();
        long getCreated();
        @NonNull ItemManager.Item[] getItems();
    }
}

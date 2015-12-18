package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public interface UserManager {
    void getUser(String username, final ResponseListener<User> listener);

    interface User extends Parcelable {
        String getId();
        String getAbout();
        long getKarma();
        String getCreated(Context context);
        @NonNull ItemManager.Item[] getItems();
    }
}

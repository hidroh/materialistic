package io.github.hidroh.materialistic.accounts;

import android.content.Context;

public interface UserServices {
    abstract class Callback {
        public void onDone(boolean successful) {}
        public void onError() {}
    }

    void login(String username, String password, boolean createAccount, Callback callback);

    void voteUp(Context context, String itemId, Callback callback);
}

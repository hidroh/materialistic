package io.github.hidroh.materialistic.accounts;

public interface UserServices {
    interface Callback {
        void onDone(boolean successful);
    }

    void login(String username, String password, Callback callback);
}

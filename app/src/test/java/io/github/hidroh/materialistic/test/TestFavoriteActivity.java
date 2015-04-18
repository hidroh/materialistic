package io.github.hidroh.materialistic.test;

import android.support.v7.view.ActionMode;

import io.github.hidroh.materialistic.FavoriteActivity;

public class TestFavoriteActivity extends FavoriteActivity {
    public ActionMode.Callback actionModeCallback;

    @Override
    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        actionModeCallback = callback;
        return super.startSupportActionMode(callback);
    }
}

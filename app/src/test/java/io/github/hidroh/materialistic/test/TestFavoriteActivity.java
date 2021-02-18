package io.github.hidroh.materialistic.test;

import androidx.appcompat.view.ActionMode;

import org.jetbrains.annotations.NotNull;

import io.github.hidroh.materialistic.FavoriteActivity;

public class TestFavoriteActivity extends FavoriteActivity {
    public ActionMode.Callback actionModeCallback;

    @Override
    public ActionMode startSupportActionMode(@NotNull ActionMode.Callback callback) {
        actionModeCallback = callback;
        return super.startSupportActionMode(callback);
    }
}

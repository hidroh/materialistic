package io.github.hidroh.materialistic;

import android.support.annotation.NonNull;

import io.github.hidroh.materialistic.data.ItemManager;

public class NewActivity extends BaseStoriesActivity {

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_new);
    }

    @NonNull
    @Override
    protected String getFetchMode() {
        return ItemManager.NEW_FETCH_MODE;
    }

}

package io.github.hidroh.materialistic;

import android.support.annotation.NonNull;

import io.github.hidroh.materialistic.data.ItemManager;

public class ListActivity extends BaseStoriesActivity {

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_list);
    }

    @NonNull
    @Override
    protected String getFetchMode() {
        return ItemManager.TOP_FETCH_MODE;
    }

}

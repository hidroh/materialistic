package io.github.hidroh.materialistic;

import io.github.hidroh.materialistic.data.ItemManager;

public class NewActivity extends BaseStoriesActivity {

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_new);
    }

    @Override
    protected ItemManager.FetchMode getFetchMode() {
        return ItemManager.FetchMode.newest;
    }

}

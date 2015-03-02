package io.github.hidroh.materialistic;

import io.github.hidroh.materialistic.data.ItemManager;

public class AskActivity extends BaseStoriesActivity {

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_ask);
    }

    @Override
    protected ItemManager.FetchMode getFetchMode() {
        return ItemManager.FetchMode.ask;
    }

}

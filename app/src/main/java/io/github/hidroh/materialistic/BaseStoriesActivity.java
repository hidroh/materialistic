package io.github.hidroh.materialistic;

import android.support.v4.app.Fragment;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public abstract class BaseStoriesActivity extends BaseListActivity {

    protected abstract ItemManager.FetchMode getFetchMode();

    @Override
    protected Fragment instantiateListFragment() {
        return ListFragment.instantiate(this, HackerNewsClient.getInstance(this),
                getFetchMode().name());
    }

    @Override
    protected boolean isItemOptionsMenuVisible() {
        return mSelectedItem != null;
    }
}

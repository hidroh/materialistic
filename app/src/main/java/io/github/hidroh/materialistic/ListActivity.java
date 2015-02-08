package io.github.hidroh.materialistic;

import android.support.v4.app.Fragment;

import io.github.hidroh.materialistic.data.HackerNewsClient;

public class ListActivity extends BaseListActivity {

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_list);
    }

    @Override
    protected Fragment instantiateListFragment() {
        return ListFragment.instantiate(this, HackerNewsClient.getInstance(this));
    }

    @Override
    protected boolean isItemOptionsMenuVisible() {
        return mSelectedItem != null;
    }
}

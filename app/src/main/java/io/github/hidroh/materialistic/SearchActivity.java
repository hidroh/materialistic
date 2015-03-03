package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public class SearchActivity extends BaseListActivity {

    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().hasExtra(SearchManager.QUERY)) {
            mQuery = getIntent().getStringExtra(SearchManager.QUERY);
        }
        super.onCreate(savedInstanceState);
        if (!TextUtils.isEmpty(mQuery)) {
            setTitle(getString(R.string.title_activity_search_query, mQuery));
        }
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_search);
    }

    @Override
    protected Fragment instantiateListFragment() {
        final ItemManager itemManager;
        if (TextUtils.isEmpty(mQuery)) {
            itemManager = HackerNewsClient.getInstance(this);
        } else {
            itemManager = AlgoliaClient.getInstance(this);
            ((AlgoliaClient) itemManager).setQuery(mQuery);
        }
        return ListFragment.instantiate(this, itemManager, ItemManager.FetchMode.top);
    }

    @Override
    protected boolean isItemOptionsMenuVisible() {
        return mSelectedItem != null;
    }
}

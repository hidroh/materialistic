package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.SearchRecentSuggestionsProvider;

public class SearchActivity extends BaseListActivity {

    private static final int MAX_RECENT_SUGGESTIONS = 10;
    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().hasExtra(SearchManager.QUERY)) {
            mQuery = getIntent().getStringExtra(SearchManager.QUERY);
        }
        super.onCreate(savedInstanceState);
        if (!TextUtils.isEmpty(mQuery)) {
            getSupportActionBar().setSubtitle(mQuery);
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SearchRecentSuggestionsProvider.PROVIDER_AUTHORITY,
                    SearchRecentSuggestionsProvider.MODE) {
                @Override
                public void saveRecentQuery(String queryString, String line2) {
                    truncateHistory(getContentResolver(), MAX_RECENT_SUGGESTIONS - 1);
                    super.saveRecentQuery(queryString, line2);
                }
            };
            suggestions.saveRecentQuery(mQuery, null);
        }
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_search);
    }

    @Override
    protected Fragment instantiateListFragment() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        if (TextUtils.isEmpty(mQuery)) {
            args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        } else {
            args.putString(ListFragment.EXTRA_ITEM_MANAGER, AlgoliaClient.class.getName());
        }
        return Fragment.instantiate(this, ListFragment.class.getName(), args);
    }

    @Override
    protected boolean isItemOptionsMenuVisible() {
        return mSelectedItem != null;
    }
}

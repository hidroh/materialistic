package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.HackerNewsClient;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sort, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(AlgoliaClient.sSortByTime ? R.id.menu_sort_recent : R.id.menu_sort_popular)
                .setChecked(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getGroupId() == R.id.menu_sort_group) {
            item.setChecked(true);
            sort(item.getItemId() == R.id.menu_sort_recent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_search);
    }

    @Override
    protected Fragment instantiateListFragment() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_FILTER, mQuery);
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

    private void sort(boolean byTime) {
        if (AlgoliaClient.sSortByTime == byTime) {
            return;
        }
        AlgoliaClient.sSortByTime = byTime;
        Preferences.setSortByRecent(this, byTime);
        ListFragment listFragment = (ListFragment) getSupportFragmentManager()
                .findFragmentByTag(LIST_FRAGMENT_TAG);
        if (listFragment != null) {
            listFragment.filter(mQuery);
        }
    }
}

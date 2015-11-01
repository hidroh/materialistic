package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import io.github.hidroh.materialistic.data.AlgoliaPopularClient;

public class PopularActivity extends BaseListActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_popular, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_range_day) {
            filter(AlgoliaPopularClient.LAST_24H);
            return true;
        } else if (item.getItemId() == R.id.menu_range_week) {
            filter(AlgoliaPopularClient.PAST_WEEK);
            return true;
        } else if (item.getItemId() == R.id.menu_range_month) {
            filter(AlgoliaPopularClient.PAST_MONTH);
            return true;
        } else if (item.getItemId() == R.id.menu_range_year) {
            filter(AlgoliaPopularClient.PAST_YEAR);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_popular);
    }

    @Override
    protected Fragment instantiateListFragment() {
        Bundle args = new Bundle();
        String range = Preferences.getPopularRange(this);
        setSubtitle(range);
        args.putString(ListFragment.EXTRA_FILTER, range);
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, AlgoliaPopularClient.class.getName());
        return Fragment.instantiate(this, ListFragment.class.getName(), args);
    }

    @Override
    protected boolean isItemOptionsMenuVisible() {
        return mSelectedItem != null;
    }

    @Override
    protected boolean isSearchable() {
        return false;
    }

    private void filter(@AlgoliaPopularClient.Range String range) {
        setSubtitle(range);
        Preferences.setPopularRange(this, range);
        ListFragment listFragment = (ListFragment) getSupportFragmentManager()
                .findFragmentByTag(LIST_FRAGMENT_TAG);
        if (listFragment != null) {
            listFragment.filter(range);
        }
    }

    private void setSubtitle(String range) {
        final int stringRes;
        switch (range) {
            case AlgoliaPopularClient.LAST_24H:
                default:
                stringRes = R.string.popular_range_last_24h;
                break;
            case AlgoliaPopularClient.PAST_WEEK:
                stringRes = R.string.popular_range_past_week;
                break;
            case AlgoliaPopularClient.PAST_MONTH:
                stringRes = R.string.popular_range_past_month;
                break;
            case AlgoliaPopularClient.PAST_YEAR:
                stringRes = R.string.popular_range_past_year;
                break;
        }
        getSupportActionBar().setSubtitle(stringRes);
    }

}

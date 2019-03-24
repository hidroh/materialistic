/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import io.github.hidroh.materialistic.data.AlgoliaPopularClient;

public class PopularActivity extends BaseListActivity {
    private static final String STATE_RANGE = "state:range";

    private String mRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            setRange(savedInstanceState.getString(STATE_RANGE));
        }
    }

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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_RANGE, mRange);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_popular);
    }

    @Override
    protected Fragment instantiateListFragment() {
        Bundle args = new Bundle();
        setRange(Preferences.getPopularRange(this));
        args.putString(ListFragment.EXTRA_FILTER, mRange);
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, AlgoliaPopularClient.class.getName());
        return Fragment.instantiate(this, ListFragment.class.getName(), args);
    }

    @Override
    protected boolean isSearchable() {
        return false;
    }

    private void filter(@AlgoliaPopularClient.Range String range) {
        setRange(range);
        Preferences.setPopularRange(this, range);
        ListFragment listFragment = (ListFragment) getSupportFragmentManager()
                .findFragmentByTag(LIST_FRAGMENT_TAG);
        if (listFragment != null) {
            listFragment.filter(range);
        }
    }

    private void setRange(String range) {
        mRange = range;
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

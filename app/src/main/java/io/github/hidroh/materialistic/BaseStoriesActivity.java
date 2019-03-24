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
import android.os.Handler;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.format.DateUtils;

import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public abstract class BaseStoriesActivity extends BaseListActivity
        implements ListFragment.RefreshCallback {

    private static final String STATE_LAST_UPDATED = "state:lastUpdated";
    @Synthetic Long mLastUpdated;
    private final Runnable mLastUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (mLastUpdated == null) {
                return;
            }
            //noinspection ConstantConditions
            if (getSupportActionBar() == null) {
                return;
            }
            if (AppUtils.hasConnection(BaseStoriesActivity.this)) {
                getSupportActionBar().setSubtitle(getString(R.string.last_updated,
                        DateUtils.getRelativeTimeSpanString(mLastUpdated,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL)));
                mHandler.postAtTime(this, SystemClock.uptimeMillis() + DateUtils.MINUTE_IN_MILLIS);
            } else {
                getSupportActionBar().setSubtitle(R.string.offline);
            }
        }
    };
    @Synthetic final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mLastUpdated = savedInstanceState.getLong(STATE_LAST_UPDATED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.removeCallbacks(mLastUpdateTask);
        mHandler.post(mLastUpdateTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mLastUpdateTask);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mLastUpdated != null) {
            outState.putLong(STATE_LAST_UPDATED, mLastUpdated);
        }
    }

    @Override
    public void onRefreshed() {
        onItemSelected(null);
        mLastUpdated = System.currentTimeMillis();
        mHandler.removeCallbacks(mLastUpdateTask);
        mHandler.post(mLastUpdateTask);
    }

    @NonNull
    @ItemManager.FetchMode
    protected abstract String getFetchMode();

    @Override
    protected Fragment instantiateListFragment() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, getFetchMode());
        return Fragment.instantiate(this, ListFragment.class.getName(), args);
    }

}

package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public abstract class BaseStoriesActivity extends BaseListActivity
        implements ListFragment.RefreshCallback {

    private static final String STATE_LAST_UPDATED = "state:lastUpdated";
    private Long mLastUpdated;
    private final Runnable mLastUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (mLastUpdated == null) {
                return;
            }
            getSupportActionBar().setSubtitle(getString(R.string.last_updated,
                    DateUtils.getRelativeTimeSpanString(mLastUpdated,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL)));
            mHandler.postAtTime(this, SystemClock.uptimeMillis() + DateUtils.MINUTE_IN_MILLIS);
        }
    };
    private final Handler mHandler = new Handler();

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

    @Override
    protected boolean isItemOptionsMenuVisible() {
        return mSelectedItem != null;
    }
}

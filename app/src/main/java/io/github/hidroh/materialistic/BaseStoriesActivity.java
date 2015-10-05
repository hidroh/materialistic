package io.github.hidroh.materialistic;

import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;

public abstract class BaseStoriesActivity extends BaseListActivity
        implements ListFragment.RefreshCallback {

    private Long mLastUpdated;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
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
        return ListFragment.instantiate(this, mItemManager, getFetchMode());
    }

    @Override
    protected boolean isItemOptionsMenuVisible() {
        return mSelectedItem != null;
    }
}

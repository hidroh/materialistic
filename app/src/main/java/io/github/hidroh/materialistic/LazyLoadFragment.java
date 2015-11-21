package io.github.hidroh.materialistic;

import android.os.Bundle;

/**
 * Base fragment that controls load timing depends on WIFI and visibility
 */
public abstract class LazyLoadFragment extends BaseFragment {
    private static final String STATE_EAGER_LOAD = "state:eagerLoad";
    private static final String STATE_ACTIVITY_CREATED = "state:activityCreated";
    private boolean mEagerLoad;
    private boolean mActivityCreated;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mEagerLoad = savedInstanceState.getBoolean(STATE_EAGER_LOAD);
            mActivityCreated = savedInstanceState.getBoolean(STATE_ACTIVITY_CREATED);
        } else {
            mEagerLoad = AppUtils.isOnWiFi(getContext());
        }
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !mEagerLoad) {
            mEagerLoad = true;
            eagerLoad();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivityCreated = true;
        eagerLoad();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_EAGER_LOAD, mEagerLoad);
        outState.putBoolean(STATE_ACTIVITY_CREATED, mActivityCreated);
    }

    /**
     * Load data after fragment becomes visible or if WIFI is enabled
     */
    protected abstract void load();

    private void eagerLoad() {
        if (!mEagerLoad) {
            return;
        }
        if (!mActivityCreated) {
            return;
        }
        load();
    }
}

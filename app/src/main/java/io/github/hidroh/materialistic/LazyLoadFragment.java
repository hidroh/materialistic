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

import android.content.Context;
import android.os.Bundle;

/**
 * Base fragment that controls load timing depends on WIFI and visibility
 */
public abstract class LazyLoadFragment extends BaseFragment {
    public static final String EXTRA_EAGER_LOAD = LazyLoadFragment.class.getName() + ".EXTRA_EAGER_LOAD";
    public static final String EXTRA_RETAIN_INSTANCE = WebFragment.class.getName() + ".EXTRA_RETAIN_INSTANCE";
    private static final String STATE_EAGER_LOAD = "state:eagerLoad";
    private static final String STATE_LOADED = "state:loaded";
    private boolean mEagerLoad, mLoaded, mActivityCreated;
    private boolean mNewInstance;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mNewInstance = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(getArguments().getBoolean(EXTRA_RETAIN_INSTANCE, false));
        mNewInstance = true;
        if (savedInstanceState != null) {
            mEagerLoad = savedInstanceState.getBoolean(STATE_EAGER_LOAD);
            mLoaded = savedInstanceState.getBoolean(STATE_LOADED);
        } else {
            mEagerLoad = getArguments() != null && getArguments().getBoolean(EXTRA_EAGER_LOAD) ||
                    !Preferences.shouldLazyLoad(getActivity());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivityCreated = true;
        if (isNewInstance()) {
            eagerLoad();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_EAGER_LOAD, mEagerLoad);
        outState.putBoolean(STATE_LOADED, false); // allow re-loading on state restoration
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivityCreated = false;
    }

    public void loadNow() {
        if (mActivityCreated) {
            mEagerLoad = true;
            eagerLoad();
        }
    }

    /**
     * Load data after fragment becomes visible or if WIFI is enabled
     */
    protected abstract void load();

    protected boolean isNewInstance() {
        return !getRetainInstance() || mNewInstance;
    }

    final void eagerLoad() {
        if (mEagerLoad && !mLoaded) {
            mLoaded = true;
            load();
        }
    }
}

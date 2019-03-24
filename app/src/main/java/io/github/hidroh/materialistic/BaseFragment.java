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
import androidx.fragment.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * Base fragment which performs injection using parent's activity object graphs if any
 */
public abstract class BaseFragment extends Fragment {
    protected final MenuTintDelegate mMenuTintDelegate = new MenuTintDelegate();
    private boolean mAttached;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAttached = true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() instanceof Injectable) {
            ((Injectable) getActivity()).inject(this);
        }
        mMenuTintDelegate.onActivityCreated(getActivity());
    }

    @Override
    public final void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (isAttached()) { // TODO http://b.android.com/80783
            createOptionsMenu(menu, inflater);
            mMenuTintDelegate.onOptionsMenuCreated(menu);
        }
    }

    @Override
    public final void onPrepareOptionsMenu(Menu menu) {
        if (isAttached()) { // TODO http://b.android.com/80783
            prepareOptionsMenu(menu);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAttached = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Application.getRefWatcher(getActivity()).watch(this);
    }

    public boolean isAttached() {
        return mAttached;
    }

    protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
        // override to create options menu
    }

    protected void prepareOptionsMenu(Menu menu) {
        // override to prepare options menu
    }
}

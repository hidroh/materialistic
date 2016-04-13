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
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;

public abstract class BaseListFragment extends BaseFragment implements Scrollable {
    private static final String STATE_ADAPTER = "state:adapter";
    @Inject CustomTabsDelegate mCustomTabsDelegate;
    protected RecyclerView mRecyclerView;
    private final SharedPreferences.OnSharedPreferenceChangeListener mListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    mChanged = true;
                    getAdapter().setCardViewEnabled(Preferences.isListItemCardView(getActivity()));
                }
            };
    private boolean mChanged;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(mListener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState == null) {
            getAdapter().setCardViewEnabled(Preferences.isListItemCardView(getActivity()));
        } else {
            getAdapter().restoreState(savedInstanceState.getBundle(STATE_ADAPTER));
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        final int verticalMargin = getResources()
                .getDimensionPixelSize(R.dimen.cardview_vertical_margin);
        final int horizontalMargin = getResources()
                .getDimensionPixelSize(R.dimen.cardview_horizontal_margin);
        final int divider = getResources().getDimensionPixelSize(R.dimen.divider);
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                if (getAdapter().isCardViewEnabled()) {
                    outRect.set(horizontalMargin, verticalMargin, horizontalMargin, 0);
                } else {
                    outRect.set(0, 0, 0, divider);
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getAdapter().setCustomTabsDelegate(mCustomTabsDelegate);
        mRecyclerView.setAdapter(getAdapter());

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChanged) {
            getAdapter().notifyDataSetChanged();
            mChanged = false;
        }
    }

    @Override
    protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list, menu);
    }

    @Override
    protected void prepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_list_toggle);
        if (getAdapter().isCardViewEnabled()) {
            item.setTitle(R.string.compact_view);
            mMenuTintDelegate.setIcon(item, R.drawable.ic_view_stream_white_24dp);
        } else {
            item.setTitle(R.string.card_view);
            mMenuTintDelegate.setIcon(item, R.drawable.ic_view_agenda_white_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.menu_list_toggle) {
            return super.onOptionsItemSelected(item);
        }
        getAdapter().setCardViewEnabled(!getAdapter().isCardViewEnabled());
        Preferences.setListItemCardView(getActivity(), getAdapter().isCardViewEnabled());
        getActivity().supportInvalidateOptionsMenu();
        getAdapter().notifyDataSetChanged();
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_ADAPTER, getAdapter().saveState());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(mListener);
    }

    @Override
    public void scrollToTop() {
        mRecyclerView.smoothScrollToPosition(0);
    }

    protected abstract ListRecyclerViewAdapter getAdapter();
}

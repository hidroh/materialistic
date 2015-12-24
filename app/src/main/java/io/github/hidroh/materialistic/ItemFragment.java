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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.widget.ItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;

public class ItemFragment extends LazyLoadFragment implements Scrollable {

    public static final String EXTRA_ITEM = ItemFragment.class.getName() + ".EXTRA_ITEM";
    private static final String STATE_ITEM = "state:item";
    private static final String STATE_ITEM_ID = "state:itemId";
    private static final String STATE_ADAPTER_ITEMS = "state:adapterItems";
    private static final String STATE_COLOR_CODED = "state:colorCoded";
    private static final String STATE_DISPLAY_OPTION = "state:displayOption";
    private static final String STATE_MAX_LINES = "state:maxLines";
    private static final String STATE_USERNAME = "state:username";
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private ItemManager.Item mItem;
    private String mItemId;
    private boolean mAttached;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SinglePageItemRecyclerViewAdapter.SavedState mAdapterItems;
    private ItemRecyclerViewAdapter mAdapter;
    private boolean mColorCoded = true;
    private String[] mDisplayOptionValues;
    private String[] mMaxLinesOptionValues;
    private String mDisplayOption;
    private int mMaxLines;
    private String mUsername;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    if (TextUtils.equals(key, getString(R.string.pref_color_code))) {
                        mColorCoded = Preferences.colorCodeEnabled(getActivity());
                        toggleColorCode();
                    } else if (TextUtils.equals(key, getString(R.string.pref_comment_display))) {
                        mDisplayOption = Preferences.getCommentDisplayOption(getActivity());
                        eagerLoad();
                    } else if (TextUtils.equals(key, getString(R.string.pref_max_lines))) {
                        mMaxLines = Preferences.getCommentMaxLines(getActivity());
                        setMaxLines();
                    } else if (TextUtils.equals(key, getString(R.string.pref_username))) {
                        mUsername = Preferences.getUsername(getActivity());
                        setHighlightUsername();
                    }
                }
            };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAttached = true;
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mItem = savedInstanceState.getParcelable(STATE_ITEM);
            mItemId = savedInstanceState.getString(STATE_ITEM_ID);
            mAdapterItems = savedInstanceState.getParcelable(STATE_ADAPTER_ITEMS);
            mColorCoded = savedInstanceState.getBoolean(STATE_COLOR_CODED);
            mDisplayOption = savedInstanceState.getString(STATE_DISPLAY_OPTION);
            mMaxLines = savedInstanceState.getInt(STATE_MAX_LINES, Integer.MAX_VALUE);
            mUsername = savedInstanceState.getString(STATE_USERNAME);
        } else {
            ItemManager.WebItem item = getArguments().getParcelable(EXTRA_ITEM);
            if (item instanceof ItemManager.Item) {
                mItem = (ItemManager.Item) item;
            }
            mItemId = item != null ? item.getId() : null;
            mColorCoded = Preferences.colorCodeEnabled(getActivity());
            mDisplayOption = Preferences.getCommentDisplayOption(getActivity());
            mMaxLines = Preferences.getCommentMaxLines(getActivity());
            mUsername = Preferences.getUsername(getActivity());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_item, container, false);
        mEmptyView = view.findViewById(android.R.id.empty);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.white);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.redA200);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (TextUtils.isEmpty(mItemId)) {
                    return;
                }

                loadKidData();
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_item_view, menu);
        mDisplayOptionValues = getResources().getStringArray(R.array.pref_comment_display_values);
        SubMenu subMenu = menu.findItem(R.id.menu_thread).getSubMenu();
        String[] options = getResources().getStringArray(R.array.pref_comment_display_options);
        for (int i = 0; i < options.length; i++) {
            subMenu.add(R.id.menu_thread_group, Menu.NONE, i, options[i]);
        }
        subMenu.setGroupCheckable(R.id.menu_thread_group, true, true);
        mMaxLinesOptionValues = getResources().getStringArray(R.array.comment_max_lines_values);
        subMenu = menu.findItem(R.id.menu_max_lines).getSubMenu();
        options = getResources().getStringArray(R.array.comment_max_lines_options);
        for (int i = 0; i < options.length; i++) {
            subMenu.add(R.id.menu_max_lines_group, Menu.NONE, i, options[i]);
        }
        subMenu.setGroupCheckable(R.id.menu_max_lines_group, true, true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem itemColorCode = menu.findItem(R.id.menu_color_code);
        itemColorCode.setEnabled(mAdapter != null &&
                mAdapter instanceof SinglePageItemRecyclerViewAdapter);
        itemColorCode.setChecked(itemColorCode.isEnabled() && mColorCoded);
        SubMenu subMenuThread = menu.findItem(R.id.menu_thread).getSubMenu();
        for (int i = 0; i < mDisplayOptionValues.length; i++) {
            if (TextUtils.equals(mDisplayOption, mDisplayOptionValues[i])) {
                subMenuThread.getItem(i).setChecked(true);
            }
        }
        SubMenu subMenuMaxLines = menu.findItem(R.id.menu_max_lines).getSubMenu();
        for (int i = 0; i < mMaxLinesOptionValues.length; i++) {
            int value = Integer.parseInt(mMaxLinesOptionValues[i]);
            if (value == -1) {
                value = Integer.MAX_VALUE;
            }
            if (mMaxLines == value) {
                subMenuMaxLines.getItem(i).setChecked(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_color_code) {
            Preferences.setColorCodeEnabled(getActivity(), !mColorCoded);
            return true;
        }
        if (item.getGroupId() == R.id.menu_thread_group) {
            if (item.isChecked()) {
                return true;
            }
            Preferences.setCommentDisplayOption(getActivity(), mDisplayOptionValues[item.getOrder()]);
            return true;
        }
        if (item.getGroupId() == R.id.menu_max_lines_group) {
            if (item.isChecked()) {
                return true;
            }
            Preferences.setCommentMaxLines(getActivity(), mMaxLinesOptionValues[item.getOrder()]);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_ITEM, mItem);
        outState.putString(STATE_ITEM_ID, mItemId);
        outState.putParcelable(STATE_ADAPTER_ITEMS, mAdapterItems);
        outState.putBoolean(STATE_COLOR_CODED, mColorCoded);
        outState.putString(STATE_DISPLAY_OPTION, mDisplayOption);
        outState.putInt(STATE_MAX_LINES, mMaxLines);
        outState.putString(STATE_USERNAME, mUsername);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRecyclerView.setAdapter(null);
        mAttached = false;
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    @Override
    public void scrollToTop() {
        mRecyclerView.smoothScrollToPosition(0);
    }

    @Override
    protected void load() {
        if (mItem != null) {
            bindKidData();
        } else if (!TextUtils.isEmpty(mItemId)) {
            loadKidData();
        }
    }

    private void loadKidData() {
        mItemManager.getItem(mItemId, new ResponseListener<ItemManager.Item>() {
            @Override
            public void onResponse(ItemManager.Item response) {
                if (!mAttached) {
                    return;
                }

                mSwipeRefreshLayout.setRefreshing(false);
                mAdapterItems = null;
                mItem = response;
                bindKidData();
            }

            @Override
            public void onError(String errorMessage) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void bindKidData() {
        if (mItem == null || mItem.getKidCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        if (Preferences.isSinglePage(getActivity(), mDisplayOption)) {
            boolean autoExpand = Preferences.isAutoExpand(getActivity(), mDisplayOption);
            // if collapsed or no saved state then start a fresh (adapter items all collapsed)
            if (!autoExpand || mAdapterItems == null) {
                mAdapterItems = new SinglePageItemRecyclerViewAdapter.SavedState(
                        new ArrayList<>(Arrays.asList(mItem.getKidItems())));
            }
            mAdapter = new SinglePageItemRecyclerViewAdapter(mItemManager, mAdapterItems, autoExpand);
            ((SinglePageItemRecyclerViewAdapter) mAdapter).toggleColorCode(mColorCoded);
        } else {
            mAdapter = new MultiPageItemRecyclerViewAdapter(mItemManager, mItem.getKidItems()
            );
        }
        mAdapter.setMaxLines(mMaxLines);
        mAdapter.setHighlightUsername(mUsername);
        invalidateOptionsMenu();
        mRecyclerView.setAdapter(mAdapter);
    }

    private void toggleColorCode() {
        if (mAdapter == null || !(mAdapter instanceof SinglePageItemRecyclerViewAdapter)) {
            return;
        }
        invalidateOptionsMenu();
        ((SinglePageItemRecyclerViewAdapter) mAdapter).toggleColorCode(mColorCoded);
    }

    private void setMaxLines() {
        if (mAdapter == null) {
            return;
        }
        invalidateOptionsMenu();
        mAdapter.setMaxLines(mMaxLines);
    }

    private void setHighlightUsername() {
        if (mAdapter == null) {
            return;
        }
        mAdapter.setHighlightUsername(mUsername);
    }

    private void invalidateOptionsMenu() {
        if (!mAttached) {
            return;
        }
        getActivity().supportInvalidateOptionsMenu();
    }
}

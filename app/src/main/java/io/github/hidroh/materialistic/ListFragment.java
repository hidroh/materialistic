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

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter;
import rx.Scheduler;

public class ListFragment extends BaseListFragment {

    public static final String EXTRA_ITEM_MANAGER = ListFragment.class.getName() + ".EXTRA_ITEM_MANAGER";
    public static final String EXTRA_FILTER = ListFragment.class.getName() + ".EXTRA_FILTER";
    private static final String STATE_FILTER = "state:filter";
    private static final String STATE_CACHE_MODE = "state:cacheMode";
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
    private final StoryRecyclerViewAdapter mAdapter = new StoryRecyclerViewAdapter();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    @Inject @Named(ActivityModule.HN) ItemManager mHnItemManager;
    @Inject @Named(ActivityModule.ALGOLIA) ItemManager mAlgoliaItemManager;
    @Inject @Named(ActivityModule.POPULAR) ItemManager mPopularItemManager;
    @Inject @Named(DataModule.IO_THREAD) Scheduler mIoThreadScheduler;
    private StoryListViewModel mStoryListViewModel;
    private View mErrorView;
    private View mEmptyView;
    private RefreshCallback mRefreshCallback;
    private String mFilter;
    private int mCacheMode = ItemManager.MODE_DEFAULT;

    public interface RefreshCallback {
        void onRefreshed();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RefreshCallback) {
            mRefreshCallback = (RefreshCallback) context;
        }
        mPreferenceObservable.subscribe(context, this::onPreferenceChanged,
                R.string.pref_highlight_updated,
                R.string.pref_username,
                R.string.pref_auto_viewed);
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFilter = savedInstanceState.getString(STATE_FILTER);
            mCacheMode = savedInstanceState.getInt(STATE_CACHE_MODE);
        } else {
            mFilter = getArguments().getString(EXTRA_FILTER);
        }
        mAdapter.initDisplayOptions(getActivity());
        mAdapter.setCacheMode(mCacheMode);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_list, container, false);
        mErrorView = view.findViewById(R.id.empty);
        mEmptyView = view.findViewById(R.id.empty_search);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.white);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(
                AppUtils.getThemedResId(getActivity(), R.attr.colorAccent));
        if (savedInstanceState == null) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mCacheMode = ItemManager.MODE_NETWORK;
            mAdapter.setCacheMode(mCacheMode);
            refresh();
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String managerClassName = getArguments().getString(EXTRA_ITEM_MANAGER);
        ItemManager itemManager;
        if (TextUtils.equals(managerClassName, AlgoliaClient.class.getName())) {
            itemManager = mAlgoliaItemManager;
        } else if (TextUtils.equals(managerClassName, AlgoliaPopularClient.class.getName())) {
            itemManager = mPopularItemManager;
        } else {
            itemManager = mHnItemManager;
        }
        mAdapter.setHotThresHold(AppUtils.HOT_THRESHOLD_NORMAL);
        if (itemManager == mHnItemManager && mFilter != null) {
            switch (mFilter) {
                case ItemManager.BEST_FETCH_MODE:
                    mAdapter.setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH);
                    break;
                case ItemManager.NEW_FETCH_MODE:
                    mAdapter.setHotThresHold(AppUtils.HOT_THRESHOLD_LOW);
                    break;
            }
        } else if (itemManager == mPopularItemManager) {
            mAdapter.setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH);
        }
        mStoryListViewModel = ViewModelProviders.of(this).get(StoryListViewModel.class);
        mStoryListViewModel.inject(itemManager, mIoThreadScheduler);
        mStoryListViewModel.getStories(mFilter, mCacheMode).observe(this, itemLists -> {
            if (itemLists == null) {
                return;
            }
            if (itemLists.first != null) {
                onItemsLoaded(itemLists.first);
            }
            if (itemLists.second != null) {
                onItemsLoaded(itemLists.second);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILTER, mFilter);
        outState.putInt(STATE_CACHE_MODE, mCacheMode);
    }

    @Override
    public void onDetach() {
        mPreferenceObservable.unsubscribe(getActivity());
        mRefreshCallback = null;
        super.onDetach();
    }

    public void filter(String filter) {
        mFilter = filter;
        mAdapter.setHighlightUpdated(false);
        mSwipeRefreshLayout.setRefreshing(true);
        refresh();
    }

    @Override
    protected ListRecyclerViewAdapter getAdapter() {
        return mAdapter;
    }

    private void onPreferenceChanged(int key, boolean contextChanged) {
        if (!contextChanged) {
            mAdapter.initDisplayOptions(getActivity());
        }
    }

    private void refresh() {
        mAdapter.setShowAll(true);
        mStoryListViewModel.refreshStories(mFilter, mCacheMode);
    }

    @Synthetic
    void onItemsLoaded(Item[] items) {
        if (!isAttached()) {
            return;
        }
        if (items == null) {
            mSwipeRefreshLayout.setRefreshing(false);
            if (mAdapter.getItems() == null || mAdapter.getItems().size() == 0) {
                // TODO make refreshing indicator visible in error view
                mEmptyView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.INVISIBLE);
                mErrorView.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getActivity(), getString(R.string.connection_error),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            mAdapter.setItems(items);
            if (items.length == 0) {
                mEmptyView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.INVISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
            mErrorView.setVisibility(View.GONE);
            mSwipeRefreshLayout.setRefreshing(false);
            if (mRefreshCallback != null) {
                mRefreshCallback.onRefreshed();
            }
        }
    }
}

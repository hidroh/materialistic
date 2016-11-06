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
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter;

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
    private ItemManager mItemManager;
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
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
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
        if (TextUtils.equals(managerClassName, AlgoliaClient.class.getName())) {
            mItemManager = mAlgoliaItemManager;
        } else if (TextUtils.equals(managerClassName, AlgoliaPopularClient.class.getName())) {
            mItemManager = mPopularItemManager;
        } else {
            mItemManager = mHnItemManager;
        }
        mAdapter.setHotThresHold(AppUtils.HOT_THRESHOLD_NORMAL);
        if (mItemManager == mHnItemManager && mFilter != null) {
            switch (mFilter) {
                case ItemManager.BEST_FETCH_MODE:
                    mAdapter.setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH);
                    break;
                case ItemManager.NEW_FETCH_MODE:
                    mAdapter.setHotThresHold(AppUtils.HOT_THRESHOLD_LOW);
                    break;
            }
        } else if (mItemManager == mPopularItemManager) {
            mAdapter.setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH);
        }
        if (mAdapter.getItems() != null) {
            mAdapter.notifyDataSetChanged();
        } else {
            refresh();
        }
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
        mItemManager.getStories(mFilter, mCacheMode, new ListResponseListener(this));
    }

    @Synthetic
    void onItemsLoaded(Item[] items) {
        if (!isAttached()) {
            return;
        }
        if (items == null) {
            mSwipeRefreshLayout.setRefreshing(false);
            if (mAdapter.getItems() == null || mAdapter.getItems().isEmpty()) {
                // TODO make refreshing indicator visible in error view
                mEmptyView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.INVISIBLE);
                mErrorView.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getActivity(), getString(R.string.connection_error),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            mAdapter.setItems(new ArrayList<>(Arrays.asList(items)));
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

    static class ListResponseListener implements ResponseListener<Item[]> {
        private final WeakReference<ListFragment> mListFragment;

        @Synthetic
        ListResponseListener(ListFragment listFragment) {
            mListFragment = new WeakReference<>(listFragment);
        }
        @Override
        public void onResponse(@Nullable final Item[] response) {
            if (mListFragment.get() != null && mListFragment.get().isAttached()) {
                mListFragment.get().onItemsLoaded(response);
            }
        }

        @Override
        public void onError(String errorMessage) {
            if (mListFragment.get() != null && mListFragment.get().isAttached()) {
                mListFragment.get().onItemsLoaded(null);
            }
        }
    }
}

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

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.MaterialisticDatabase;
import io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter;
import rx.Scheduler;

public class ListFragment extends BaseListFragment {

    public static final String EXTRA_ITEM_MANAGER = ListFragment.class.getName() + ".EXTRA_ITEM_MANAGER";
    public static final String EXTRA_FILTER = ListFragment.class.getName() + ".EXTRA_FILTER";
    private static final String STATE_FILTER = "state:filter";
    private static final String STATE_CACHE_MODE = "state:cacheMode";
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
    private final Observer<Uri> mObserver = uri -> {
        if (uri == null) {
            return;
        }
        int toastMessageResId = 0;
        if (FavoriteManager.Companion.isAdded(uri)) {
            toastMessageResId = R.string.toast_saved;
        } else if (FavoriteManager.Companion.isRemoved(uri)) {
            toastMessageResId = R.string.toast_removed;
        }
        if (toastMessageResId == 0) {
            return;
        }
        Snackbar.make(mRecyclerView, toastMessageResId, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, v -> getAdapter().toggleSave(uri.getLastPathSegment()))
                .show();
    };
    private StoryRecyclerViewAdapter mAdapter;
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
            getAdapter().setCacheMode(mCacheMode);
            refresh();
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MaterialisticDatabase.getInstance(getContext()).getLiveData().observe(this, mObserver);
        String managerClassName = getArguments().getString(EXTRA_ITEM_MANAGER);
        ItemManager itemManager;
        if (TextUtils.equals(managerClassName, AlgoliaClient.class.getName())) {
            itemManager = mAlgoliaItemManager;
        } else if (TextUtils.equals(managerClassName, AlgoliaPopularClient.class.getName())) {
            itemManager = mPopularItemManager;
        } else {
            itemManager = mHnItemManager;
        }
        getAdapter().setHotThresHold(AppUtils.HOT_THRESHOLD_NORMAL);
        if (itemManager == mHnItemManager && mFilter != null) {
            switch (mFilter) {
                case ItemManager.BEST_FETCH_MODE:
                    getAdapter().setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH);
                    break;
                case ItemManager.NEW_FETCH_MODE:
                    getAdapter().setHotThresHold(AppUtils.HOT_THRESHOLD_LOW);
                    break;
            }
        } else if (itemManager == mPopularItemManager) {
            getAdapter().setHotThresHold(AppUtils.HOT_THRESHOLD_HIGH);
        }
        getAdapter().initDisplayOptions(mRecyclerView);
        getAdapter().setCacheMode(mCacheMode);
        getAdapter().setUpdateListener((showAll, itemCount, actionClickListener) -> {
            if (showAll) {
                Snackbar.make(mRecyclerView,
                        getResources().getQuantityString(R.plurals.new_stories_count,
                                itemCount, itemCount),
                        Snackbar.LENGTH_LONG)
                        .setAction(R.string.show_me, actionClickListener)
                        .show();
            } else {
                final Snackbar snackbar = Snackbar.make(mRecyclerView,
                        getResources().getQuantityString(R.plurals.showing_new_stories,
                                itemCount, itemCount),
                        Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(R.string.show_all, actionClickListener).show();
            }

        });
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
        getAdapter().setHighlightUpdated(false);
        mSwipeRefreshLayout.setRefreshing(true);
        refresh();
    }

    @Override
    protected StoryRecyclerViewAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new StoryRecyclerViewAdapter(getContext());
        }
        return mAdapter;
    }

    private void onPreferenceChanged(int key, boolean contextChanged) {
        if (!contextChanged) {
            getAdapter().initDisplayOptions(mRecyclerView);
        }
    }

    private void refresh() {
        getAdapter().setShowAll(true);
        mStoryListViewModel.refreshStories(mFilter, mCacheMode);
    }

    @Synthetic
    void onItemsLoaded(Item[] items) {
        if (!isAttached()) {
            return;
        }
        if (items == null) {
            mSwipeRefreshLayout.setRefreshing(false);
            if (getAdapter().getItems() == null || getAdapter().getItems().size() == 0) {
                // TODO make refreshing indicator visible in error view
                mEmptyView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.INVISIBLE);
                mErrorView.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getActivity(), getString(R.string.connection_error),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            getAdapter().setItems(items);
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

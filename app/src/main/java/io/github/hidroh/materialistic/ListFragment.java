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

import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter;

public class ListFragment extends BaseListFragment {

    public static final String EXTRA_ITEM_MANAGER = ListFragment.class.getName() + ".EXTRA_ITEM_MANAGER";
    public static final String EXTRA_FILTER = ListFragment.class.getName() + ".EXTRA_FILTER";
    private static final String STATE_FILTER = "state:filter";
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals(getActivity().getString(R.string.pref_highlight_updated))) {
                        mAdapter.setHighlightUpdated(sharedPreferences.getBoolean(key, true));
                        mAdapter.notifyDataSetChanged();
                    } else if (key.equals(getActivity().getString(R.string.pref_username))) {
                        mAdapter.setUsername(Preferences.getUsername(getActivity()));
                        mAdapter.notifyDataSetChanged();
                    }
                }
            };
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

    public interface RefreshCallback {
        void onRefreshed();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RefreshCallback) {
            mRefreshCallback = (RefreshCallback) context;
        }
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFilter = savedInstanceState.getString(STATE_FILTER);
        } else {
            mFilter = getArguments().getString(EXTRA_FILTER);
            mAdapter.setHighlightUpdated(Preferences.highlightUpdatedEnabled(getActivity()));
            mAdapter.setUsername(Preferences.getUsername(getActivity()));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_list, container, false);
        mErrorView = view.findViewById(android.R.id.empty);
        mEmptyView = view.findViewById(R.id.empty_search);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.white);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(
                AppUtils.getThemedResId(getActivity(), R.attr.colorAccent));
        if (savedInstanceState == null) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
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
    }

    @Override
    public void onDetach() {
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
        mRefreshCallback = null;
        mRecyclerView.setAdapter(null); // force adapter detach
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

    private void refresh() {
        mAdapter.setShowAll(true);
        mItemManager.getStories(mFilter, new ListResponseListener(this));
    }

    private void onItemsLoaded(ItemManager.Item[] items) {
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

    private static class ListResponseListener implements ResponseListener<ItemManager.Item[]> {
        private final WeakReference<ListFragment> mListFragment;

        public ListResponseListener(ListFragment listFragment) {
            mListFragment = new WeakReference<>(listFragment);
        }
        @Override
        public void onResponse(final ItemManager.Item[] response) {
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

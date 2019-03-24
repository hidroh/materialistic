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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.widget.CommentItemDecoration;
import io.github.hidroh.materialistic.widget.ItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SnappyLinearLayoutManager;

public class ItemFragment extends LazyLoadFragment implements Scrollable, Navigable {

    public static final String EXTRA_ITEM = ItemFragment.class.getName() + ".EXTRA_ITEM";
    public static final String EXTRA_CACHE_MODE = ItemFragment.class.getName() + ".EXTRA_CACHE_MODE";
    private static final String STATE_ITEM = "state:item";
    private static final String STATE_ITEM_ID = "state:itemId";
    private static final String STATE_ADAPTER_ITEMS = "state:adapterItems";
    private static final String STATE_CACHE_MODE = "state:cacheMode";
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private Item mItem;
    private String mItemId;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SinglePageItemRecyclerViewAdapter.SavedState mAdapterItems;
    private ItemRecyclerViewAdapter mAdapter;
    private KeyDelegate.RecyclerViewHelper mScrollableHelper;
    private @ItemManager.CacheMode int mCacheMode = ItemManager.MODE_DEFAULT;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
    private CommentItemDecoration mItemDecoration;
    private View mFragmentView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPreferenceObservable.subscribe(context, this::onPreferenceChanged,
                R.string.pref_comment_display,
                R.string.pref_max_lines,
                R.string.pref_username,
                R.string.pref_line_height,
                R.string.pref_color_code,
                R.string.pref_thread_indicator,
                R.string.pref_font,
                R.string.pref_text_size,
                R.string.pref_smooth_scroll,
                R.string.pref_color_code_opacity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mCacheMode = savedInstanceState.getInt(STATE_CACHE_MODE, ItemManager.MODE_DEFAULT);
            mItem = savedInstanceState.getParcelable(STATE_ITEM);
            mItemId = savedInstanceState.getString(STATE_ITEM_ID);
            mAdapterItems = savedInstanceState.getParcelable(STATE_ADAPTER_ITEMS);
        } else {
            mCacheMode = getArguments().getInt(EXTRA_CACHE_MODE, ItemManager.MODE_DEFAULT);
            WebItem item = getArguments().getParcelable(EXTRA_ITEM);
            if (item instanceof Item) {
                mItem = (Item) item;
            }
            mItemId = item != null ? item.getId() : null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        if (isNewInstance()) {
            mFragmentView = inflater.inflate(R.layout.fragment_item, container, false);
            mEmptyView = mFragmentView.findViewById(R.id.empty);
            mRecyclerView = (RecyclerView) mFragmentView.findViewById(R.id.recycler_view);
            mRecyclerView.setLayoutManager(new SnappyLinearLayoutManager(getActivity(), true));
            mItemDecoration = new CommentItemDecoration(getActivity());
            mRecyclerView.addItemDecoration(mItemDecoration);
            mSwipeRefreshLayout = (SwipeRefreshLayout) mFragmentView.findViewById(R.id.swipe_layout);
            mSwipeRefreshLayout.setColorSchemeResources(R.color.white);
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.redA200);
            mSwipeRefreshLayout.setOnRefreshListener(() -> {
                if (TextUtils.isEmpty(mItemId)) {
                    return;
                }
                mCacheMode = ItemManager.MODE_NETWORK;
                if (mAdapter != null) {
                    mAdapter.setCacheMode(mCacheMode);
                }
                loadKidData();
            });
        }
        return mFragmentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isNewInstance()) {
            mScrollableHelper = new KeyDelegate.RecyclerViewHelper(mRecyclerView,
                    KeyDelegate.RecyclerViewHelper.SCROLL_ITEM);
            mScrollableHelper.smoothScrollEnabled(Preferences.smoothScrollEnabled(getActivity()));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_comments) {
            showPreferences();
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
        outState.putInt(STATE_CACHE_MODE, mCacheMode);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.detach(getActivity(), mRecyclerView);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPreferenceObservable.unsubscribe(getActivity());
    }

    @Override
    public void scrollToTop() {
        mScrollableHelper.scrollToTop();
    }

    @Override
    public boolean scrollToNext() {
        return mScrollableHelper.scrollToNext();
    }

    @Override
    public boolean scrollToPrevious() {
        return mScrollableHelper.scrollToPrevious();
    }

    @Override
    public void onNavigate(int direction) {
        if (mAdapter == null) { // no kids
            return;
        }
        mAdapter.getNextPosition(mScrollableHelper.getCurrentPosition(),
                direction,
                position -> mAdapter.lockBinding(mScrollableHelper.scrollToPosition(position)));
    }

    @Override
    protected void load() {
        if (mItem != null) {
            bindKidData();
        } else if (!TextUtils.isEmpty(mItemId)) {
            loadKidData();
        }
    }

    @Override
    protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_item_view, menu);
    }

    private void loadKidData() {
        mItemManager.getItem(mItemId, mCacheMode, new ItemResponseListener(this));
    }

    void onItemLoaded(@Nullable Item item) {
        mSwipeRefreshLayout.setRefreshing(false);
        if (item != null) {
            mAdapterItems = null;
            mItem = item;
            notifyItemLoaded(item);
            bindKidData();
        }
    }

    private void bindKidData() {
        if (mItem == null || mItem.getKidCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        mEmptyView.setVisibility(View.GONE);
        String displayOption = Preferences.getCommentDisplayOption(getActivity());
        if (Preferences.isSinglePage(getActivity(), displayOption)) {
            boolean autoExpand = Preferences.isAutoExpand(getActivity(), displayOption);
            // if collapsed or no saved state then start a fresh (adapter items all collapsed)
            if (!autoExpand || mAdapterItems == null) {
                mAdapterItems = new SinglePageItemRecyclerViewAdapter.SavedState(
                        new ArrayList<>(Arrays.asList(mItem.getKidItems())));
            }
            mAdapter = new SinglePageItemRecyclerViewAdapter(mItemManager, mAdapterItems, autoExpand);
        } else {
            mAdapter = new MultiPageItemRecyclerViewAdapter(mItemManager, mItem.getKidItems());
        }
        mAdapter.setCacheMode(mCacheMode);
        mAdapter.initDisplayOptions(getActivity());
        mAdapter.attach(getActivity(), mRecyclerView);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void onPreferenceChanged(int key, boolean contextChanged) {
        if (contextChanged || key == R.string.pref_comment_display) {
            load();
        } else if (mAdapter != null) {
            mScrollableHelper.smoothScrollEnabled(Preferences.smoothScrollEnabled(getActivity()));
            mItemDecoration.setColorCodeEnabled(Preferences.colorCodeEnabled(getActivity()));
            mItemDecoration.setThreadIndicatorEnabled(Preferences.threadIndicatorEnabled(getActivity()));
            mAdapter.initDisplayOptions(getActivity());
            mAdapter.notifyDataSetChanged();
        }
    }

    private void showPreferences() {
        Bundle args = new Bundle();
        args.putInt(PopupSettingsFragment.EXTRA_TITLE, R.string.font_options);
        args.putInt(PopupSettingsFragment.EXTRA_SUMMARY, R.string.pull_up_hint);
        args.putIntArray(PopupSettingsFragment.EXTRA_XML_PREFERENCES, new int[]{
                R.xml.preferences_font,
                R.xml.preferences_comments});
        ((DialogFragment) Fragment.instantiate(getActivity(),
                PopupSettingsFragment.class.getName(), args))
                .show(getFragmentManager(), PopupSettingsFragment.class.getName());
    }

    private void notifyItemLoaded(@NonNull Item item) {
        if (getActivity() instanceof ItemChangedListener) {
            ((ItemChangedListener) getActivity()).onItemChanged(item);
        }
    }

    static class ItemResponseListener implements ResponseListener<Item> {
        private WeakReference<ItemFragment> mItemFragment;

        @Synthetic
        ItemResponseListener(ItemFragment itemFragment) {
            mItemFragment = new WeakReference<>(itemFragment);
        }

        @Override
        public void onResponse(@Nullable Item response) {
            if (mItemFragment.get() != null && mItemFragment.get().isAttached()) {
                mItemFragment.get().onItemLoaded(response);
            }
        }

        @Override
        public void onError(String errorMessage) {
            if (mItemFragment.get() != null && mItemFragment.get().isAttached()) {
                mItemFragment.get().onItemLoaded(null);
            }
        }
    }

    interface ItemChangedListener {
        void onItemChanged(@NonNull Item item);
    }
}

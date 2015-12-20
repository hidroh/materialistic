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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LongSparseArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.MaterialisticProvider;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.PopupMenu;

public class ListFragment extends BaseListFragment {

    public static final String EXTRA_ITEM_MANAGER = ListFragment.class.getName() + ".EXTRA_ITEM_MANAGER";
    public static final String EXTRA_FILTER = ListFragment.class.getName() + ".EXTRA_FILTER";
    private static final String STATE_ITEMS = "state:items";
    private static final String STATE_FILTER = "state:filter";
    private static final String STATE_UPDATED = "state:updated";
    private static final String STATE_SHOW_ALL = "state:showAll";
    private static final String STATE_GREEN_ITEMS = "state:greenItems";
    private static final String STATE_HIGHLIGHT_UPDATED = "state:highlightUpdated";
    private static final String STATE_USERNAME = "state:username";
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals(getActivity().getString(R.string.pref_highlight_updated))) {
                        mHighlightUpdated = sharedPreferences.getBoolean(key, true);
                        mAdapter.notifyDataSetChanged();
                    } else if (key.equals(getActivity().getString(R.string.pref_username))) {
                        mUsername = Preferences.getUsername(getActivity());
                        mAdapter.notifyDataSetChanged();
                    }
                }
            };
    private final ListRecyclerViewAdapter mAdapter = new RecyclerViewAdapter();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BroadcastReceiver mBroadcastReceiver;
    private int mLocalRevision = 0;
    private ArrayList<ItemManager.Item> mItems;
    private ArrayList<ItemManager.Item> mUpdated = new ArrayList<>();
    private ArrayList<String> mGreenItems = new ArrayList<>();
    private LongSparseArray<ItemManager.Item> mItemIdMaps = new LongSparseArray<>();
    private ItemManager mItemManager;
    @Inject @Named(ActivityModule.HN) ItemManager mHnItemManager;
    @Inject @Named(ActivityModule.ALGOLIA) ItemManager mAlgoliaItemManager;
    @Inject @Named(ActivityModule.POPULAR) ItemManager mPopularItemManager;
    @Inject UserServices mUserServices;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    @Inject PopupMenu mPopupMenu;
    private View mErrorView;
    private View mEmptyView;
    private Set<String> mChangedFavorites = new HashSet<>();
    private MultiPaneListener mMultiPaneListener;
    private RefreshCallback mRefreshCallback;
    private String mFilter;
    @Inject FavoriteManager mFavoriteManager;
    private boolean mResumed;
    private boolean mShowAll = true;
    private boolean mHighlightUpdated = true;
    private boolean mAttached;
    private String mUsername;

    public interface RefreshCallback {
        void onRefreshed();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMultiPaneListener = (MultiPaneListener) context;
        if (context instanceof RefreshCallback) {
            mRefreshCallback = (RefreshCallback) context;
        }
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (FavoriteManager.ACTION_CLEAR.equals(intent.getAction())) {
                    mLocalRevision++;
                } else if (FavoriteManager.ACTION_ADD.equals(intent.getAction())) {
                    mChangedFavorites.add(intent.getStringExtra(FavoriteManager.ACTION_ADD_EXTRA_DATA));
                } else if (FavoriteManager.ACTION_REMOVE.equals(intent.getAction())) {
                    mChangedFavorites.add(intent.getStringExtra(FavoriteManager.ACTION_REMOVE_EXTRA_DATA));
                }
                if (!mResumed) {
                    // refresh favorite/viewed state if any changes
                    mAdapter.notifyDataSetChanged();
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, FavoriteManager.makeClearIntentFilter());
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, FavoriteManager.makeAddIntentFilter());
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, FavoriteManager.makeRemoveIntentFilter());
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(mPreferenceListener);
        mAttached = true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<ItemManager.Item> savedItems = savedInstanceState.getParcelableArrayList(STATE_ITEMS);
            setItems(savedItems);
            mUpdated = savedInstanceState.getParcelableArrayList(STATE_UPDATED);
            mGreenItems = savedInstanceState.getStringArrayList(STATE_GREEN_ITEMS);
            mFilter = savedInstanceState.getString(STATE_FILTER);
            mShowAll = savedInstanceState.getBoolean(STATE_SHOW_ALL, true);
            mHighlightUpdated = savedInstanceState.getBoolean(STATE_HIGHLIGHT_UPDATED, true);
            mUsername = savedInstanceState.getString(STATE_USERNAME);
        } else {
            mFilter = getArguments().getString(EXTRA_FILTER);
            mHighlightUpdated = Preferences.highlightUpdatedEnabled(getActivity());
            mUsername = Preferences.getUsername(getActivity());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        if (mItems != null) {
            bindData();
        } else {
            refresh();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        mResumed = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_ITEMS, mItems);
        outState.putParcelableArrayList(STATE_UPDATED, mUpdated);
        outState.putStringArrayList(STATE_GREEN_ITEMS, mGreenItems);
        outState.putString(STATE_FILTER, mFilter);
        outState.putBoolean(STATE_SHOW_ALL, mShowAll);
        outState.putBoolean(STATE_HIGHLIGHT_UPDATED, mHighlightUpdated);
    }

    @Override
    public void onPause() {
        mResumed = false;
        super.onPause();
    }

    @Override
    public void onDetach() {
        mAttached = false;
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
        mBroadcastReceiver = null;
        mMultiPaneListener = null;
        mRefreshCallback = null;
        super.onDetach();
    }

    public void filter(String filter) {
        mFilter = filter;
        setItems(null); // prevent updated comparison
        mSwipeRefreshLayout.setRefreshing(true);
        refresh();
    }

    @Override
    protected ListRecyclerViewAdapter getAdapter() {
        return mAdapter;
    }

    private void refresh() {
        mShowAll = true;
        mItemManager.getStories(mFilter, new ResponseListener<ItemManager.Item[]>() {
            @Override
            public void onResponse(final ItemManager.Item[] response) {
                if (!mAttached) {
                    return;
                }
                if (response == null) {
                    onError(null);
                } else {
                    ArrayList<ItemManager.Item> updated = new ArrayList<>(Arrays.asList(response));
                    bindUpdated(updated);
                    setItems(updated);
                    bindData();
                    if (mRefreshCallback != null) {
                        mRefreshCallback.onRefreshed();
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!mAttached) {
                    return;
                }
                mSwipeRefreshLayout.setRefreshing(false);
                if (mItems == null || mItems.isEmpty()) {
                    // TODO make refreshing indicator visible in error view
                    mEmptyView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.INVISIBLE);
                    mErrorView.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.connection_error),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindUpdated(ArrayList<ItemManager.Item> updated) {
        if (!mHighlightUpdated) {
            return;
        }
        if (mItems == null) {
            return;
        }
        mUpdated.clear();
        mGreenItems.clear();
        for (ItemManager.Item item : updated) {
            ItemManager.Item currentRevision = mItemIdMaps.get(Long.valueOf(item.getId()));
            if (currentRevision == null) {
                mUpdated.add(item);
            } else {
                item.setLastKidCount(currentRevision.getLastKidCount());
                int lastRank = currentRevision.getRank();
                if (lastRank > item.getRank()) {
                    mGreenItems.add(item.getId());
                }
            }
        }
        if (mUpdated.isEmpty()) {
            return;
        }
        Snackbar.make(mRecyclerView,
                getString(R.string.new_stories_count, mUpdated.size()),
                Snackbar.LENGTH_LONG)
                .setAction(R.string.show_me, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mShowAll = false;
                        bindData();
                    }
                })
                .show();
    }

    private void bindData() {
        if (!mShowAll) {
            final Snackbar snackbar = Snackbar.make(mRecyclerView,
                    getString(R.string.showing_new_stories, mUpdated.size()),
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.show_all, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                    mUpdated.clear();
                    mShowAll = true;
                    bindData();
                }
            }).show();
        }
        if (mItems == null || mItems.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        mErrorView.setVisibility(View.GONE);
        mAdapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void setItems(ArrayList<ItemManager.Item> items) {
        mItems = items;
        mItemIdMaps.clear();
        if (items != null) {
            for (ItemManager.Item item : items) {
                mItemIdMaps.put(Long.valueOf(item.getId()), item);
            }
        }
    }

    private class RecyclerViewAdapter extends ListRecyclerViewAdapter<ListRecyclerViewAdapter.ItemViewHolder, ItemManager.Item> {
        private final ContentObserver mObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                ItemManager.Item item = mItemIdMaps.get(Long.valueOf(uri.getLastPathSegment()));
                if (item != null) {
                    item.setIsViewed(true);
                    notifyDataSetChanged();
                }
            }
        };

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            recyclerView.getContext().getContentResolver()
                    .registerContentObserver(MaterialisticProvider.URI_VIEWED, true, mObserver);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            recyclerView.getContext().getContentResolver().unregisterContentObserver(mObserver);
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ItemViewHolder(getLayoutInflater(null)
                    .inflate(R.layout.item_story, parent, false));
        }

        @Override
        public int getItemCount() {
            if (mShowAll) {
                return mItems != null ? mItems.size() : 0;
            } else {
                return mUpdated.size();
            }
        }

        @Override
        protected void loadItem(final int adapterPosition) {
            final ItemManager.Item partialStory = getItem(adapterPosition);
            mItemManager.getItem(partialStory.getId(), new ResponseListener<ItemManager.Item>() {
                @Override
                public void onResponse(ItemManager.Item response) {
                    if (!mResumed || response == null) {
                        return;
                    }
                    partialStory.populate(response);
                    notifyItemChanged(adapterPosition);
                }

                @Override
                public void onError(String errorMessage) {
                    // do nothing
                }
            });
        }

        @Override
        protected void bindItem(final ItemViewHolder holder) {
            final ItemManager.Item story = getItem(holder.getAdapterPosition());
            bindUpdated(holder, story);
            bindFavorite(holder, story);
            bindViewed(holder, story);
            highlightUserPost(holder, story);
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showMoreOptions(holder.mStoryView.getMoreOptions(), story, holder);
                    return true;
                }
            });
            holder.mStoryView.getMoreOptions().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMoreOptions(v, story, holder);
                }
            });
        }

        @Override
        protected boolean isItemAvailable(ItemManager.Item item) {
            return item != null && !TextUtils.isEmpty(item.getTitle());
        }

        @Override
        protected void onItemSelected(ItemManager.Item item, View itemView) {
            mMultiPaneListener.onItemSelected(item);
        }

        @Override
        protected boolean isSelected(String itemId) {
            return mMultiPaneListener.getSelectedItem() != null &&
                    itemId.equals(mMultiPaneListener.getSelectedItem().getId());
        }

        @Override
        protected ItemManager.Item getItem(int position) {
            if (mShowAll) {
                return mItems.get(position);
            } else {
                return mUpdated.get(position);
            }
        }

        @Override
        protected boolean shouldCompact() {
            return !mCardView;
        }

        private void bindUpdated(ItemViewHolder holder, ItemManager.Item story) {
            if (mHighlightUpdated) {
                holder.mStoryView.setUpdated(story, mUpdated, mGreenItems);
            }
        }

        private void bindViewed(final ItemViewHolder holder, final ItemManager.Item story) {
            holder.mStoryView.setViewed(story.isViewed());
        }

        private void bindFavorite(final ItemViewHolder holder, final ItemManager.Item story) {
            if (story.getLocalRevision() < mLocalRevision || mChangedFavorites.contains(story.getId())) {
                story.setLocalRevision(mLocalRevision);
                mChangedFavorites.remove(story.getId());
                mFavoriteManager.check(getActivity(), story.getId(),
                        new FavoriteManager.OperationCallbacks() {
                            @Override
                            public void onCheckComplete(boolean isFavorite) {
                                story.setFavorite(isFavorite);
                                holder.mStoryView.setFavorite(story.isFavorite());
                            }

                        });
            } else {
                holder.mStoryView.setFavorite(story.isFavorite());
            }
        }

        private void showMoreOptions(View v, final ItemManager.Item story, final ItemViewHolder holder) {
            mPopupMenu.create(getActivity(), v, Gravity.NO_GRAVITY);
            mPopupMenu.inflate(R.menu.menu_contextual_story);
            mPopupMenu.getMenu().findItem(R.id.menu_contextual_save)
                    .setTitle(story.isFavorite() ? R.string.unsave : R.string.save);
            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.menu_contextual_save) {
                        toggleSave(story, holder);
                        return true;
                    }
                    if (item.getItemId() == R.id.menu_contextual_vote) {
                        vote(story, holder);
                        return true;
                    }
                    if (item.getItemId() == R.id.menu_contextual_comment) {
                        Intent intent = new Intent(getActivity(), ComposeActivity.class);
                        intent.putExtra(ComposeActivity.EXTRA_PARENT_ID, story.getId());
                        intent.putExtra(ComposeActivity.EXTRA_PARENT_TEXT, story.getDisplayedTitle());
                        startActivity(intent);
                        return true;
                    }
                    if (item.getItemId() == R.id.menu_contextual_profile) {
                        startActivity(new Intent(getActivity(), UserActivity.class)
                                .putExtra(UserActivity.EXTRA_USERNAME, story.getBy()));
                        return true;
                    }
                    return false;
                }
            });
            mPopupMenu.show();
        }

        private void toggleSave(final ItemManager.Item story, final ItemViewHolder holder) {
            final int toastMessageResId;
            if (!story.isFavorite()) {
                mFavoriteManager.add(getActivity(), story);
                toastMessageResId = R.string.toast_saved;
            } else {
                mFavoriteManager.remove(getActivity(), story.getId());
                toastMessageResId = R.string.toast_removed;
            }
            Snackbar.make(mRecyclerView, toastMessageResId, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            toggleSave(story, holder);
                        }
                    })
                    .show();
            story.setFavorite(!story.isFavorite());
            holder.mStoryView.setFavorite(story.isFavorite());
        }

        private void vote(final ItemManager.Item story, final ItemViewHolder holder) {
            mUserServices.voteUp(getActivity(), story.getId(), new UserServices.Callback() {
                @Override
                public void onDone(boolean successful) {
                    if (successful) {
                        // TODO update locally only, as API does not update instantly
                        story.incrementScore();
                        holder.mStoryView.animateVote(story.getScore());
                        Toast.makeText(getActivity(), R.string.voted, Toast.LENGTH_SHORT).show();
                    } else {
                        AppUtils.showLogin(getActivity(), mAlertDialogBuilder);
                    }
                }

                @Override
                public void onError() {
                    Toast.makeText(getActivity(), R.string.vote_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void highlightUserPost(ListRecyclerViewAdapter.ItemViewHolder holder,
                                       ItemManager.Item story) {
            holder.mStoryView.setChecked(isSelected(story.getId()) ||
                    !TextUtils.isEmpty(mUsername) &&
                    TextUtils.equals(mUsername, story.getBy()));
        }

    }
}

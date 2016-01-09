/*
 * Copyright (c) 2016 Ha Duy Trung
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

package io.github.hidroh.materialistic.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.AlertDialogBuilder;
import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.ComposeActivity;
import io.github.hidroh.materialistic.Injectable;
import io.github.hidroh.materialistic.MultiPaneListener;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.UserActivity;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.MaterialisticProvider;
import io.github.hidroh.materialistic.data.ResponseListener;

public class StoryRecyclerViewAdapter extends
        ListRecyclerViewAdapter<ListRecyclerViewAdapter.ItemViewHolder, ItemManager.Item> {
    private static final String STATE_ITEMS = "state:items";
    private static final String STATE_UPDATED = "state:updated";
    private static final String STATE_PROMOTED = "state:promoted";
    private static final String STATE_SHOW_ALL = "state:showAll";
    private static final String STATE_HIGHLIGHT_UPDATED = "state:highlightUpdated";
    private static final String STATE_FAVORITE_REVISION = "state:favoriteRevision";
    private static final String STATE_USERNAME = "state:username";
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (FavoriteManager.isCleared(uri)) {
                mFavoriteRevision++; // invalidate all favorite statuses
                notifyDataSetChanged();
                return;
            }
            Integer position = mItemPositions.get(Long.valueOf(uri.getLastPathSegment()));
            if (position == null) {
                return;
            }
            ItemManager.Item item = mItems.get(position);
            if (FavoriteManager.isAdded(uri)) {
                item.setFavorite(true);
                item.setLocalRevision(mFavoriteRevision);
            } else if (FavoriteManager.isRemoved(uri)) {
                item.setFavorite(false);
                item.setLocalRevision(mFavoriteRevision);
            } else {
                item.setIsViewed(true);
            }
            notifyItemChanged(position);
        }
    };
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private MultiPaneListener mMultiPaneListener;
    private RecyclerView mRecyclerView;
    @Inject PopupMenu mPopupMenu;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    @Inject UserServices mUserServices;
    @Inject FavoriteManager mFavoriteManager;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    private ArrayList<ItemManager.Item> mItems;
    private ArrayList<ItemManager.Item> mUpdated = new ArrayList<>();
    private ArrayList<String> mPromoted = new ArrayList<>();
    private final LongSparseArray<Integer> mItemPositions = new LongSparseArray<>();
    private final LongSparseArray<Integer> mUpdatedPositions = new LongSparseArray<>();
    private int mFavoriteRevision = -1;
    private String mUsername;
    private boolean mHighlightUpdated = true;
    private boolean mShowAll = true;

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
        mContext = recyclerView.getContext();
        mMultiPaneListener = (MultiPaneListener) mContext;
        ((Injectable) mContext).inject(this);
        mLayoutInflater = LayoutInflater.from(mContext);
        ContentResolver cr = recyclerView.getContext().getContentResolver();
        cr.registerContentObserver(MaterialisticProvider.URI_VIEWED, true, mObserver);
        cr.registerContentObserver(MaterialisticProvider.URI_FAVORITE, true, mObserver);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        recyclerView.getContext().getContentResolver().unregisterContentObserver(mObserver);
        mRecyclerView = null;
        mContext = null;
        mMultiPaneListener = null;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(mLayoutInflater.inflate(R.layout.item_story, parent, false));
    }

    @Override
    public int getItemCount() {
        if (mShowAll) {
            return mItemPositions.size();
        } else {
            return mUpdatedPositions.size();
        }
    }

    @Override
    public Bundle saveState() {
        Bundle savedState = super.saveState();
        savedState.putParcelableArrayList(STATE_ITEMS, mItems);
        savedState.putParcelableArrayList(STATE_UPDATED, mUpdated);
        savedState.putStringArrayList(STATE_PROMOTED, mPromoted);
        savedState.putBoolean(STATE_SHOW_ALL, mShowAll);
        savedState.putBoolean(STATE_HIGHLIGHT_UPDATED, mHighlightUpdated);
        savedState.putInt(STATE_FAVORITE_REVISION, mFavoriteRevision);
        savedState.putString(STATE_USERNAME, mUsername);
        return savedState;
    }

    @Override
    public void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }
        super.restoreState(savedState);
        ArrayList<ItemManager.Item> savedItems = savedState.getParcelableArrayList(STATE_ITEMS);
        setItemsInternal(savedItems);
        mUpdated = savedState.getParcelableArrayList(STATE_UPDATED);
        if (mUpdated != null) {
            for (int i = 0; i < mUpdated.size(); i++) {
                mUpdatedPositions.put(mUpdated.get(i).getLongId(), i);
            }
        }
        mPromoted = savedState.getStringArrayList(STATE_PROMOTED);
        mShowAll = savedState.getBoolean(STATE_SHOW_ALL, true);
        mHighlightUpdated = savedState.getBoolean(STATE_HIGHLIGHT_UPDATED, true);
        mFavoriteRevision = savedState.getInt(STATE_FAVORITE_REVISION);
        mUsername = savedState.getString(STATE_USERNAME);
    }

    public ArrayList<ItemManager.Item> getItems() {
        return mItems;
    }

    public void setItems(ArrayList<ItemManager.Item> items) {
        mMultiPaneListener.onItemSelected(null);
        setUpdated(items);
        setItemsInternal(items);
        notifyDataSetChanged();
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public void setHighlightUpdated(boolean highlightUpdated) {
        mHighlightUpdated = highlightUpdated;
    }

    public void setShowAll(boolean showAll) {
        mShowAll = showAll;
    }

    @Override
    protected void loadItem(final int adapterPosition) {
        ItemManager.Item item = getItem(adapterPosition);
        mItemManager.getItem(item.getId(), new ItemResponseListener(this, item));
    }

    @Override
    protected void bindItem(final ItemViewHolder holder) {
        final ItemManager.Item story = getItem(holder.getAdapterPosition());
        bindItemUpdated(holder, story);
        highlightUserPost(holder, story);
        holder.mStoryView.setViewed(story.isViewed());
        if (story.getLocalRevision() < mFavoriteRevision) {
            story.setFavorite(false);
        }
        holder.mStoryView.setFavorite(story.isFavorite());
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
        return mMultiPaneListener.isMultiPane() &&
                mMultiPaneListener.getSelectedItem() != null &&
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

    private void setItemsInternal(ArrayList<ItemManager.Item> items) {
        mItems = items;
        mItemPositions.clear();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                mItemPositions.put(items.get(i).getLongId(), i);
            }
        }
    }

    private void setUpdated(ArrayList<ItemManager.Item> items) {
        if (!mHighlightUpdated || getItems() == null) {
            return;
        }
        mUpdated.clear();
        mUpdatedPositions.clear();
        mPromoted.clear();
        for (ItemManager.Item item : items) {
            Integer position = mItemPositions.get(item.getLongId());
            if (position == null) {
                mUpdated.add(item);
                mUpdatedPositions.put(item.getLongId(), mUpdated.size() - 1);
            } else {
                ItemManager.Item currentRevision = mItems.get(position);
                item.setLastKidCount(currentRevision.getLastKidCount());
                int lastRank = currentRevision.getRank();
                if (lastRank > item.getRank()) {
                    mPromoted.add(item.getId());
                }
            }
        }
        if (!mUpdated.isEmpty()) {
            notifyUpdated();
        }
    }

    private void notifyUpdated() {
        if (mShowAll) {
            Snackbar.make(mRecyclerView,
                    mContext.getResources().getQuantityString(R.plurals.new_stories_count,
                            mUpdated.size(), mUpdated.size()),
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.show_me, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            setShowAll(false);
                            notifyUpdated();
                            notifyDataSetChanged();
                        }
                    })
                    .show();
        } else {
            final Snackbar snackbar = Snackbar.make(mRecyclerView,
                    mContext.getResources().getQuantityString(R.plurals.showing_new_stories,
                            mUpdated.size(), mUpdated.size()),
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.show_all, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                    mUpdated.clear();
                    setShowAll(true);
                    notifyDataSetChanged();
                }
            }).show();
        }
    }

    private void onItemLoaded(ItemManager.Item item) {
        Integer position = mShowAll ? mItemPositions.get(item.getLongId()) :
                mUpdatedPositions.get(item.getLongId());
        // ignore changes if item was invalidated by refresh / filter
        if (position != null && position >= 0 && position < getItemCount()) {
            notifyItemChanged(position);
        }
    }

    private void bindItemUpdated(ItemViewHolder holder, ItemManager.Item story) {
        if (mHighlightUpdated) {
            holder.mStoryView.setUpdated(story,
                    mUpdatedPositions.indexOfKey(story.getLongId()) >= 0,
                    mPromoted.contains(story.getId()));
        }
    }

    private void showMoreOptions(View v, final ItemManager.Item story, final ItemViewHolder holder) {
        mPopupMenu.create(mContext, v, Gravity.NO_GRAVITY);
        mPopupMenu.inflate(R.menu.menu_contextual_story);
        mPopupMenu.getMenu().findItem(R.id.menu_contextual_save)
                .setTitle(story.isFavorite() ? R.string.unsave : R.string.save);
        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_contextual_save) {
                    toggleSave(story);
                    return true;
                }
                if (item.getItemId() == R.id.menu_contextual_vote) {
                    vote(story, holder);
                    return true;
                }
                if (item.getItemId() == R.id.menu_contextual_comment) {
                    mContext.startActivity(new Intent(mContext, ComposeActivity.class)
                            .putExtra(ComposeActivity.EXTRA_PARENT_ID, story.getId())
                            .putExtra(ComposeActivity.EXTRA_PARENT_TEXT,
                                    story.getDisplayedTitle()));
                    return true;
                }
                if (item.getItemId() == R.id.menu_contextual_profile) {
                    mContext.startActivity(new Intent(mContext, UserActivity.class)
                            .putExtra(UserActivity.EXTRA_USERNAME, story.getBy()));
                    return true;
                }
                return false;
            }
        });
        mPopupMenu.show();
    }

    private void toggleSave(final ItemManager.Item story) {
        final int toastMessageResId;
        if (!story.isFavorite()) {
            mFavoriteManager.add(mContext, story);
            toastMessageResId = R.string.toast_saved;
        } else {
            mFavoriteManager.remove(mContext, story.getId());
            toastMessageResId = R.string.toast_removed;
        }
        Snackbar.make(mRecyclerView, toastMessageResId, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleSave(story);
                    }
                })
                .show();
    }

    private void vote(final ItemManager.Item story, final ItemViewHolder holder) {
        mUserServices.voteUp(mContext, story.getId(),
                new VoteCallback(this, holder.getAdapterPosition(), story));
    }

    private void onVoted(int position, Boolean successful) {
        if (successful == null) {
            Toast.makeText(mContext, R.string.vote_failed, Toast.LENGTH_SHORT).show();
        } else if (successful) {
            Toast.makeText(mContext, R.string.voted, Toast.LENGTH_SHORT).show();
            if (position < getItemCount()) {
                notifyItemChanged(position);
            }
        } else {
            AppUtils.showLogin(mContext, mAlertDialogBuilder);
        }
    }

    private void highlightUserPost(ItemViewHolder holder,
                                   ItemManager.Item story) {
        holder.mStoryView.setChecked(isSelected(story.getId()) ||
                !TextUtils.isEmpty(mUsername) &&
                TextUtils.equals(mUsername, story.getBy()));
    }

    private static class ItemResponseListener implements ResponseListener<ItemManager.Item> {
        private final WeakReference<StoryRecyclerViewAdapter> mAdapter;
        private final ItemManager.Item mPartialItem;

        public ItemResponseListener(StoryRecyclerViewAdapter adapter,
                                    ItemManager.Item partialItem) {
            mAdapter = new WeakReference<>(adapter);
            mPartialItem = partialItem;
        }

        @Override
        public void onResponse(ItemManager.Item response) {
            if (mAdapter.get() != null && mAdapter.get().isAttached() && response != null) {
                mPartialItem.populate(response);
                mAdapter.get().onItemLoaded(mPartialItem);
            }
        }

        @Override
        public void onError(String errorMessage) {
            // do nothing
        }
    }

    private static class VoteCallback extends UserServices.Callback {
        private final WeakReference<StoryRecyclerViewAdapter> mAdapter;
        private final int mPosition;
        private final ItemManager.Item mItem;

        public VoteCallback(StoryRecyclerViewAdapter adapter, int position,
                            ItemManager.Item item) {
            mAdapter = new WeakReference<>(adapter);
            mPosition = position;
            mItem = item;
        }

        @Override
        public void onDone(boolean successful) {
            // TODO update locally only, as API does not update instantly
            mItem.incrementScore();
            if (mAdapter.get() != null && mAdapter.get().isAttached()) {
                mAdapter.get().onVoted(mPosition, successful);
            }
        }

        @Override
        public void onError() {
            if (mAdapter.get() != null && mAdapter.get().isAttached()) {
                mAdapter.get().onVoted(mPosition, null);
            }
        }
    }
}

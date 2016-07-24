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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.ComposeActivity;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.UserActivity;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.MaterialisticProvider;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.SessionManager;

public class StoryRecyclerViewAdapter extends
        ListRecyclerViewAdapter<ListRecyclerViewAdapter.ItemViewHolder, Item> {
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
            Item item = mItems.get(position);
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
    private final RecyclerView.OnScrollListener mAutoViewScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (dy > 0) { // scrolling down
                markAsViewed(((LinearLayoutManager) recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition() - 1);
            }
        }
    };
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    @Inject SessionManager mSessionManager;
    private ArrayList<Item> mItems;
    private ArrayList<Item> mUpdated = new ArrayList<>();
    private ArrayList<String> mPromoted = new ArrayList<>();
    private final LongSparseArray<Integer> mItemPositions = new LongSparseArray<>();
    private final LongSparseArray<Integer> mUpdatedPositions = new LongSparseArray<>();
    private int mFavoriteRevision = 1;
    private String mUsername;
    private boolean mHighlightUpdated = true;
    private boolean mShowAll = true;
    private int mCacheMode = ItemManager.MODE_DEFAULT;
    private ItemTouchHelper mItemTouchHelper;

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        ContentResolver cr = recyclerView.getContext().getContentResolver();
        cr.registerContentObserver(MaterialisticProvider.URI_VIEWED, true, mObserver);
        cr.registerContentObserver(MaterialisticProvider.URI_FAVORITE, true, mObserver);
        mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(recyclerView.getContext()) {
            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                Item item = getItem(viewHolder.getAdapterPosition());
                int swipeDirs = 0;
                if (!item.isFavorite()) {
                    swipeDirs |= ItemTouchHelper.LEFT;
                }
                if (!item.isVoted() && !item.isPendingVoted()) {
                    swipeDirs |= ItemTouchHelper.RIGHT;
                }
                return swipeDirs;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                Item item = getItem(viewHolder.getAdapterPosition());
                if (direction == ItemTouchHelper.LEFT) {
                    toggleSave(item);
                } else {
                    notifyItemChanged(viewHolder.getAdapterPosition());
                    vote(item, viewHolder);
                }
            }
        });
        mItemTouchHelper.attachToRecyclerView(recyclerView);
        toggleAutoMarkAsViewed(recyclerView.getContext());
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        recyclerView.getContext().getContentResolver().unregisterContentObserver(mObserver);
        mItemTouchHelper.attachToRecyclerView(null);
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(mInflater.inflate(R.layout.item_story, parent, false));
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
        ArrayList<Item> savedItems = savedState.getParcelableArrayList(STATE_ITEMS);
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

    public ArrayList<Item> getItems() {
        return mItems;
    }

    public void setItems(ArrayList<Item> items) {
        setUpdated(items);
        setItemsInternal(items);
        notifyDataSetChanged();
    }

    public void setHighlightUpdated(boolean highlightUpdated) {
        mHighlightUpdated = highlightUpdated;
    }

    public void setShowAll(boolean showAll) {
        mShowAll = showAll;
    }

    public void initDisplayOptions(Context context) {
        mHighlightUpdated = Preferences.highlightUpdatedEnabled(context);
        mUsername = Preferences.getUsername(context);
        if (isAttached()) {
            toggleAutoMarkAsViewed(context);
            notifyDataSetChanged();
        }
    }

    @Override
    protected void loadItem(final int adapterPosition) {
        Item item = getItem(adapterPosition);
        if (item.getLocalRevision() == 0) {
            return;
        }
        item.setLocalRevision(0);
        mItemManager.getItem(item.getId(), getItemCacheMode(), new ItemResponseListener(this, item));
    }

    @Override
    protected void bindItem(final ItemViewHolder holder) {
        final Item story = getItem(holder.getAdapterPosition());
        bindItemUpdated(holder, story);
        highlightUserPost(holder, story);
        holder.mStoryView.setViewed(story.isViewed());
        if (story.getLocalRevision() < mFavoriteRevision) {
            story.setFavorite(false);
        }
        holder.mStoryView.setFavorite(story.isFavorite());
        holder.itemView.setOnLongClickListener(v -> {
            showMoreOptions(holder.mStoryView.getMoreOptions(), story, holder);
            return true;
        });
        holder.mStoryView.getMoreOptions().setOnClickListener(v -> showMoreOptions(v, story, holder));
    }

    @Override
    protected boolean isItemAvailable(Item item) {
        return item != null && item.getLocalRevision() > 0;
    }

    @Override
    protected Item getItem(int position) {
        if (mShowAll) {
            return mItems.get(position);
        } else {
            return mUpdated.get(position);
        }
    }

    @Override
    protected int getItemCacheMode() {
        return mCacheMode;
    }

    private void setItemsInternal(ArrayList<Item> items) {
        mItems = items;
        mItemPositions.clear();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                mItemPositions.put(items.get(i).getLongId(), i);
            }
        }
    }

    private void setUpdated(ArrayList<Item> items) {
        if (!mHighlightUpdated || getItems() == null) {
            return;
        }
        mUpdated.clear();
        mUpdatedPositions.clear();
        mPromoted.clear();
        for (Item item : items) {
            Integer position = mItemPositions.get(item.getLongId());
            if (position == null) {
                mUpdated.add(item);
                mUpdatedPositions.put(item.getLongId(), mUpdated.size() - 1);
            } else {
                Item currentRevision = mItems.get(position);
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
                    .setAction(R.string.show_me, v -> {
                        setShowAll(false);
                        notifyUpdated();
                        notifyDataSetChanged();
                    })
                    .show();
        } else {
            final Snackbar snackbar = Snackbar.make(mRecyclerView,
                    mContext.getResources().getQuantityString(R.plurals.showing_new_stories,
                            mUpdated.size(), mUpdated.size()),
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.show_all, v -> {
                snackbar.dismiss();
                mUpdated.clear();
                setShowAll(true);
                notifyDataSetChanged();
            }).show();
        }
    }

    private void onItemLoaded(Item item) {
        Integer position = mShowAll ? mItemPositions.get(item.getLongId()) :
                mUpdatedPositions.get(item.getLongId());
        // ignore changes if item was invalidated by refresh / filter
        if (position != null && position >= 0 && position < getItemCount()) {
            notifyItemChanged(position);
        }
    }

    private void bindItemUpdated(ItemViewHolder holder, Item story) {
        if (mHighlightUpdated) {
            holder.mStoryView.setUpdated(story,
                    mUpdatedPositions.indexOfKey(story.getLongId()) >= 0,
                    mPromoted.contains(story.getId()));
        }
    }

    private void showMoreOptions(View v, final Item story, final ItemViewHolder holder) {
        mPopupMenu.create(mContext, v, Gravity.NO_GRAVITY)
                .inflate(R.menu.menu_contextual_story)
                .setMenuItemTitle(R.id.menu_contextual_save,
                        story.isFavorite() ? R.string.unsave : R.string.save)
                .setOnMenuItemClickListener(item -> {
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
                    if (item.getItemId() == R.id.menu_contextual_share) {
                        mContext.startActivity(AppUtils.makeChooserShareIntent(mContext,
                                story.getDisplayedTitle(),
                                story.getUrl()));
                        return true;
                    }
                    return false;
                })
                .show();
    }

    private void toggleSave(final Item story) {
        final int toastMessageResId;
        if (!story.isFavorite()) {
            mFavoriteManager.add(mContext, story);
            toastMessageResId = R.string.toast_saved;
        } else {
            mFavoriteManager.remove(mContext, story.getId());
            toastMessageResId = R.string.toast_removed;
        }
        Snackbar.make(mRecyclerView, toastMessageResId, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, v -> toggleSave(story))
                .show();
    }

    private void vote(final Item story, final RecyclerView.ViewHolder holder) {
        if (!mUserServices.voteUp(mContext, story.getId(),
                new VoteCallback(this, holder.getAdapterPosition(), story))) {
            AppUtils.showLogin(mContext, mAlertDialogBuilder);
        }
    }

    private void onVoted(int position, Boolean successful) {
        if (successful == null) {
            Toast.makeText(mContext, R.string.vote_failed, Toast.LENGTH_SHORT).show();
        } else if (successful) {
            Toast.makeText(mContext, R.string.voted, Toast.LENGTH_SHORT).show();
            if (position < getItemCount()) {
                notifyItemChanged(position);
            }
        }
    }

    private void highlightUserPost(ItemViewHolder holder,
                                   Item story) {
        holder.mStoryView.setChecked(isSelected(story.getId()) ||
                !TextUtils.isEmpty(mUsername) &&
                TextUtils.equals(mUsername, story.getBy()));
    }

    public void setCacheMode(int cacheMode) {
        mCacheMode = cacheMode;
    }

    private void markAsViewed(int position) {
        if (position < 0) {
            return;
        }
        Item item = mItems != null && position < mItems.size() ?
                mItems.get(position) : null;
        if (item == null || !isItemAvailable(item) || item.isViewed()) {
            return;
        }
        mSessionManager.view(mContext, item.getId());
    }

    private void toggleAutoMarkAsViewed(Context context) {
        if (Preferences.autoMarkAsViewed(context)) {
            mRecyclerView.addOnScrollListener(mAutoViewScrollListener);
        } else {
            mRecyclerView.removeOnScrollListener(mAutoViewScrollListener);
        }
    }

    private static class ItemResponseListener implements ResponseListener<Item> {
        private final WeakReference<StoryRecyclerViewAdapter> mAdapter;
        private final Item mPartialItem;

        ItemResponseListener(StoryRecyclerViewAdapter adapter,
                                    Item partialItem) {
            mAdapter = new WeakReference<>(adapter);
            mPartialItem = partialItem;
        }

        @Override
        public void onResponse(@Nullable Item response) {
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
        private final Item mItem;

        VoteCallback(StoryRecyclerViewAdapter adapter, int position,
                            Item item) {
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
        public void onError(Throwable throwable) {
            if (mAdapter.get() != null && mAdapter.get().isAttached()) {
                mAdapter.get().onVoted(mPosition, null);
            }
        }
    }

    static abstract class ItemTouchHelperCallback extends ItemTouchHelper.SimpleCallback {
        private static final float SWIPE_THRESHOLD = 0.25f;
        private final Paint mPaint = new Paint();
        private final String mSaveText;
        private final String mVoteText;

        ItemTouchHelperCallback(Context context) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mPaint.setColor(ContextCompat.getColor(context,
                    AppUtils.getThemedResId(context, android.R.attr.textColorPrimary)));
            mPaint.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.text_size_small));
            mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            mSaveText = context.getString(R.string.save);
            mVoteText = context.getString(R.string.vote_up);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            dX = drawPeekingText(c, viewHolder, dX);
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        private float drawPeekingText(Canvas canvas, RecyclerView.ViewHolder viewHolder, float dX) {
            View itemView = viewHolder.itemView;
            String text = dX > 0 ? mVoteText : mSaveText;
            Rect rect = new Rect();
            mPaint.getTextBounds(text, 0, text.length(), rect);
            float textWidth = rect.right - rect.left,
                    textHeight = rect.bottom - rect.top,
                    width = itemView.getWidth() * SWIPE_THRESHOLD,
                    paddingX = (width - textWidth) / 2,
                    paddingY = (itemView.getHeight() - textHeight) / 2;
            dX = Math.max(dX, -width);
            dX = Math.min(dX, width);
            mPaint.setAlpha((int) (255 * Math.abs(dX / width)));
            canvas.drawText(text.toUpperCase(Locale.getDefault()),
                    dX > 0 ? itemView.getLeft() + paddingX :
                            itemView.getRight() - width + paddingX,
                    itemView.getBottom() - paddingY,
                    mPaint);
            return dX;
        }

        @Override
        public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
            return SWIPE_THRESHOLD;
        }
    }
}

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

package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AlertDialogBuilder;
import io.github.hidroh.materialistic.Injectable;
import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.MultiPaneListener;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.WebItem;

/**
 * Base {@link android.support.v7.widget.RecyclerView.Adapter} class for list items
 * @param <VH>  view holder type, should contain title, posted, source and comment views
 * @param <T>   item type, should provide title, posted, source
 */
public abstract class ListRecyclerViewAdapter
        <VH extends ListRecyclerViewAdapter.ItemViewHolder, T extends WebItem>
        extends RecyclerView.Adapter<VH> {

    private static final String STATE_LAST_SELECTION_POSITION = "state:lastSelectedPosition";
    private static final String STATE_CARD_VIEW_ENABLED = "state:cardViewEnabled";
    protected Context mContext;
    private MultiPaneListener mMultiPaneListener;
    protected RecyclerView mRecyclerView;
    protected LayoutInflater mInflater;
    @Inject PopupMenu mPopupMenu;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    @Inject UserServices mUserServices;
    @Inject FavoriteManager mFavoriteManager;
    private int mLastSelectedPosition = -1;
    private int mCardElevation;
    private int mCardRadius;
    private boolean mCardViewEnabled = true;

    public ListRecyclerViewAdapter() {
        setHasStableIds(true);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
        mContext = recyclerView.getContext();
        mInflater = LayoutInflater.from(mContext);
        ((Injectable) mContext).inject(this);
        mMultiPaneListener = (MultiPaneListener) mContext;
        mCardElevation = mContext.getResources()
                .getDimensionPixelSize(R.dimen.cardview_default_elevation);
        mCardRadius = mContext.getResources()
                .getDimensionPixelSize(R.dimen.cardview_default_radius);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mContext = null;
        mMultiPaneListener = null;
        mRecyclerView = null;
    }

    @Override
    public final void onBindViewHolder(final VH holder, int position) {
        final T item = getItem(position);
        if (mCardViewEnabled) {
            holder.mCardView.setCardElevation(mCardElevation);
            holder.mCardView.setRadius(mCardRadius);
            holder.mCardView.setUseCompatPadding(true);
        } else {
            holder.mCardView.setCardElevation(0);
            holder.mCardView.setRadius(0);
            holder.mCardView.setUseCompatPadding(false);
        }
        if (!isItemAvailable(item)) {
            clearViewHolder(holder);
            loadItem(holder.getAdapterPosition());
            return;
        }
        holder.mStoryView.setStory(item);
        holder.mStoryView.setChecked(isSelected(item.getId()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleItemClick(item, holder);
            }
        });
        holder.mStoryView.setOnCommentClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openItem(item);
            }
        });
        bindItem(holder);
    }

    @Override
    public final long getItemId(int position) {
        return getItem(position).getLongId();
    }

    public final boolean isCardViewEnabled() {
        return mCardViewEnabled;
    }

    public final void setCardViewEnabled(boolean cardViewEnabled) {
        this.mCardViewEnabled = cardViewEnabled;
    }

    public Bundle saveState() {
        Bundle savedState = new Bundle();
        savedState.putInt(STATE_LAST_SELECTION_POSITION, mLastSelectedPosition);
        savedState.putBoolean(STATE_CARD_VIEW_ENABLED, mCardViewEnabled);
        return savedState;
    }

    public void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }
        mCardViewEnabled = savedState.getBoolean(STATE_CARD_VIEW_ENABLED, true);
        mLastSelectedPosition = savedState.getInt(STATE_LAST_SELECTION_POSITION);
    }

    public final boolean isAttached() {
        return mContext != null;
    }

    protected void loadItem(int adapterPosition) {
        // override to load item if needed
    }

    protected abstract void bindItem(VH holder);

    protected abstract boolean isItemAvailable(T item);

    /**
     * Clears previously bind data from given view holder
     * @param holder    view holder to clear
     */
    protected final void clearViewHolder(VH holder) {
        holder.mStoryView.reset();
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
    }

    /**
     * Checks if item with given ID has been selected
     * @param itemId    item ID to check
     * @return  true if selected, false otherwise or if selection is disabled
     */
    protected boolean isSelected(String itemId) {
        return mMultiPaneListener.isMultiPane() &&
                mMultiPaneListener.getSelectedItem() != null &&
                itemId.equals(mMultiPaneListener.getSelectedItem().getId());
    }

    /**
     * Gets item at position
     * @param position    item position
     * @return item at given position or null
     */
    protected abstract T getItem(int position);

    /**
     * Handles item click
     * @param item      clicked item
     * @param holder    clicked item view holder
     */
    protected void handleItemClick(T item, VH holder) {
        mMultiPaneListener.onItemSelected(item);
        if (isSelected(item.getId())) {
            notifyItemChanged(holder.getAdapterPosition());
            if (mLastSelectedPosition >= 0) {
                notifyItemChanged(mLastSelectedPosition);
            }
            mLastSelectedPosition = holder.getAdapterPosition();
        }
    }

    private void openItem(T item) {
        mContext.startActivity(new Intent(mContext, ItemActivity.class)
                .putExtra(ItemActivity.EXTRA_ITEM, item)
                .putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true));
    }

    /**
     * Base {@link android.support.v7.widget.RecyclerView.ViewHolder} class for list item view
     */
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public final StoryView mStoryView;
        public final CardView mCardView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mCardView = (CardView) itemView;
            mStoryView = (StoryView) itemView.findViewById(R.id.story_view);
        }
    }
}

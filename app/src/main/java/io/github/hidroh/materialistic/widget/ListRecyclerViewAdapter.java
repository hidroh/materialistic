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
import android.view.View;

import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

/**
 * Base {@link android.support.v7.widget.RecyclerView.Adapter} class for list items
 * @param <VH>  view holder type, should contain title, posted, source and comment views
 * @param <T>   item type, should provide title, posted, source
 */
public abstract class ListRecyclerViewAdapter<VH extends ListRecyclerViewAdapter.ItemViewHolder, T extends ItemManager.WebItem> extends RecyclerView.Adapter<VH> {

    private static final String STATE_LAST_SELECTION_POSITION = "state:lastSelectedPosition";
    private Context mContext;
    private int mLastSelectedPosition = -1;
    private int mCardElevation;
    private int mCardRadius;

    public ListRecyclerViewAdapter() {
        setHasStableIds(true);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mContext = recyclerView.getContext();
        mCardElevation = mContext.getResources()
                .getDimensionPixelSize(R.dimen.cardview_default_elevation);
        mCardRadius = mContext.getResources()
                .getDimensionPixelSize(R.dimen.cardview_default_radius);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mContext = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public final void onBindViewHolder(final VH holder, int position) {
        final T item = getItem(position);
        if (shouldCompact()) {
            holder.mCardView.setCardElevation(0);
            holder.mCardView.setRadius(0);
        } else {
            holder.mCardView.setCardElevation(mCardElevation);
            holder.mCardView.setRadius(mCardRadius);
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
                handleCommentButtonClick(item, holder);
            }
        });
        bindItem(holder);
    }

    @Override
    public long getItemId(int position) {
        return Long.valueOf(getItem(position).getId());
    }

    public Bundle saveState() {
        Bundle savedState = new Bundle();
        savedState.putInt(STATE_LAST_SELECTION_POSITION, mLastSelectedPosition);
        return savedState;
    }

    public void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }
        mLastSelectedPosition = savedState.getInt(STATE_LAST_SELECTION_POSITION);
    }

    protected abstract boolean shouldCompact();

    protected void loadItem(int adapterPosition) {
        // override to load item if needed
    }

    protected abstract void bindItem(VH holder);

    protected abstract boolean isItemAvailable(T item);

    /**
     * Handles item click
     * @param item      clicked item
     * @param holder    clicked item view holder
     */
    protected void handleItemClick(T item, VH holder) {
        onItemSelected(item, holder.itemView);
        if (isSelected(item.getId())) {
            notifyItemChanged(holder.getAdapterPosition());
            if (mLastSelectedPosition >= 0) {
                notifyItemChanged(mLastSelectedPosition);
            }
            mLastSelectedPosition = holder.getAdapterPosition();
        }
    }

    /**
     * Handles comment button click
     * @param item      clicked item
     * @param holder    clicked item view holder
     */
    protected void handleCommentButtonClick(T item, VH holder) {
        openItem(item);
    }

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
     * Handles item selection
     * @param item  item that has been selected
     * @param itemView selected item view
     */
    protected abstract void onItemSelected(T item, View itemView);

    /**
     * Checks if item with given ID has been selected
     * @param itemId    item ID to check
     * @return  true if selected, false otherwise or if selection is disabled
     */
    protected abstract boolean isSelected(String itemId);

    /**
     * Gets item at position
     * @param position    item position
     * @return item at given position or null
     */
    protected abstract T getItem(int position);

    private void openItem(T item) {
        final Intent intent = new Intent(mContext, ItemActivity.class);
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        intent.putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true);
        mContext.startActivity(intent);
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

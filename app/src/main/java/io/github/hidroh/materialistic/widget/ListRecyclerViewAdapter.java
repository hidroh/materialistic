package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.Intent;
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

    private Context mContext;
    private int mLastSelectedPosition = -1;
    private int mCardElevation;
    private int mCardRadius;

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
     * @param itemView
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

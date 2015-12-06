package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextSwitcher;
import android.widget.TextView;

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

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mContext = recyclerView.getContext();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mContext = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    /**
     * Populates view holder with data from given item
     * @param holder    view holder to populate
     * @param item      item that contains data
     */
    protected void bindViewHolder(final VH holder, final T item) {
        holder.mTitleTextView.setCurrentText(mContext.getString(R.string.loading_text));
        holder.mTitleTextView.setText(item.getDisplayedTitle());
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext, true));
        holder.mStoryView.setChecked(isSelected(item.getId()));
        switch (item.getType()) {
            case ItemManager.Item.JOB_TYPE:
                holder.mSourceTextView.setText(null);
                holder.mSourceTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_work_grey600_18dp, 0, 0, 0);
                break;
            case ItemManager.Item.POLL_TYPE:
                holder.mSourceTextView.setText(null);
                holder.mSourceTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_poll_grey600_18dp, 0, 0, 0);
                break;
            default:
                holder.mSourceTextView.setText(item.getSource());
                holder.mSourceTextView.setCompoundDrawables(null, null, null, null);
                break;
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleItemClick(item, holder);
            }
        });
        holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCommentButtonClick(item, holder);
            }
        });
    }

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
    protected void clearViewHolder(VH holder) {
        holder.mTitleTextView.setCurrentText(mContext.getString(R.string.loading_text));
        holder.mPostedTextView.setText(R.string.loading_text);
        holder.mSourceTextView.setText(R.string.loading_text);
        holder.mSourceTextView.setCompoundDrawables(null, null, null, null);
        holder.mCommentButton.setVisibility(View.GONE);
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
    protected abstract ItemManager.WebItem getItem(int position);

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
        public final TextView mPostedTextView;
        public final TextSwitcher mTitleTextView;
        public final View mCommentButton;
        public final TextView mSourceTextView;
        public final StoryView mStoryView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mStoryView = (StoryView) itemView.findViewById(R.id.story_view);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mTitleTextView = (TextSwitcher) itemView.findViewById(R.id.title);
            mSourceTextView = (TextView) itemView.findViewById(R.id.source);
            mCommentButton = itemView.findViewById(R.id.comment);
        }
    }
}

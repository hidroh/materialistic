package io.github.hidroh.materialistic.widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import io.github.hidroh.materialistic.AppUtils;
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
    private int mCardBackgroundColorResId;
    private int mCardHighlightColorResId;

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mContext = recyclerView.getContext();
        mCardBackgroundColorResId = mContext.getResources().getColor(
                AppUtils.getThemedResId(mContext, R.attr.themedCardBackgroundColor));
        mCardHighlightColorResId = mContext.getResources().getColor(
                AppUtils.getThemedResId(mContext, R.attr.themedCardHighlightColor));
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
        holder.mTitleTextView.setText(item.getDisplayedTitle());
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext));
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
        decorateCardSelection(holder, item.getId());
    }

    /**
     * Handles item click
     * @param item      clicked item
     * @param holder    clicked item view holder
     */
    protected void handleItemClick(T item, VH holder) {
        onItemSelected(item, holder.itemView);
        if (isSelected(item.getId())) {
            notifyDataSetChanged(); // switch selection decorator
        }
    }

    /**
     * Handles comment button click
     * @param item      clicked item
     * @param holder    clicked item view holder
     */
    protected void handleCommentButtonClick(T item, VH holder) {
        openItem(item, holder.itemView);
    }

    /**
     * Clears previously bind data from given view holder
     * @param holder    view holder to clear
     */
    protected void clearViewHolder(VH holder) {
        holder.mTitleTextView.setText(R.string.loading_text);
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

    private void decorateCardSelection(ItemViewHolder holder, String itemId) {
        holder.itemView.setBackgroundColor(isSelected(itemId) ?
                        mCardHighlightColorResId : mCardBackgroundColorResId);
    }

    private void openItem(T item, View sharedElement) {
        final Intent intent = new Intent(mContext, ItemActivity.class);
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        final ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation((Activity) mContext,
                        sharedElement, mContext.getString(R.string.transition_item_container));
        ActivityCompat.startActivity((Activity) mContext, intent, options.toBundle());
    }

    /**
     * Base {@link android.support.v7.widget.RecyclerView.ViewHolder} class for list item view
     */
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public final TextView mPostedTextView;
        public final TextView mTitleTextView;
        public final View mCommentButton;
        public final TextView mSourceTextView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mTitleTextView = (TextView) itemView.findViewById(R.id.title);
            mSourceTextView = (TextView) itemView.findViewById(R.id.source);
            mCommentButton = itemView.findViewById(R.id.comment);
        }
    }
}

package io.github.hidroh.materialistic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import io.github.hidroh.materialistic.data.ItemManager;

/**
 * Base {@link android.support.v7.widget.RecyclerView.Adapter} class for list items
 * @param <VH>  view holder type, should contain title, posted, source and comment views
 * @param <T>   item type, should provide title, posted, source
 */
public abstract class ItemRecyclerViewAdapter<VH extends ItemRecyclerViewAdapter.ItemViewHolder, T extends ItemManager.WebItem> extends RecyclerView.Adapter<VH> {

    protected String mSelectedItemId;
    private Context mContext;

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
        holder.mTitleTextView.setText(item.getDisplayedTitle());
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext));
        switch (item.getType()) {
            case job:
                holder.mSourceTextView.setText(null);
                holder.mSourceTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_work_grey600_18dp, 0, 0, 0);
                break;
            case poll:
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
                openItem(item, holder);
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
        if (mContext.getResources().getBoolean(R.bool.multi_pane)) {
            if (!TextUtils.isEmpty(mSelectedItemId) && item.getId().equals(mSelectedItemId)) {
                return;
            }

            mSelectedItemId = item.getId();
            notifyDataSetChanged();
            onItemSelected(item);
        } else {
            if (PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getBoolean(mContext.getString(R.string.pref_item_click), false)) {
                openItem(item, holder);
            } else {
                AppUtils.openWebUrl(mContext, item);
            }
        }
    }

    /**
     * Clears previously bind data from given view holder
     * @param holder    view holder to clear
     */
    protected void clearViewHolder(VH holder) {
        holder.mTitleTextView.setText(mContext.getString(R.string.loading_text));
        holder.mPostedTextView.setText(mContext.getString(R.string.loading_text));
        holder.mSourceTextView.setText(mContext.getString(R.string.loading_text));
        holder.mSourceTextView.setCompoundDrawables(null, null, null, null);
        holder.mCommentButton.setVisibility(View.GONE);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
    }

    /**
     * Handles item selection
     * @param item  item that has been selected
     */
    protected abstract void onItemSelected(T item);

    /**
     * Checks if item with given ID has been selected
     * @param itemId    item ID to check
     * @return  true if selected, false otherwise or if selection is disabled
     */
    protected abstract boolean isSelected(String itemId);

    private void openItem(T item, ItemViewHolder holder) {
        final Intent intent = new Intent(mContext, ItemActivity.class);
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        final ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation((Activity) mContext,
                        holder.itemView, mContext.getString(R.string.transition_item_container));
        ActivityCompat.startActivity((Activity) mContext, intent, options.toBundle());
    }

    private void decorateCardSelection(ItemViewHolder holder, String itemId) {
        ((CardView) holder.itemView).setCardBackgroundColor(
                mContext.getResources().getColor(isSelected(itemId) ?
                                R.color.colorPrimaryLight : R.color.cardview_light_background));
    }

    /**
     * Base {@link android.support.v7.widget.RecyclerView.ViewHolder} class for list item view
     */
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView mPostedTextView;
        final TextView mTitleTextView;
        final View mCommentButton;
        final TextView mSourceTextView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mTitleTextView = (TextView) itemView.findViewById(R.id.title);
            mSourceTextView = (TextView) itemView.findViewById(R.id.source);
            mCommentButton = itemView.findViewById(R.id.comment);
        }
    }
}

package io.github.hidroh.materialistic.widget;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public class MultiPageItemRecyclerViewAdapter extends ItemRecyclerViewAdapter {
    private final ItemManager.Item[] mItems;
    private final int mItemLevel;

    public MultiPageItemRecyclerViewAdapter(ItemManager itemManager, ItemManager.Item[] items,
                                            int itemLevel) {
        super(itemManager);
        this.mItems = items;
        this.mItemLevel = itemLevel;
    }

    @Override
    protected ItemManager.Item getItem(int position) {
        return mItems[position];
    }

    @Override
    protected void bind(final ItemViewHolder holder, final ItemManager.Item item) {
        holder.mCommentButton.setVisibility(View.GONE);
        holder.mPostedTextView.setOnClickListener(null);
        if (item == null) {
            holder.mPostedTextView.setText(mContext.getString(R.string.loading_text));
            holder.mContentTextView.setText(mContext.getString(R.string.loading_text));
        } else {
            holder.mPostedTextView.setText(item.getDisplayedTime(mContext));
            AppUtils.setTextWithLinks(holder.mContentTextView, item.getText());
            if (item.getKidCount() > 0) {
                holder.mCommentText.setText(String.valueOf(item.getKidCount()));
                holder.mCommentButton.setVisibility(View.VISIBLE);
                holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openItem(holder, item);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return mItems.length;
    }

    private void openItem(ItemViewHolder holder, ItemManager.Item item) {
        final Intent intent = new Intent(mContext, ItemActivity.class);
        Activity activity = (Activity) mContext;
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        intent.putExtra(ItemActivity.EXTRA_ITEM_LEVEL, mItemLevel + 1);
        final ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity,
                        holder.itemView, mContext.getString(R.string.transition_item_container));
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }
}

package io.github.hidroh.materialistic.widget;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public class UserItemRecyclerViewAdapter extends ItemRecyclerViewAdapter<ItemRecyclerViewAdapter.ItemViewHolder> {

    private final ItemManager.Item[] mItems;

    public UserItemRecyclerViewAdapter(ItemManager itemManager, @NonNull ItemManager.Item[] items) {
        super(itemManager);
        mItems = items;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(mLayoutInflater.inflate(R.layout.item_comment, parent, false));
    }

    @Override
    public int getItemCount() {
        return mItems.length;
    }

    @Override
    protected ItemManager.Item getItem(int position) {
        return mItems[position];
    }

    @Override
    protected void bind(final ItemViewHolder holder, final ItemManager.Item item) {
        super.bind(holder, item);
        if (item == null) {
            return;
        }
        // TODO display story title/link to comment's parent
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext, false));
        holder.mCommentButton.setText(R.string.open);
        holder.mCommentButton.setVisibility(View.VISIBLE);
        holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openItem(item);
            }
        });
    }

    private void openItem(ItemManager.Item item) {
        final Intent intent = new Intent(mContext, ItemActivity.class);
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        mContext.startActivity(intent);
    }
}

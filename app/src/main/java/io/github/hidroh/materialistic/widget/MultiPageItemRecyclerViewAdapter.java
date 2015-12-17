package io.github.hidroh.materialistic.widget;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public class MultiPageItemRecyclerViewAdapter
        extends ItemRecyclerViewAdapter<ItemRecyclerViewAdapter.ItemViewHolder> {
    private final ItemManager.Item[] mItems;
    private final int mItemLevel;

    public MultiPageItemRecyclerViewAdapter(ItemManager itemManager,
                                            ItemManager.Item[] items,
                                            int itemLevel) {
        super(itemManager);
        this.mItems = items;
        this.mItemLevel = itemLevel;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(mLayoutInflater.inflate(R.layout.item_comment, parent, false));
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
        if (item.getKidCount() > 0) {
            holder.mCommentButton.setText(mContext.getString(R.string.comments_count, item.getKidCount()));
            holder.mCommentButton.setVisibility(View.VISIBLE);
            holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openItem(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mItems.length;
    }

    private void openItem(ItemManager.Item item) {
        final Intent intent = new Intent(mContext, ItemActivity.class);
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        intent.putExtra(ItemActivity.EXTRA_ITEM_LEVEL, mItemLevel + 1);
        intent.putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true);
        mContext.startActivity(intent);
    }
}

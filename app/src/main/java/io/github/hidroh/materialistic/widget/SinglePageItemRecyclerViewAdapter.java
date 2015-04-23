package io.github.hidroh.materialistic.widget;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public class SinglePageItemRecyclerViewAdapter extends ItemRecyclerViewAdapter {
    private final ArrayList<ItemManager.Item> mList;
    private Map<String, ItemManager.Item> mLoaded = new HashMap<>();

    public SinglePageItemRecyclerViewAdapter(ItemManager itemManager, ArrayList<ItemManager.Item> list) {
        super(itemManager);
        this.mList = list;
    }

    @Override
    protected ItemManager.Item getItem(int position) {
        return mList.get(position);
    }

    @Override
    protected void bind(final ItemViewHolder holder, final ItemManager.Item item) {
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                holder.itemView.getLayoutParams();
        params.leftMargin = AppUtils.getDimensionInDp(mContext,
                R.dimen.level_indicator_width) * (item == null ? 0 : item.getLevel() - 1);
        params.bottomMargin = AppUtils.getDimensionInDp(mContext, R.dimen.margin);
        holder.itemView.setLayoutParams(params);
        // higher level item gets higher elevation, max 10dp
        ViewCompat.setElevation(holder.itemView,
                10f - 1f * (item == null ? 0 : item.getLevel() - 1));
        holder.mCommentButton.setVisibility(View.GONE);
        holder.mPostedTextView.setOnClickListener(null);
        if (item == null) {
            holder.mPostedTextView.setText(mContext.getString(R.string.loading_text));
            holder.mContentTextView.setText(mContext.getString(R.string.loading_text));
            return;
        }

        holder.mPostedTextView.setText(item.getDisplayedTime(mContext));
        AppUtils.setTextWithLinks(holder.mContentTextView, item.getText());
        if (item.getKidCount() == 0) {
            return;
        }

        params.bottomMargin = 0;
        holder.itemView.setLayoutParams(params);
        if (!mLoaded.containsKey(item.getId())) {
            int index = mList.indexOf(item) + 1;
            mLoaded.put(item.getId(), item);
            // recursive here!!!
            mList.addAll(index, Arrays.asList(item.getKidItems()));
            notifyItemRangeInserted(index, item.getKidCount());
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }
}

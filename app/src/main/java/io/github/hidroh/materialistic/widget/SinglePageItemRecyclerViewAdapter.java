package io.github.hidroh.materialistic.widget;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public class SinglePageItemRecyclerViewAdapter
        extends ItemRecyclerViewAdapter<ToggleItemViewHolder> {
    private final ArrayList<ItemManager.Item> mList;
    private Map<String, ItemManager.Item> mExpanded = new HashMap<>();
    private Set<String> mCollapsed = new HashSet<>();
    private int mLevelIndicatorWidth = 0;
    private int mDefaultItemVerticalMargin = 0;

    public SinglePageItemRecyclerViewAdapter(ItemManager itemManager, ArrayList<ItemManager.Item> list) {
        super(itemManager);
        this.mList = list;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mLevelIndicatorWidth = AppUtils.getDimensionInDp(mContext, R.dimen.level_indicator_width);
        mDefaultItemVerticalMargin = AppUtils.getDimensionInDp(mContext, R.dimen.margin);
    }

    @Override
    public ToggleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ToggleItemViewHolder holder =
                new ToggleItemViewHolder(mLayoutInflater.inflate(R.layout.item_comment, parent, false));
        // higher level item gets higher elevation, max 10dp
        ViewCompat.setElevation(holder.itemView, 10f - 1f * viewType);
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                holder.itemView.getLayoutParams();
        params.leftMargin = mLevelIndicatorWidth * viewType;
        holder.itemView.setLayoutParams(params);
        return holder;
    }

    @Override
    protected ItemManager.Item getItem(int position) {
        return mList.get(position);
    }

    @Override
    protected void bind(final ToggleItemViewHolder holder, final ItemManager.Item item) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                holder.itemView.getLayoutParams();
        params.bottomMargin = mDefaultItemVerticalMargin;
        holder.itemView.setLayoutParams(params);
        holder.mCommentButton.setVisibility(View.GONE);
        holder.mPostedTextView.setOnClickListener(null);
        holder.mToggle.setVisibility(View.GONE);
        if (item == null) {
            holder.mPostedTextView.setText(R.string.loading_text);
            holder.mContentTextView.setText(R.string.loading_text);
            return;
        }

        holder.mPostedTextView.setText(item.getDisplayedTime(mContext));
        AppUtils.setTextWithLinks(holder.mContentTextView, item.getText());
        if (item.getKidCount() == 0) {
            return;
        }

        holder.mToggle.setVisibility(View.VISIBLE);
        if (!mCollapsed.contains(item.getId())) {
            expand(item);
        }
        if(mExpanded.containsKey(item.getId())) {
            params.bottomMargin = 0;
            holder.itemView.setLayoutParams(params);
            holder.mToggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    R.drawable.ic_expand_less_grey600_24dp, 0);
        } else {
            params.bottomMargin = mDefaultItemVerticalMargin;
            holder.itemView.setLayoutParams(params);
            holder.mToggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    R.drawable.ic_expand_more_grey600_24dp, 0);
        }
        holder.mToggle.setText(mContext.getString(R.string.title_activity_item_count,
                item.getKidCount()));
        holder.mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCollapsed.contains(item.getId())) {
                    mCollapsed.remove(item.getId());
                } else {
                    mCollapsed.add(item.getId());
                }

                if (mExpanded.containsKey(item.getId())) {
                    collapse(item);
                } else {
                    expand(item);
                }

                notifyItemChanged(mList.indexOf(item)); // TODO prevent exception
            }
        });
    }

    private void expand(final ItemManager.Item item) {
        if (mExpanded.containsKey(item.getId())) {
            return;
        }

        final int index = mList.indexOf(item) + 1;
        mExpanded.put(item.getId(), item);
        // recursive here!!!
        mList.addAll(index, Arrays.asList(item.getKidItems()));
        try {
            notifyItemRangeInserted(index, item.getKidCount());
        } catch (IllegalStateException e) {
            // TODO Cannot call this method while RecyclerView is computing a layout or scrolling
        }
    }

    private void collapse(ItemManager.Item item) {
        final int index = mList.indexOf(item) + 1;
        final int count = recursiveRemove(item);
        notifyItemRangeRemoved(index, count);
    }

    private int recursiveRemove(ItemManager.Item item) {
        if (!mExpanded.containsKey(item.getId())) {
            return 0;
        }

        // if item is already expanded, its kids must be added, so we need to remove them
        int count = item.getKidCount();
        mExpanded.remove(item.getId());
        for (ItemManager.Item kid : item.getKidItems()) {
            count += recursiveRemove(kid);
            mList.remove(kid);
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getLevel() - 1;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

}

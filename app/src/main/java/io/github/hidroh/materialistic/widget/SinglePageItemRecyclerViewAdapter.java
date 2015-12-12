package io.github.hidroh.materialistic.widget;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public class SinglePageItemRecyclerViewAdapter
        extends ItemRecyclerViewAdapter<ToggleItemViewHolder> {
    private int mLevelIndicatorWidth = 0;
    private final boolean mAutoExpand;
    private boolean mColorCoded = true;
    private TypedArray mColors;
    private RecyclerView mRecyclerView;
    private final @NonNull SavedState mState;

    public SinglePageItemRecyclerViewAdapter(ItemManager itemManager,
                                             @NonNull SavedState state,
                                             boolean autoExpand) {
        super(itemManager);
        this.mState = state;
        mAutoExpand = autoExpand;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mLevelIndicatorWidth = AppUtils.getDimensionInDp(mContext, R.dimen.level_indicator_width);
        mColors = mContext.getResources().obtainTypedArray(R.array.color_codes);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public ToggleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final ToggleItemViewHolder holder =
                new ToggleItemViewHolder(mLayoutInflater.inflate(R.layout.item_comment, parent, false));
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                holder.itemView.getLayoutParams();
        params.leftMargin = mLevelIndicatorWidth * viewType;
        holder.itemView.setLayoutParams(params);
        return holder;
    }

    @Override
    public void onBindViewHolder(ToggleItemViewHolder holder, int position) {
        if (mColorCoded && mColors != null && mColors.length() > 0) {
            holder.mLevel.setVisibility(View.VISIBLE);
            holder.mLevel.setBackgroundColor(mColors.getColor(
                    getItemViewType(position) % mColors.length(), 0));
        } else {
            holder.mLevel.setVisibility(View.GONE);
        }
        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getLevel() - 1;
    }

    @Override
    public int getItemCount() {
        return mState.list.size();
    }

    public void toggleColorCode(boolean enabled) {
        mColorCoded = enabled;
        notifyDataSetChanged();
    }

    @Override
    protected ItemManager.Item getItem(int position) {
        return mState.list.get(position);
    }

    @Override
    protected void clear(ToggleItemViewHolder holder) {
        super.clear(holder);
        holder.mToggle.setVisibility(View.GONE);
    }

    @Override
    protected void bind(final ToggleItemViewHolder holder, final ItemManager.Item item) {
        super.bind(holder, item);
        if (item == null) {
            return;
        }
        bindNavigation(holder, item);
        toggleKids(holder, item);
    }

    private void bindNavigation(ToggleItemViewHolder holder, ItemManager.Item item) {
        if (item.isDeleted() || !mState.expanded.containsKey(item.getParent())) {
            holder.mPostedTextView.setText(item.getDisplayedTime(mContext, false));
            holder.mPostedTextView.setOnClickListener(null);
        } else {
            final ItemManager.Item parent = mState.expanded.getParcelable(item.getParent());
            AppUtils.setHtmlText(holder.mPostedTextView, mContext.getString(R.string.posted_reply,
                    item.getDisplayedTime(mContext, false),
                    parent.getBy()));
            holder.mPostedTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mRecyclerView.smoothScrollToPosition(mState.list.indexOf(parent));
                }
            });
        }
    }

    private void toggleKids(ToggleItemViewHolder holder, final ItemManager.Item item) {
        if (item.getKidCount() == 0) {
            return;
        }
        holder.mToggle.setVisibility(View.VISIBLE);
        if (!item.isCollapsed() && mAutoExpand) {
            expand(item);
        }
        if(mState.expanded.containsKey(item.getId())) {
            holder.mToggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    R.drawable.ic_expand_less_grey600_24dp, 0);
            holder.mToggle.setText(mContext.getString(R.string.hide_comments, item.getKidCount()));
        } else {
            holder.mToggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    R.drawable.ic_expand_more_grey600_24dp, 0);
            holder.mToggle.setText(mContext.getString(R.string.show_comments, item.getKidCount()));
        }
        holder.mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.setCollapsed(!item.isCollapsed());
                if (mState.expanded.containsKey(item.getId())) {
                    collapse(item);
                } else {
                    expand(item);
                }

                notifyItemChanged(mState.list.indexOf(item)); // TODO prevent exception
            }
        });
    }

    private void expand(final ItemManager.Item item) {
        if (mState.expanded.containsKey(item.getId())) {
            return;
        }

        final int index = mState.list.indexOf(item) + 1;
        mState.expanded.putParcelable(item.getId(), item);
        // recursive here!!!
        mState.list.addAll(index, Arrays.asList(item.getKidItems()));
        try {
            notifyItemRangeInserted(index, item.getKidCount());
        } catch (IllegalStateException e) {
            // TODO Cannot call this method while RecyclerView is computing a layout or scrolling
        }
    }

    private void collapse(ItemManager.Item item) {
        final int index = mState.list.indexOf(item) + 1;
        final int count = recursiveRemove(item);
        notifyItemRangeRemoved(index, count);
    }

    private int recursiveRemove(ItemManager.Item item) {
        if (!mState.expanded.containsKey(item.getId())) {
            return 0;
        }

        // if item is already expanded, its kids must be added, so we need to remove them
        int count = item.getKidCount();
        mState.expanded.remove(item.getId());
        for (ItemManager.Item kid : item.getKidItems()) {
            count += recursiveRemove(kid);
            mState.list.remove(kid);
        }
        return count;
    }

    public static class SavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private ArrayList<ItemManager.Item> list;
        private Bundle expanded;

        public SavedState(ArrayList<ItemManager.Item> list) {
            this.list = list;
            expanded = new Bundle();
        }

        @SuppressWarnings("unchecked")
        private SavedState(Parcel source) {
            list = source.readArrayList(ItemManager.Item.class.getClassLoader());
            expanded = source.readBundle(list.isEmpty() ? null :
                    list.get(0).getClass().getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeList(list);
            dest.writeBundle(expanded);
        }
    }
}

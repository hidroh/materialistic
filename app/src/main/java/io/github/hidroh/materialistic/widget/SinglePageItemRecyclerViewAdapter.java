/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;

public class SinglePageItemRecyclerViewAdapter
        extends ItemRecyclerViewAdapter<ToggleItemViewHolder> {
    private int mLevelIndicatorWidth = 0;
    private final boolean mAutoExpand;
    private boolean mColorCoded = true;
    private TypedArray mColors;
    private RecyclerView mRecyclerView;
    private final @NonNull SavedState mState;
    private ItemTouchHelper mItemTouchHelper;

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
        mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (getItem(viewHolder.getAdapterPosition()).getKidCount() == 0) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                notifyItemChanged(position);
                toggleKids(getItem(position));
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                float swipeWidth = viewHolder.itemView.getWidth() * getSwipeThreshold(viewHolder);
                dX = Math.max(dX, -swipeWidth);
                dX = Math.min(dX, swipeWidth);
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
                return 0.1f;
            }
        });
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mRecyclerView = null;
        mItemTouchHelper.attachToRecyclerView(null);
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

    @Override
    public void initDisplayOptions(Context context) {
        mColorCoded = Preferences.colorCodeEnabled(context);
        super.initDisplayOptions(context);
    }

    @Override
    protected Item getItem(int position) {
        return mState.list.get(position);
    }

    @Override
    protected void onItemLoaded(int position, Item item) {
        // item position may already be shifted due to expansion, need to get new position
        int index = mState.list.indexOf(item);
        if (index >= 0 && index < getItemCount()) {
            notifyItemChanged(index);
        }
    }

    @Override
    protected void clear(ToggleItemViewHolder holder) {
        super.clear(holder);
        holder.mToggle.setVisibility(View.GONE);
    }

    @Override
    protected void bind(ToggleItemViewHolder holder, Item item) {
        super.bind(holder, item);
        if (item == null) {
            return;
        }
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext, false, true));
        bindNavigation(holder, item);
        bindKidsToggle(holder, item);
    }

    private void bindNavigation(ToggleItemViewHolder holder, final Item item) {
        if (!mState.expanded.containsKey(item.getParent())) {
            holder.mParent.setVisibility(View.INVISIBLE);
            return;
        }
        holder.mParent.setVisibility(View.VISIBLE);
        holder.mParent.setOnClickListener(v -> {
            Item parent = mState.expanded.getParcelable(item.getParent());
            int position = mState.list.indexOf(parent);
            mRecyclerView.smoothScrollToPosition(position);
        });
    }

    private void bindKidsToggle(final ToggleItemViewHolder holder, final Item item) {
        holder.mToggle.setVisibility(item.getKidCount() > 0 ? View.VISIBLE : View.GONE);
        if (item.getKidCount() == 0) {
            return;
        }
        if (!item.isCollapsed() && mAutoExpand) {
            expand(item);
        }
        bindToggle(holder, item, isExpanded(item));
        holder.mToggle.setOnClickListener(v -> {
            bindToggle(holder, item, !isExpanded(item));
            toggleKids(item);
        });
    }

    private void toggleKids(Item item) {
        boolean expanded = isExpanded(item);
        item.setCollapsed(!item.isCollapsed());
        if (expanded) {
            collapse(item);
        } else {
            expand(item);
        }
    }

    private void bindToggle(ToggleItemViewHolder holder, Item item, boolean expanded) {
        if(expanded) {
            holder.mToggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    R.drawable.ic_expand_less_white_24dp, 0);
            holder.mToggle.setText(mContext.getResources()
                    .getQuantityString(R.plurals.hide_comments, item.getKidCount(), item.getKidCount()));
        } else {
            holder.mToggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    R.drawable.ic_expand_more_white_24dp, 0);
            holder.mToggle.setText(mContext.getResources()
                    .getQuantityString(R.plurals.show_comments, item.getKidCount(), item.getKidCount()));
        }
    }

    private void expand(final Item item) {
        if (isExpanded(item)) {
            return;
        }
        mState.expanded.putParcelable(item.getId(), item);
        mRecyclerView.post(() -> {
            int index = mState.list.indexOf(item) + 1;
            mState.list.addAll(index, Arrays.asList(item.getKidItems())); // recursive
            notifyItemRangeInserted(index, item.getKidCount());
        });
    }

    private void collapse(final Item item) {
        int index = mState.list.indexOf(item) + 1;
        int count = recursiveRemove(item);
        notifyItemRangeRemoved(index, count);
    }

    private int recursiveRemove(Item item) {
        if (!isExpanded(item)) {
            return 0;
        }
        // if item is already expanded, its kids must be added, so we need to remove them
        int count = item.getKidCount();
        mState.expanded.remove(item.getId());
        for (Item kid : item.getKidItems()) {
            count += recursiveRemove(kid);
            mState.list.remove(kid);
        }
        return count;
    }

    private boolean isExpanded(Item item) {
        return mState.expanded.containsKey(item.getId());
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

        private ArrayList<Item> list;
        private Bundle expanded;

        public SavedState(ArrayList<Item> list) {
            this.list = list;
            expanded = new Bundle();
        }

        @SuppressWarnings("unchecked")
        private SavedState(Parcel source) {
            list = source.readArrayList(Item.class.getClassLoader());
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

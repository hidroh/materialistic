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

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;

public class MultiPageItemRecyclerViewAdapter
        extends ItemRecyclerViewAdapter<ItemRecyclerViewAdapter.ItemViewHolder> {
    private static final int VIEW_TYPE_FOOTER = -1;
    private final Item[] mItems;

    public MultiPageItemRecyclerViewAdapter(ItemManager itemManager, Item[] items) {
        super(itemManager);
        mItems = Arrays.copyOf(items, items.length + 1);
        mItems[items.length] = null; // footer
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            return new ItemViewHolder(mLayoutInflater.inflate(R.layout.item_footer, parent, false), null);
        }
        return new ItemViewHolder(mLayoutInflater.inflate(R.layout.item_comment, parent, false));
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        if (!holder.isFooter()) {
            super.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position) == null) {
            return VIEW_TYPE_FOOTER;
        }
        return super.getItemViewType(position);
    }

    @Override
    protected Item getItem(int position) {
        return mItems[position];
    }

    @Override
    protected void bind(final ItemViewHolder holder, final Item item) {
        super.bind(holder, item);
        if (item == null) {
            return;
        }
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext));
        holder.mPostedTextView.append(item.getDisplayedAuthor(mContext, true, 0));
        if (item.getKidCount() > 0) {
            holder.mCommentButton.setText(mContext.getResources()
                    .getQuantityString(R.plurals.comments_count, item.getKidCount(), item.getKidCount()));
            holder.mCommentButton.setVisibility(View.VISIBLE);
            holder.mCommentButton.setOnClickListener(v -> openItem(item));
        }
    }

    @Override
    public int getItemCount() {
        return mItems.length;
    }

    private void openItem(Item item) {
        mContext.startActivity(new Intent(mContext, ItemActivity.class)
                .putExtra(ItemActivity.EXTRA_ITEM, item)
                .putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true));
    }
}

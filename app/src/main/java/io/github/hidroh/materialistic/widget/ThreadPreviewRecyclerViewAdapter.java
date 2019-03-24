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
import android.os.Handler;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;

public class ThreadPreviewRecyclerViewAdapter extends ItemRecyclerViewAdapter<SubmissionViewHolder> {
    private final List<Item> mItems = new ArrayList<>();
    private final List<String> mExpanded = new ArrayList<>();
    private int mLevelIndicatorWidth;
    private final String mUsername;

    public ThreadPreviewRecyclerViewAdapter(ItemManager itemManager, Item item) {
        super(itemManager);
        mItems.add(item);
        mUsername = item.getBy();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        attach(recyclerView.getContext(), recyclerView);
        mLevelIndicatorWidth = AppUtils.getDimensionInDp(mContext, R.dimen.level_indicator_width);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        detach(recyclerView.getContext(), recyclerView);
    }

    @Override
    public SubmissionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SubmissionViewHolder holder = new SubmissionViewHolder(mLayoutInflater
                .inflate(R.layout.item_submission, parent, false));
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                holder.itemView.getLayoutParams();
        params.leftMargin = mLevelIndicatorWidth * viewType;
        holder.itemView.setLayoutParams(params);
        holder.mCommentButton.setVisibility(View.GONE);
        return holder;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    protected void bind(SubmissionViewHolder holder, final Item item) {
        super.bind(holder, item);
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext));
        holder.mPostedTextView.append(item.getDisplayedAuthor(mContext,
                !TextUtils.equals(item.getBy(), mUsername), 0));
        holder.mMoreButton.setVisibility(View.GONE);
        if (TextUtils.equals(item.getType(), Item.COMMENT_TYPE)) {
            holder.mTitleTextView.setText(null);
            holder.itemView.setOnClickListener(null);
            holder.mCommentButton.setVisibility(View.GONE);
        } else {
            holder.mTitleTextView.setText(item.getDisplayedTitle());
            holder.mCommentButton.setVisibility(View.VISIBLE);
            holder.mCommentButton.setOnClickListener(v -> openItem(item));
        }
        holder.mTitleTextView.setVisibility(holder.mTitleTextView.length() > 0 ?
                View.VISIBLE : View.GONE);
        holder.mContentTextView.setVisibility(holder.mContentTextView.length() > 0 ?
                View.VISIBLE : View.GONE);
        if (!mExpanded.contains(item.getId()) && item.getParentItem() != null) {
            mExpanded.add(item.getId());
            new Handler().post(() -> {
                mItems.add(0, item.getParentItem()); // recursive
                notifyItemInserted(0);
                notifyItemRangeChanged(1, mItems.size());
            });
        }
    }

    @Override
    protected Item getItem(int position) {
        return mItems.get(position);
    }

    private void openItem(Item item) {
        mContext.startActivity(new Intent(mContext, ItemActivity.class)
                .putExtra(ItemActivity.EXTRA_ITEM, item));
    }
}

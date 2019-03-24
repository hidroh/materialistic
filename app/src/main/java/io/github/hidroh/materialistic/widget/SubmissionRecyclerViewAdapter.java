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
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.ThreadPreviewActivity;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;

public class SubmissionRecyclerViewAdapter extends ItemRecyclerViewAdapter<SubmissionViewHolder> {
    private final Item[] mItems;

    public SubmissionRecyclerViewAdapter(ItemManager itemManager, @NonNull Item[] items) {
        super(itemManager);
        mItems = items;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        attach(recyclerView.getContext(), recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        detach(recyclerView.getContext(), recyclerView);
    }

    @Override
    public SubmissionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SubmissionViewHolder(mLayoutInflater.inflate(R.layout.item_submission, parent, false));
    }

    @Override
    public int getItemCount() {
        return mItems.length;
    }

    @Override
    protected Item getItem(int position) {
        return mItems[position];
    }

    @Override
    protected void bind(final SubmissionViewHolder holder, final Item item) {
        super.bind(holder, item);
        if (item == null) {
            return;
        }
        final boolean isComment = TextUtils.equals(item.getType(), Item.COMMENT_TYPE);
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext));
        holder.mPostedTextView.append(item.getDisplayedAuthor(mContext, false, 0));
        if (isComment) {
            holder.mTitleTextView.setText(null);
            holder.mCommentButton.setText(R.string.view_thread);
        } else {
            holder.mPostedTextView.append(" - ");
            holder.mPostedTextView.append(mContext.getResources()
                    .getQuantityString(R.plurals.score, item.getScore(), item.getScore()));
            holder.mTitleTextView.setText(item.getDisplayedTitle());
            holder.mCommentButton.setText(R.string.view_story);
        }
        holder.mTitleTextView.setVisibility(holder.mTitleTextView.length() > 0 ?
                View.VISIBLE : View.GONE);
        holder.mContentTextView.setVisibility(holder.mContentTextView.length() > 0 ?
                View.VISIBLE : View.GONE);
        holder.mCommentButton.setVisibility(item.isDeleted() ? View.GONE : View.VISIBLE);
        holder.mCommentButton.setOnClickListener(v -> {
            if (isComment) {
                openPreview(item);
            } else {
                openItem(item);
            }
        });
    }

    private void openItem(Item item) {
        mContext.startActivity(new Intent(mContext, ItemActivity.class)
                .putExtra(ItemActivity.EXTRA_ITEM, item));
    }

    private void openPreview(Item item) {
        mContext.startActivity(new Intent(mContext, ThreadPreviewActivity.class)
                .putExtra(ThreadPreviewActivity.EXTRA_ITEM, item));
    }
}

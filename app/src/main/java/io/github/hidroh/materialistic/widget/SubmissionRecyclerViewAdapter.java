package io.github.hidroh.materialistic.widget;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public class SubmissionRecyclerViewAdapter extends ItemRecyclerViewAdapter<SubmissionViewHolder> {
    private final ItemManager.Item[] mItems;

    public SubmissionRecyclerViewAdapter(ItemManager itemManager, @NonNull ItemManager.Item[] items) {
        super(itemManager);
        mItems = items;
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
    protected ItemManager.Item getItem(int position) {
        return mItems[position];
    }

    @Override
    protected void bind(final SubmissionViewHolder holder, final ItemManager.Item item) {
        super.bind(holder, item);
        if (item == null) {
            return;
        }
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext, false, false));
        if (TextUtils.equals(item.getType(), ItemManager.Item.COMMENT_TYPE)) {
            AppUtils.setTextWithLinks(holder.mParentTextView,
                    mContext.getString(R.string.parent_link, item.getParent()));
            holder.mTitleTextView.setText(null);
        } else {
            holder.mParentTextView.setText(" - " + mContext.getString(R.string.score, item.getScore()));
            holder.mTitleTextView.setText(item.getDisplayedTitle());
        }
        holder.mTitleTextView.setVisibility(holder.mTitleTextView.length() > 0 ?
                View.VISIBLE : View.GONE);
        holder.mContentTextView.setVisibility(holder.mContentTextView.length() > 0 ?
                View.VISIBLE : View.GONE);
        holder.mCommentButton.setVisibility(item.isDeleted() ? View.GONE : View.VISIBLE);
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

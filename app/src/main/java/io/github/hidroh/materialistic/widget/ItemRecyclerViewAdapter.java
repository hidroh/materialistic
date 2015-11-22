package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public abstract class ItemRecyclerViewAdapter<VH extends ItemRecyclerViewAdapter.ItemViewHolder>
        extends RecyclerView.Adapter<VH> {
    private int mLocalRevision = 0;
    protected LayoutInflater mLayoutInflater;
    private ItemManager mItemManager;
    protected Context mContext;
    private int mTertiaryTextColorResId;
    private int mSecondaryTextColorResId;

    public ItemRecyclerViewAdapter(ItemManager itemManager) {
        mItemManager = itemManager;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mContext = recyclerView.getContext();
        mLayoutInflater = LayoutInflater.from(mContext);
        TypedArray ta = mContext.obtainStyledAttributes(new int[]{
                android.R.attr.textColorTertiary,
                android.R.attr.textColorSecondary,
        });
        mTertiaryTextColorResId = ta.getInt(0, 0);
        mSecondaryTextColorResId = ta.getInt(1, 0);
        ta.recycle();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mContext = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public void onBindViewHolder(final VH holder, int position) {
        final ItemManager.Item item = getItem(position);
        if (item.getLocalRevision() < mLocalRevision) {
            bind(holder, null);
            mItemManager.getItem(item.getId(),
                    new ItemManager.ResponseListener<ItemManager.Item>() {
                        @Override
                        public void onResponse(ItemManager.Item response) {
                            if (response == null) {
                                return;
                            }

                            if (mContext == null) {
                                return;
                            }

                            item.populate(response);
                            item.setLocalRevision(mLocalRevision);
                            bind(holder, item);
                            decorateDead(holder, item);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // do nothing
                        }
                    });
        } else {
            bind(holder, item);
            decorateDead(holder, item);
        }
    }

    protected abstract ItemManager.Item getItem(int position);

    protected abstract void bind(VH holder, ItemManager.Item item);

    private void decorateDead(VH holder, ItemManager.Item item) {
        holder.mContentTextView.setTextColor(item.isDead() ?
                mSecondaryTextColorResId : mTertiaryTextColorResId);
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView mPostedTextView;
        final TextView mContentTextView;
        final Button mCommentButton;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mContentTextView = (TextView) itemView.findViewById(R.id.text);
            mCommentButton = (Button) itemView.findViewById(R.id.comment);
            mCommentButton.setVisibility(View.INVISIBLE);
        }
    }
}
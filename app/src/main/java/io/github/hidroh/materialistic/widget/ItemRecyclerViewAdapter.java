package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public abstract class ItemRecyclerViewAdapter
        extends RecyclerView.Adapter<ItemRecyclerViewAdapter.ItemViewHolder> {
    private int mLocalRevision = 0;
    private LayoutInflater mLayoutInflater;
    private ItemManager mItemManager;
    protected Context mContext;

    public ItemRecyclerViewAdapter(ItemManager itemManager) {
        mItemManager = itemManager;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mContext = recyclerView.getContext();
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mContext = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(mLayoutInflater.inflate(R.layout.item_comment, parent, false));
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
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
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // do nothing
                        }
                    });
        } else {
            bind(holder, item);
        }
    }

    protected abstract ItemManager.Item getItem(int position);

    protected abstract void bind(ItemViewHolder holder, ItemManager.Item item);

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView mPostedTextView;
        final TextView mContentTextView;
        final View mCommentButton;
        final TextView mCommentText;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mContentTextView = (TextView) itemView.findViewById(R.id.text);
            mCommentButton = itemView.findViewById(R.id.comment);
            mCommentText = (TextView) mCommentButton.findViewById(R.id.text);
            mCommentButton.setVisibility(View.INVISIBLE);
        }
    }
}
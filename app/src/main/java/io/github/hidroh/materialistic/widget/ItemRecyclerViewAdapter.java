package io.github.hidroh.materialistic.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public abstract class ItemRecyclerViewAdapter<VH extends ItemRecyclerViewAdapter.ItemViewHolder>
        extends RecyclerView.Adapter<VH> {
    private static final String PROPERTY_MAX_LINES = "maxLines";
    private static final int DURATION_PER_LINE_MILLIS = 20;
    private int mLocalRevision = 0;
    protected LayoutInflater mLayoutInflater;
    private ItemManager mItemManager;
    protected Context mContext;
    private int mTertiaryTextColorResId;
    private int mSecondaryTextColorResId;
    private int mContentMaxLines;

    public ItemRecyclerViewAdapter(ItemManager itemManager) {
        mItemManager = itemManager;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mContext = recyclerView.getContext();
        mLayoutInflater = LayoutInflater.from(mContext);
        mContentMaxLines = Preferences.getCommentMaxLines(mContext);
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

    protected void bindContent(final VH holder, final @NonNull ItemManager.Item item) {
        AppUtils.setTextWithLinks(holder.mContentTextView, item.getText());
        final int lineCount = holder.mContentTextView.getLineCount(); // TODO 0 on restore state
        if (item.isContentExpanded() || lineCount <= mContentMaxLines) {
            holder.mContentTextView.setMaxLines(Integer.MAX_VALUE);
            setTextIsSelectable(holder.mContentTextView, true);
            holder.mReadMoreTextView.setVisibility(View.GONE);
            return;
        }
        holder.mContentTextView.setMaxLines(mContentMaxLines);
        setTextIsSelectable(holder.mContentTextView, false);
        holder.mReadMoreTextView.setVisibility(View.VISIBLE);
        holder.mReadMoreTextView.setText(mContext.getString(R.string.read_more, lineCount));
        holder.mReadMoreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                item.setContentExpanded(true);
                v.setVisibility(View.GONE);
                setTextIsSelectable(holder.mContentTextView, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    ObjectAnimator.ofInt(holder.mContentTextView, PROPERTY_MAX_LINES, lineCount)
                            .setDuration((lineCount - mContentMaxLines) * DURATION_PER_LINE_MILLIS)
                            .start();
                } else {
                    holder.mContentTextView.setMaxLines(Integer.MAX_VALUE);
                }
            }
        });
    }

    private void decorateDead(VH holder, ItemManager.Item item) {
        holder.mContentTextView.setTextColor(item.isDead() ?
                mSecondaryTextColorResId : mTertiaryTextColorResId);
    }

    private void setTextIsSelectable(TextView textView, boolean isSelectable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            textView.setTextIsSelectable(isSelectable);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView mPostedTextView;
        final TextView mContentTextView;
        final TextView mReadMoreTextView;
        final Button mCommentButton;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mContentTextView = (TextView) itemView.findViewById(R.id.text);
            mReadMoreTextView = (TextView) itemView.findViewById(R.id.more);
            mCommentButton = (Button) itemView.findViewById(R.id.comment);
            mCommentButton.setVisibility(View.INVISIBLE);
        }
    }
}
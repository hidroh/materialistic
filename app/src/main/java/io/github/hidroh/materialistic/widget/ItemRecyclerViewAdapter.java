package io.github.hidroh.materialistic.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AlertDialogBuilder;
import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.ComposeActivity;
import io.github.hidroh.materialistic.Injectable;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;

public abstract class ItemRecyclerViewAdapter<VH extends ItemRecyclerViewAdapter.ItemViewHolder>
        extends RecyclerView.Adapter<VH> {
    private static final String PROPERTY_MAX_LINES = "maxLines";
    private static final int DURATION_PER_LINE_MILLIS = 20;
    private int mLocalRevision = 0;
    protected LayoutInflater mLayoutInflater;
    private ItemManager mItemManager;
    @Inject UserServices mUserServices;
    @Inject PopupMenu mPopupMenu;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    protected Context mContext;
    private int mTertiaryTextColorResId;
    private int mSecondaryTextColorResId;
    private int mCardBackgroundColorResId;
    private int mCardHighlightColorResId;
    private int mContentMaxLines = Integer.MAX_VALUE;
    private String mUsername;

    public ItemRecyclerViewAdapter(ItemManager itemManager) {
        mItemManager = itemManager;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mContext = recyclerView.getContext();
        if (mContext instanceof Injectable) {
            ((Injectable) mContext).inject(this);
        }
        mLayoutInflater = LayoutInflater.from(mContext);
        TypedArray ta = mContext.obtainStyledAttributes(new int[]{
                android.R.attr.textColorTertiary,
                android.R.attr.textColorSecondary,
                R.attr.colorCardBackground,
                R.attr.colorCardHighlight
        });
        mTertiaryTextColorResId = ta.getInt(0, 0);
        mSecondaryTextColorResId = ta.getInt(1, 0);
        mCardBackgroundColorResId = ta.getInt(2, 0);
        mCardHighlightColorResId = ta.getInt(3, 0);
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
            clear(holder);
            load(holder, item);
        } else {
            bind(holder, item);
        }
    }

    public void setMaxLines(int maxLines) {
        mContentMaxLines = maxLines;
        notifyDataSetChanged();
    }

    public void setHighlightUsername(String username) {
        mUsername = username;
        notifyDataSetChanged();
    }

    protected abstract ItemManager.Item getItem(int position);

    @CallSuper
    protected void bind(final VH holder, final ItemManager.Item item) {
        if (item == null) {
            return;
        }
        highlightUserItem(holder, item);
        holder.mPostedTextView.setText(item.getDisplayedTime(mContext, false));
        AppUtils.setTextWithLinks(holder.mContentTextView, item.getText());
        decorateDead(holder, item);
        toggleCollapsibleContent(holder, item);
        bindActions(holder, item);
    }

    protected void clear(VH holder) {
        holder.mCommentButton.setVisibility(View.GONE);
        holder.mPostedTextView.setOnClickListener(null);
        holder.mPostedTextView.setText(R.string.loading_text);
        holder.mContentTextView.setText(R.string.loading_text);
        holder.mReadMoreTextView.setVisibility(View.GONE);
    }

    private void load(final VH holder, final ItemManager.Item item) {
        mItemManager.getItem(item.getId(),
                new ResponseListener<ItemManager.Item>() {
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
    }

    private void highlightUserItem(VH holder, ItemManager.Item item) {
        boolean highlight = !TextUtils.isEmpty(mUsername) &&
                TextUtils.equals(mUsername, item.getBy());
        holder.mContentView.setBackgroundColor(highlight ?
                mCardHighlightColorResId : mCardBackgroundColorResId);
    }

    private void decorateDead(VH holder, ItemManager.Item item) {
        holder.mContentTextView.setTextColor(item.isDead() ?
                mSecondaryTextColorResId : mTertiaryTextColorResId);
    }

    private void toggleCollapsibleContent(final VH holder, final ItemManager.Item item) {
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

    private void setTextIsSelectable(TextView textView, boolean isSelectable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            textView.setTextIsSelectable(isSelectable);
        }
    }

    private void bindActions(final VH holder, final ItemManager.Item item) {
        holder.mMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupMenu.create(mContext, holder.mMoreButton, Gravity.NO_GRAVITY);
                mPopupMenu.inflate(R.menu.menu_contextual_comment);
                mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.menu_contextual_vote) {
                            vote(item);
                            return true;
                        }
                        if (menuItem.getItemId() == R.id.menu_contextual_comment) {
                            Intent intent = new Intent(mContext, ComposeActivity.class);
                            intent.putExtra(ComposeActivity.EXTRA_PARENT_ID, item.getId());
                            intent.putExtra(ComposeActivity.EXTRA_PARENT_TEXT, item.getText());
                            mContext.startActivity(intent);
                            return true;
                        }
                        return false;
                    }
                });
                mPopupMenu.show();
            }
        });
    }

    private void vote(final ItemManager.Item item) {
        mUserServices.voteUp(mContext, item.getId(), new UserServices.Callback() {
            @Override
            public void onDone(boolean successful) {
                if (successful) {
                    Toast.makeText(mContext, R.string.voted, Toast.LENGTH_SHORT).show();
                } else {
                    AppUtils.showLogin(mContext, mAlertDialogBuilder);
                }
            }

            @Override
            public void onError() {
                Toast.makeText(mContext, R.string.vote_failed, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView mPostedTextView;
        final TextView mContentTextView;
        final TextView mReadMoreTextView;
        final Button mCommentButton;
        final View mMoreButton;
        final View mContentView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mPostedTextView.setMovementMethod(LinkMovementMethod.getInstance());
            mContentTextView = (TextView) itemView.findViewById(R.id.text);
            mReadMoreTextView = (TextView) itemView.findViewById(R.id.more);
            mCommentButton = (Button) itemView.findViewById(R.id.comment);
            mCommentButton.setVisibility(View.GONE);
            mMoreButton = itemView.findViewById(R.id.button_more);
            mContentView = itemView.findViewById(R.id.content);
        }
    }
}
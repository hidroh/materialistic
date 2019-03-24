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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AlertDialogBuilder;
import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.ComposeActivity;
import io.github.hidroh.materialistic.Injectable;
import io.github.hidroh.materialistic.Navigable;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;

public abstract class ItemRecyclerViewAdapter<VH extends ItemRecyclerViewAdapter.ItemViewHolder>
        extends RecyclerViewAdapter<VH> {
    private static final String PROPERTY_MAX_LINES = "maxLines";
    private static final int DURATION_PER_LINE_MILLIS = 20;
    LayoutInflater mLayoutInflater;
    private ItemManager mItemManager;
    @Inject UserServices mUserServices;
    @Inject PopupMenu mPopupMenu;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    private int mTertiaryTextColorResId;
    private int mSecondaryTextColorResId;
    private int mCardBackgroundColorResId;
    private int mCardHighlightColorResId;
    private int mContentMaxLines = Integer.MAX_VALUE;
    private String mUsername;
    private final Map<String, Integer> mLineCounted = new HashMap<>();
    private int mCacheMode = ItemManager.MODE_DEFAULT;
    private float mLineHeight = 1.0f;

    public interface PositionCallback {
        void onPosition(int position);
    }

    ItemRecyclerViewAdapter(ItemManager itemManager) {
        mItemManager = itemManager;
    }

    @Override
    public void attach(Context context, RecyclerView recyclerView) {
        super.attach(context, recyclerView);
        if (mContext instanceof Injectable) {
            ((Injectable) mContext).inject(this);
        }
        mLayoutInflater = AppUtils.createLayoutInflater(mContext);
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
    public void onBindViewHolder(final VH holder, int position) {
        final Item item = getItem(position);
        if (item == null) {
            return;
        }
        clear(holder);
        if (item.getLocalRevision() < 0) {
            load(holder.getAdapterPosition(), item);
        } else if (item.getLocalRevision() > 0) {
            bind(holder, item);
        }
    }

    @Override
    public long getItemId(int position) {
        Item item = getItem(position);
        return item != null ? item.getLongId() : RecyclerView.NO_ID;
    }

    public void setCacheMode(int cacheMode) {
        mCacheMode = cacheMode;
    }

    public void initDisplayOptions(Context context) {
        mContentMaxLines = Preferences.getCommentMaxLines(context);
        mUsername = Preferences.getUsername(context);
        mLineHeight = Preferences.getLineHeight(context);
    }

    public void getNextPosition(int position, int direction, PositionCallback callback) {
        switch (direction) {
            case Navigable.DIRECTION_UP:
                callback.onPosition(position - 1);
                break;
            case Navigable.DIRECTION_DOWN:
                callback.onPosition(position + 1);
                break;
        }
    }

    public void lockBinding(int[] lock) { }

    @Nullable
    protected abstract Item getItem(int position);

    @CallSuper
    protected void bind(final VH holder, final Item item) {
        if (item == null) {
            return;
        }
        highlightUserItem(holder, item);
        decorateDead(holder, item);
        holder.mContentTextView.setLineSpacing(0f, mLineHeight);
        AppUtils.setTextWithLinks(holder.mContentTextView, item.getDisplayedText());
        Integer lineCount = mLineCounted.get(item.getId());
        if (lineCount != null && lineCount > 0) {
            toggleCollapsibleContent(holder, item, lineCount);
        } else {
            holder.mContentTextView.post(() -> {
                if (mContext == null) {
                    return;
                }
                int count = holder.mContentTextView.getLineCount();
                mLineCounted.put(item.getId(), count);
                toggleCollapsibleContent(holder, item, count);
            });
        }
        bindActions(holder, item);
    }

    protected void clear(VH holder) {
        holder.mCommentButton.setVisibility(View.GONE);
        holder.mPostedTextView.setOnClickListener(null);
        holder.mPostedTextView.setText(R.string.loading_text);
        holder.mContentTextView.setText(R.string.loading_text);
        holder.mReadMoreTextView.setVisibility(View.GONE);
    }

    @Synthetic
    boolean isAttached() {
        return mContext != null;
    }

    private void load(int adapterPosition, Item item) {
        item.setLocalRevision(0);
        mItemManager.getItem(item.getId(), mCacheMode,
                new ItemResponseListener(this, adapterPosition, item));
    }

    protected void onItemLoaded(int position, Item item) {
        if (position < getItemCount()) {
            notifyItemChanged(position);
        }
    }

    private void highlightUserItem(VH holder, Item item) {
        boolean highlight = !TextUtils.isEmpty(mUsername) &&
                TextUtils.equals(mUsername, item.getBy());
        holder.mContentView.setBackgroundColor(highlight ?
                mCardHighlightColorResId : mCardBackgroundColorResId);
    }

    private void decorateDead(VH holder, Item item) {
        holder.mContentTextView.setTextColor(item.isDead() ?
                mSecondaryTextColorResId : mTertiaryTextColorResId);
    }

    private void toggleCollapsibleContent(final VH holder, final Item item, int lineCount) {
        if (item.isContentExpanded() || lineCount <= mContentMaxLines) {
            holder.mContentTextView.setMaxLines(Integer.MAX_VALUE);
            holder.mReadMoreTextView.setVisibility(View.GONE);
            return;
        }
        holder.mContentTextView.setMaxLines(mContentMaxLines);
        holder.mReadMoreTextView.setVisibility(View.VISIBLE);
        holder.mReadMoreTextView.setText(mContext.getString(R.string.read_more, lineCount));
        holder.mReadMoreTextView.setOnClickListener(v -> {
            item.setContentExpanded(true);
            v.setVisibility(View.GONE);
            ObjectAnimator.ofInt(holder.mContentTextView, PROPERTY_MAX_LINES, lineCount)
                    .setDuration((lineCount - mContentMaxLines) * DURATION_PER_LINE_MILLIS)
                    .start();
        });
    }

    private void bindActions(final VH holder, final Item item) {
        if (item.isDead() || item.isDeleted()) {
            holder.mMoreButton.setVisibility(View.INVISIBLE);
            return;
        }
        holder.mMoreButton.setVisibility(View.VISIBLE);
        holder.mMoreButton.setOnClickListener(v ->
            mPopupMenu.create(mContext, holder.mMoreButton, Gravity.NO_GRAVITY)
                .inflate(R.menu.menu_contextual_comment)
                .setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.menu_contextual_vote) {
                        vote(item);
                        return true;
                    }
                    if (menuItem.getItemId() == R.id.menu_contextual_comment) {
                        mContext.startActivity(new Intent(mContext, ComposeActivity.class)
                                .putExtra(ComposeActivity.EXTRA_PARENT_ID, item.getId())
                                .putExtra(ComposeActivity.EXTRA_PARENT_TEXT, item.getText()));
                        return true;
                    }
                    if (menuItem.getItemId() == R.id.menu_contextual_share) {
                        AppUtils.share(mContext,
                                item.isStoryType() ? item.getDisplayedTitle() : null,
                                item.isStoryType() ? item.getUrl() :
                                        item.getDisplayedText() == null ?
                                                null : item.getDisplayedText().toString());
                        return true;
                    }
                    return false;
                })
                .show());
    }

    private void vote(final Item item) {
        mUserServices.voteUp(mContext, item.getId(), new VoteCallback(this));
    }

    @Synthetic
    void onVoted(Boolean successful) {
        if (successful == null) {
            Toast.makeText(mContext, R.string.vote_failed, Toast.LENGTH_SHORT).show();
        } else if (successful) {
            Toast.makeText(mContext, R.string.voted, Toast.LENGTH_SHORT).show();
        } else {
            AppUtils.showLogin(mContext, mAlertDialogBuilder);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        boolean mIsFooter;
        TextView mPostedTextView;
        TextView mContentTextView;
        TextView mReadMoreTextView;
        TextView mCommentButton;
        View mMoreButton;
        View mContentView;

        ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mPostedTextView.setMovementMethod(LinkMovementMethod.getInstance());
            mContentTextView = (TextView) itemView.findViewById(R.id.text);
            mReadMoreTextView = (TextView) itemView.findViewById(R.id.more);
            mCommentButton = (TextView) itemView.findViewById(R.id.comment);
            mCommentButton.setVisibility(View.GONE);
            mMoreButton = itemView.findViewById(R.id.button_more);
            mContentView = itemView.findViewById(R.id.content);
        }

        ItemViewHolder(View itemView, @SuppressWarnings("UnusedParameters") Object payload) {
            super(itemView);
            mIsFooter = true;
        }

        boolean isFooter() {
            return mIsFooter;
        }
    }

    private static class ItemResponseListener implements ResponseListener<Item> {
        private final WeakReference<ItemRecyclerViewAdapter> mAdapter;
        private final int mPosition;
        private final Item mPartialItem;

        @Synthetic
        ItemResponseListener(ItemRecyclerViewAdapter adapter, int position,
                                    Item partialItem) {
            mAdapter = new WeakReference<>(adapter);
            mPosition = position;
            mPartialItem = partialItem;
        }

        @Override
        public void onResponse(@Nullable Item response) {
            if (mAdapter.get() != null && mAdapter.get().isAttached() && response != null) {
                mPartialItem.populate(response);
                mAdapter.get().onItemLoaded(mPosition, mPartialItem);
            }
        }

        @Override
        public void onError(String errorMessage) {
            // do nothing
        }
    }

    static class VoteCallback extends UserServices.Callback {
        private final WeakReference<ItemRecyclerViewAdapter> mAdapter;

        @Synthetic
        VoteCallback(ItemRecyclerViewAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);
        }

        @Override
        public void onDone(boolean successful) {
            if (mAdapter.get() != null && mAdapter.get().isAttached()) {
                mAdapter.get().onVoted(successful);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (mAdapter.get() != null && mAdapter.get().isAttached()) {
                mAdapter.get().onVoted(null);
            }
        }
    }
}

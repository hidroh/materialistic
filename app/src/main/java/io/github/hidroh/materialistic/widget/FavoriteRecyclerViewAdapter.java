/*
 * Copyright (c) 2016 Ha Duy Trung
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
import android.content.Intent;
import android.graphics.Canvas;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import androidx.collection.ArrayMap;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.ComposeActivity;
import io.github.hidroh.materialistic.MenuTintDelegate;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.Favorite;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.SyncScheduler;

public class FavoriteRecyclerViewAdapter extends ListRecyclerViewAdapter
        <ListRecyclerViewAdapter.ItemViewHolder, Favorite> {

    @Inject SyncScheduler mSyncScheduler;

    public interface ActionModeDelegate {

        boolean startActionMode(ActionMode.Callback callback);
        boolean isInActionMode();
        void stopActionMode();
    }

    private final ItemTouchHelper mItemTouchHelper;
    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        private boolean mPendingClear;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.menu_favorite_action, menu);
            mMenuTintDelegate.onOptionsMenuCreated(menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_clear) {
                mAlertDialogBuilder
                        .init(mContext)
                        .setMessage(R.string.confirm_clear_selected)
                        .setPositiveButton(android.R.string.ok,
                                (dialog, which) -> {
                                    mPendingClear = true;
                                    removeSelection();
                                    actionMode.finish();
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
                return true;
            }
            if (menuItem.getItemId() == R.id.menu_refresh) {
                refreshSelection();
                actionMode.finish();
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            if (!isAttached()) {
                return;
            }
            mActionModeDelegate.stopActionMode();
            if (mPendingClear) {
                mPendingClear = false;
            } else {
                mSelected.clear();
            }
            notifyDataSetChanged();
        }
    };
    @Synthetic final ActionModeDelegate mActionModeDelegate;
    @Synthetic final MenuTintDelegate mMenuTintDelegate;
    @Synthetic final ArrayMap<Integer, String> mSelected = new ArrayMap<>();
    private int mPendingAdd = -1;

    public FavoriteRecyclerViewAdapter(Context context, ActionModeDelegate actionModeDelegate) {
        super(context);
        mActionModeDelegate = actionModeDelegate;
        mMenuTintDelegate = new MenuTintDelegate();
        mMenuTintDelegate.onActivityCreated(mContext);
        mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(mContext) {
            @Override
            public int getSwipeDirs(RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder) {
                if (mActionModeDelegate != null && mActionModeDelegate.isInActionMode()) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    dismiss(viewHolder.itemView, viewHolder.getAdapterPosition());
                } else {
                    Favorite item = getItem(viewHolder.getAdapterPosition());
                    if (item != null) {
                        mSyncScheduler.scheduleSync(mContext, item.getId());
                    }
                    notifyItemChanged(viewHolder.getAdapterPosition());
                }
            }
        });
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mItemTouchHelper.attachToRecyclerView(null);
    }

    @Override
    protected ItemViewHolder create(ViewGroup parent, int viewType) {
        return new ItemViewHolder(mInflater.inflate(R.layout.item_favorite, parent, false));
    }

    @Override
    public int getItemCount() {
        return mFavoriteManager.getSize();
    }

    @Override
    protected void bindItem(final ItemViewHolder holder, int position) {
        final Favorite favorite = getItem(holder.getAdapterPosition());
        holder.setOnLongClickListener(v -> {
            if (mActionModeDelegate.startActionMode(mActionModeCallback)) {
                toggle(favorite.getId(), holder.getAdapterPosition());
                return true;
            }

            return false;
        });
        holder.bindMoreOptions(v -> showMoreOptions(v, favorite), false);
    }

    @Override
    protected boolean isItemAvailable(Favorite item) {
        return item != null;
    }

    @Override
    protected void handleItemClick(Favorite item, ItemViewHolder holder) {
        if (!mActionModeDelegate.isInActionMode()) {
            super.handleItemClick(item, holder);
        } else {
            toggle(item.getId(), holder.getAdapterPosition());
        }
    }

    @Override
    protected Favorite getItem(int position) {
        return mFavoriteManager.getItem(position);
    }

    @Override
    protected boolean isSelected(String itemId) {
        return super.isSelected(itemId) || mSelected.containsValue(itemId);
    }

    @Override
    protected int getItemCacheMode() {
        return ItemManager.MODE_CACHE;
    }

    public void notifyChanged() {
        if (getItemCount() == 0) {
            notifyDataSetChanged();
            return;
        }
        if (!mSelected.isEmpty()) { // has pending removals, notify removed
            List<Integer> positions = new ArrayList<>(mSelected.keySet());
            Collections.sort(positions);
            mSelected.clear();
            for (int i = positions.size() - 1; i >= 0; i--) {
                notifyItemRemoved(positions.get(i));
            }
        } else if (mPendingAdd >= 0) { // has pending insertion, notify inserted
            notifyItemInserted(mPendingAdd);
            mPendingAdd = -1;
        } else { // no pending changes, simply refresh list
            notifyDataSetChanged();
        }
    }

    @Synthetic
    void removeSelection() {
        mFavoriteManager.remove(mContext, mSelected.values());
    }

    @Synthetic
    void refreshSelection() {
        for (String id : mSelected.values()) {
            mSyncScheduler.scheduleSync(mContext, id);
        }
    }

    @Synthetic
    void dismiss(View view, final int position) {
        final Favorite item = getItem(position);
        mSelected.put(position, item.getId());
        mFavoriteManager.remove(mContext, mSelected.values());
        Snackbar.make(view, R.string.toast_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> {
                    mPendingAdd = position;
                    mFavoriteManager.add(mContext, item);
                })
                .show();
    }

    private void toggle(String itemId, int position) {
        if (mSelected.containsValue(itemId)) {
            mSelected.remove(position);
        } else {
            mSelected.put(position, itemId);
        }
        notifyItemChanged(position);
    }

    private void showMoreOptions(View v, final Favorite item) {
        mPopupMenu.create(mContext, v, Gravity.NO_GRAVITY)
                .inflate(R.menu.menu_contextual_favorite)
                .setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.menu_contextual_vote) {
                        vote(item);
                        return true;
                    }
                    if (menuItem.getItemId() == R.id.menu_contextual_comment) {
                        mContext.startActivity(new Intent(mContext, ComposeActivity.class)
                                .putExtra(ComposeActivity.EXTRA_PARENT_ID, item.getId())
                                .putExtra(ComposeActivity.EXTRA_PARENT_TEXT, item.getDisplayedTitle()));
                        return true;
                    }
                    if (menuItem.getItemId() == R.id.menu_contextual_share) {
                        AppUtils.share(mContext, item.getDisplayedTitle(), item.getUrl());
                        return true;
                    }
                    return false;
                })
                .show();
    }

    private void vote(final Favorite item) {
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

    static class VoteCallback extends UserServices.Callback {
        private final WeakReference<FavoriteRecyclerViewAdapter> mAdapter;

        @Synthetic
        VoteCallback(FavoriteRecyclerViewAdapter adapter) {
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

    static abstract class ItemTouchHelperCallback extends PeekabooTouchHelperCallback {

        private final String mDeleteText;
        private final String mRefreshText;
        private final int mDeleteColor;
        private final int mRefreshColor;

        ItemTouchHelperCallback(Context context) {
            super(context);
            mDeleteText = context.getString(R.string.delete);
            mRefreshText = context.getString(R.string.refresh);
            mDeleteColor = ContextCompat.getColor(context, R.color.red500);
            mRefreshColor = ContextCompat.getColor(context, R.color.lightBlueA700);
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            float alpha = 1 - Math.abs(dX) / viewHolder.itemView.getWidth();
            viewHolder.itemView.setAlpha(alpha);
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        protected String getLeftText() {
            return mDeleteText;
        }

        @Override
        protected String getRightText() {
            return mRefreshText;
        }

        @Override
        protected int getLeftTextColor() {
            return mDeleteColor;
        }

        @Override
        protected int getRightTextColor() {
            return mRefreshColor;
        }
    }
}

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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AlertDialogBuilder;
import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.CustomTabsDelegate;
import io.github.hidroh.materialistic.Injectable;
import io.github.hidroh.materialistic.ItemActivity;
import io.github.hidroh.materialistic.MultiPaneListener;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.WebItem;

/**
 * Base {@link RecyclerView.Adapter} class for list items
 * @param <VH>  view holder type, should contain title, posted, source and comment views
 * @param <T>   item type, should provide title, posted, source
 */
public abstract class ListRecyclerViewAdapter
        <VH extends ListRecyclerViewAdapter.ItemViewHolder, T extends WebItem>
        extends RecyclerView.Adapter<VH> {

    private static final String STATE_LAST_SELECTION_POSITION = "state:lastSelectedPosition";
    private static final int VIEW_TYPE_CARD = 0;
    private static final int VIEW_TYPE_FLAT = 1;
    private CustomTabsDelegate mCustomTabsDelegate;
    protected Context mContext;
    private MultiPaneListener mMultiPaneListener;
    LayoutInflater mInflater;
    @Inject PopupMenu mPopupMenu;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    @Inject UserServices mUserServices;
    @Inject FavoriteManager mFavoriteManager;
    private int mLastSelectedPosition = -1;
    private boolean mCardViewEnabled = true;
    private int mHotThreshold = Integer.MAX_VALUE;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
    private boolean mMultiWindowEnabled;

    public ListRecyclerViewAdapter(Context context) {
        mContext = context;
        mInflater = AppUtils.createLayoutInflater(mContext);
        ((Injectable) mContext).inject(this);
        mMultiPaneListener = (MultiPaneListener) mContext;
        mMultiWindowEnabled = Preferences.multiWindowEnabled(mContext);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mPreferenceObservable.subscribe(mContext, (key, contextChanged) ->
                mMultiWindowEnabled = Preferences.multiWindowEnabled(mContext),
                R.string.pref_multi_window);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mPreferenceObservable.unsubscribe(mContext);
    }

    @Override
    public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
        VH holder = create(parent, viewType);
        if (viewType == VIEW_TYPE_FLAT) {
            holder.flatten();
        }
        return holder;
    }

    @Override
    public final void onBindViewHolder(final VH holder, int position) {
        final T item = getItem(position);
        clearViewHolder(holder);
        if (!isItemAvailable(item)) {
            loadItem(holder.getAdapterPosition());
            return;
        }
        // TODO naive launch priority for now
        mCustomTabsDelegate.mayLaunchUrl(Uri.parse(item.getUrl()), null, null);
        holder.bind(item,
                mHotThreshold,
                mCardViewEnabled,
                isSelected(item.getId()),
                v -> handleItemClick(item, holder),
                v -> openItem(item));
        bindItem(holder, position);
    }

    @Override
    public final int getItemViewType(int position) {
        return mCardViewEnabled ? VIEW_TYPE_CARD : VIEW_TYPE_FLAT;
    }

    @Override
    public final long getItemId(int position) {
        return getItem(position).getLongId();
    }

    public final boolean isCardViewEnabled() {
        return mCardViewEnabled;
    }

    public final void setCardViewEnabled(boolean cardViewEnabled) {
        this.mCardViewEnabled = cardViewEnabled;
        notifyDataSetChanged();
    }

    public void setCustomTabsDelegate(CustomTabsDelegate customTabsDelegate) {
        mCustomTabsDelegate = customTabsDelegate;
    }

    public void setHotThresHold(int hotThresHold) {
        mHotThreshold = hotThresHold;
    }

    public Bundle saveState() {
        Bundle savedState = new Bundle();
        savedState.putInt(STATE_LAST_SELECTION_POSITION, mLastSelectedPosition);
        return savedState;
    }

    public void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }
        mLastSelectedPosition = savedState.getInt(STATE_LAST_SELECTION_POSITION);
    }

    final boolean isAttached() {
        return mContext != null;
    }

    protected abstract VH create(ViewGroup parent, int viewType);

    protected void loadItem(int adapterPosition) {
        // override to load item if needed
    }

    protected abstract void bindItem(VH holder, int position);

    protected abstract boolean isItemAvailable(T item);

    private void clearViewHolder(VH holder) {
        holder.clear();
    }

    /**
     * Checks if item with given ID has been selected
     * @param itemId    item ID to check
     * @return  true if selected, false otherwise or if selection is disabled
     */
    protected boolean isSelected(String itemId) {
        return mMultiPaneListener.isMultiPane() &&
                mMultiPaneListener.getSelectedItem() != null &&
                itemId.equals(mMultiPaneListener.getSelectedItem().getId());
    }

    /**
     * Gets item at position
     * @param position    item position
     * @return item at given position or null
     */
    protected abstract T getItem(int position);

    /**
     * Handles item click
     * @param item      clicked item
     * @param holder    clicked item view holder
     */
    protected void handleItemClick(T item, VH holder) {
        mMultiPaneListener.onItemSelected(item);
        if (isSelected(item.getId())) {
            notifyItemChanged(holder.getAdapterPosition());
            if (mLastSelectedPosition >= 0) {
                notifyItemChanged(mLastSelectedPosition);
            }
            mLastSelectedPosition = holder.getAdapterPosition();
        }
    }

    /**
     * Gets cache mode for {@link ItemManager}
     *
     * @return cache mode
     */
    @ItemManager.CacheMode
    protected abstract int getItemCacheMode();

    private void openItem(T item) {
        Intent intent = new Intent(mContext, ItemActivity.class)
                .putExtra(ItemActivity.EXTRA_CACHE_MODE, getItemCacheMode())
                .putExtra(ItemActivity.EXTRA_ITEM, item)
                .putExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true);
        mContext.startActivity(mMultiWindowEnabled ?
                AppUtils.multiWindowIntent((Activity) mContext, intent) : intent);
    }

    /**
     * Base {@link RecyclerView.ViewHolder} class for list item view
     */
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final StoryView mStoryView;
        private final FlatCardView mCardView;
        private final int mCardElevation;

        public interface ShowMoreOptionsListener {
            void showMoreOptions(View anchor);
        }

        ItemViewHolder(View itemView) {
            super(itemView);
            mCardView = (FlatCardView) itemView;
            mStoryView = itemView.findViewById(R.id.story_view);
            mCardElevation = itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.cardview_elevation);
        }

        public void bind(WebItem item,
                         int hotThreshold,
                         boolean selected,
                         boolean cardViewEnabled,
                         View.OnClickListener itemClickListener,
                         View.OnClickListener commentClickListener) {
            mCardView.setCardElevation(selected ? mCardElevation * 2 :
                    (cardViewEnabled ? mCardElevation : 0));
            mStoryView.setStory(item, hotThreshold);
            mStoryView.setChecked(selected);
            itemView.setOnClickListener(itemClickListener);
            mStoryView.setOnCommentClickListener(commentClickListener);
        }

        public void clear() {
            mCardView.setCardElevation(0);
            mStoryView.reset();
            itemView.setOnClickListener(null);
            itemView.setOnLongClickListener(null);
        }

        public void flatten() {
            mCardView.flatten();
        }

        public void animateVote(int score) {
            mStoryView.animateVote(score);
        }

        public void setViewed(boolean viewed) {
            mStoryView.setViewed(viewed);
        }

        public void setFavorite(boolean favorite) {
            mStoryView.setFavorite(favorite);
        }

        public void setUpdated(Item story, boolean updated, int change) {
            mStoryView.setUpdated(story, updated, change);
        }

        public void setChecked(boolean checked) {
            mStoryView.setChecked(checked);
        }

        public void setOnLongClickListener(View.OnLongClickListener longClickListener) {
            itemView.setOnLongClickListener(longClickListener);
        }

        public void bindMoreOptions(ShowMoreOptionsListener listener, boolean allowLongClick) {
            if (allowLongClick) {
                itemView.setOnLongClickListener(v -> {
                    listener.showMoreOptions(mStoryView.getMoreOptions());
                    return true;
                });
            }
            mStoryView.getMoreOptions().setOnClickListener(v -> listener.showMoreOptions(v));
        }
    }
}

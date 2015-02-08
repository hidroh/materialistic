package io.github.hidroh.materialistic;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;

public class ListFragment extends Fragment {

    private static final String EXTRA_ITEMS = ListFragment.class.getName() + ".EXTRA_ITEMS";
    private RecyclerView mRecyclerView;
    private ItemRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BroadcastReceiver mBroadcastReceiver;
    private int mLocalRevision = 0;
    private ItemManager.Item[] mItems = new ItemManager.Item[0];
    private ItemManager mItemManager;
    private View mEmptyView;
    private Set<String> mChangedFavorites = new HashSet<>();
    private ItemOpenListener mItemOpenListener;

    public static ListFragment instantiate(Context context, ItemManager itemManager) {
        ListFragment fragment = (ListFragment) Fragment.instantiate(context, ListFragment.class.getName());
        fragment.mItemManager = itemManager;
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mItemOpenListener = (ItemOpenListener) activity;
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (TextUtils.isEmpty(intent.getAction())) {
                    return;
                }

                if (FavoriteManager.ACTION_CLEAR.equals(intent.getAction())) {
                    mLocalRevision++;
                } else if (FavoriteManager.ACTION_ADD.equals(intent.getAction())) {
                    mChangedFavorites.add(intent.getStringExtra(FavoriteManager.ACTION_ADD_EXTRA_DATA));
                } else if (FavoriteManager.ACTION_REMOVE.equals(intent.getAction())) {
                    mChangedFavorites.add(intent.getStringExtra(FavoriteManager.ACTION_REMOVE_EXTRA_DATA));
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, FavoriteManager.makeClearIntentFilter());
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, FavoriteManager.makeAddIntentFilter());
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, FavoriteManager.makeRemoveIntentFilter());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_list, container, false);
        mEmptyView = view.findViewById(android.R.id.empty);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }
        });
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new RecyclerViewAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.textColorPrimary);
        mSwipeRefreshLayout.setProgressBackgroundColor(R.color.colorAccent);
        if (savedInstanceState == null) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                bindData();
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            bindData();
        } else {
            final Parcelable[] savedItems = savedInstanceState.getParcelableArray(EXTRA_ITEMS);
            if (savedItems instanceof ItemManager.Item[]) {
                mItems = (ItemManager.Item[]) savedItems;
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mAdapter.mSelectedItemId = null;
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        // refresh favorite state if any changes
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray(EXTRA_ITEMS, mItems);
    }

    @Override
    public void onDetach() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;
        mItemOpenListener = null;
        super.onDetach();
    }

    private void bindData() {
        mItemManager.getTopStories(new ItemManager.ResponseListener<ItemManager.Item[]>() {
            @Override
            public void onResponse(final ItemManager.Item[] response) {
                mItems = response;
                mRecyclerView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                mAdapter.notifyDataSetChanged();
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(String errorMessage) {
                mSwipeRefreshLayout.setRefreshing(false);
                if (mItems == null || mItems.length == 0) {
                    // TODO make refreshing indicator visible in empty view
                    mRecyclerView.setVisibility(View.INVISIBLE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.connection_error),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class ViewHolder extends ItemRecyclerViewAdapter.ItemViewHolder {
        private final View mBookmarked;
        private TextView mRankTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mRankTextView = (TextView) itemView.findViewById(R.id.rank);
            mBookmarked = itemView.findViewById(R.id.bookmarked);
            // TODO remember tinted drawable so we don't apply it again
            AppUtils.initTintedDrawable(getResources(), R.drawable.ic_mode_comment_grey600_48dp,
                    R.color.colorAccent);
        }
    }

    private class RecyclerViewAdapter extends ItemRecyclerViewAdapter<ViewHolder, ItemManager.Item> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(getLayoutInflater(null)
                    .inflate(R.layout.item_story, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.mRankTextView.setText(String.valueOf(position + 1));
            final ItemManager.Item story = getItem(position);
            if (story.getLocalRevision() < mLocalRevision || mChangedFavorites.contains(story.getId())) {
                story.setLocalRevision(mLocalRevision);
                mChangedFavorites.remove(story.getId());
                FavoriteManager.check(getActivity(), story.getId(),
                        new FavoriteManager.OperationCallbacks() {
                            @Override
                            public void onCheckComplete(boolean isFavorite) {
                                story.setFavorite(isFavorite);
                                decorateFavorite(holder, story);
                            }

                        });
            } else {
                decorateFavorite(holder, story);
            }
            if (!TextUtils.isEmpty(story.getTitle())) {
                bindViewHolder(holder, story);
            } else {
                clearViewHolder(holder);
                mItemManager.getItem(story.getId(), new ItemManager.ResponseListener<ItemManager.Item>() {
                    @Override
                    public void onResponse(ItemManager.Item response) {
                        if (response == null) {
                            return;
                        }

                        story.populate(response);
                        bindViewHolder(holder, story);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // do nothing
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mItems.length;
        }

        @Override
        protected void onItemSelected(ItemManager.Item item) {
            mItemOpenListener.onItemOpen(item);
        }

        @Override
        protected boolean isSelected(String itemId) {
            return !TextUtils.isEmpty(mSelectedItemId) && itemId.equals(mSelectedItemId);
        }

        private void decorateFavorite(ViewHolder holder, ItemManager.Item story) {
            holder.mBookmarked.setVisibility(story.isFavorite() ? View.VISIBLE : View.INVISIBLE);
        }

        protected void bindViewHolder(final ViewHolder holder, final ItemManager.Item story) {
            super.bindViewHolder(holder, story);
            if (story.getKidCount() > 0) {
                ((Button) holder.mCommentButton).setText(String.valueOf(story.getKidCount()));
                holder.mCommentButton.setVisibility(View.VISIBLE);
            } else {
                holder.mCommentButton.setVisibility(View.GONE);
            }
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final int toastMessageResId;
                    if (!story.isFavorite()) {
                        FavoriteManager.add(getActivity(), story);
                        toastMessageResId = R.string.toast_saved;
                    } else {
                        FavoriteManager.remove(getActivity(), story.getId());
                        toastMessageResId = R.string.toast_removed;
                    }
                    Toast.makeText(getActivity(), toastMessageResId, Toast.LENGTH_SHORT).show();
                    story.setFavorite(!story.isFavorite());
                    decorateFavorite(holder, story);
                    return true;
                }
            });
        }

        private ItemManager.Item getItem(int position) {
            return mItems[position];
        }
    }

}

package io.github.hidroh.materialistic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LongSparseArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.widget.AsteriskSpan;
import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;

public class ListFragment extends BaseFragment implements Scrollable {

    public static final String EXTRA_ITEM_MANAGER = ListFragment.class.getName() + ".EXTRA_ITEM_MANAGER";
    public static final String EXTRA_FILTER = ListFragment.class.getName() + ".EXTRA_FILTER";
    private static final String STATE_ITEMS = "state:items";
    private static final String STATE_FILTER = "state:filter";
    private static final String STATE_UPDATED = "state:updated";
    private static final String STATE_SHOW_ALL = "state:showAll";
    private static final String STATE_GREEN_ITEMS = "state:greenItems";
    private static final String STATE_HIGHLIGHT_UPDATED = "state:highlightUpdated";
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals(getActivity().getString(R.string.pref_highlight_updated))) {
                        mHighlightUpdated = sharedPreferences.getBoolean(key, true);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            };
    private RecyclerView mRecyclerView;
    private ListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BroadcastReceiver mBroadcastReceiver;
    private int mLocalRevision = 0;
    private ArrayList<ItemManager.Item> mItems;
    private ArrayList<ItemManager.Item> mUpdated = new ArrayList<>();
    private ArrayList<String> mGreenItems = new ArrayList<>();
    private LongSparseArray<ItemManager.Item> mItemIdMaps = new LongSparseArray<>();
    private ItemManager mItemManager;
    @Inject @Named(ActivityModule.HN) ItemManager mHnItemManager;
    @Inject @Named(ActivityModule.ALGOLIA) ItemManager mAlgoliaItemManager;
    @Inject @Named(ActivityModule.POPULAR) ItemManager mPopularItemManager;
    private View mErrorView;
    private View mEmptyView;
    private Set<String> mChangedFavorites = new HashSet<>();
    private Set<String> mViewed = new HashSet<>();
    private MultiPaneListener mMultiPaneListener;
    private RefreshCallback mRefreshCallback;
    private String mFilter;
    @Inject FavoriteManager mFavoriteManager;
    @Inject SessionManager mSessionManager;
    private boolean mResumed;
    private int mPrimaryTextColorResId;
    private int mSecondaryTextColorResId;
    private int mPromotedColorResId;
    private boolean mShowAll = true;
    private boolean mHighlightUpdated = true;

    public interface RefreshCallback {
        void onRefreshed();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        TypedArray ta = context.obtainStyledAttributes(new int[]{
                R.attr.themedTextColorPrimaryInverse,
                R.attr.themedTextColorSecondaryInverse,
        });
        mPrimaryTextColorResId = ta.getInt(0, 0);
        mSecondaryTextColorResId = ta.getInt(1, 0);
        mPromotedColorResId = ContextCompat.getColor(context, R.color.promoted);
        ta.recycle();
        mMultiPaneListener = (MultiPaneListener) context;
        if (context instanceof RefreshCallback) {
            mRefreshCallback = (RefreshCallback) context;
        }
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (FavoriteManager.ACTION_CLEAR.equals(intent.getAction())) {
                    mLocalRevision++;
                } else if (FavoriteManager.ACTION_ADD.equals(intent.getAction())) {
                    mChangedFavorites.add(intent.getStringExtra(FavoriteManager.ACTION_ADD_EXTRA_DATA));
                } else if (FavoriteManager.ACTION_REMOVE.equals(intent.getAction())) {
                    mChangedFavorites.add(intent.getStringExtra(FavoriteManager.ACTION_REMOVE_EXTRA_DATA));
                } else if (SessionManager.ACTION_ADD.equals(intent.getAction())) {
                    mViewed.add(intent.getStringExtra(SessionManager.ACTION_ADD_EXTRA_DATA));
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, FavoriteManager.makeClearIntentFilter());
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, FavoriteManager.makeAddIntentFilter());
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, FavoriteManager.makeRemoveIntentFilter());
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mBroadcastReceiver, SessionManager.makeAddIntentFilter());
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<ItemManager.Item> savedItems = savedInstanceState.getParcelableArrayList(STATE_ITEMS);
            setItems(savedItems);
            mUpdated = savedInstanceState.getParcelableArrayList(STATE_UPDATED);
            mGreenItems = savedInstanceState.getStringArrayList(STATE_GREEN_ITEMS);
            mFilter = savedInstanceState.getString(STATE_FILTER);
            mShowAll = savedInstanceState.getBoolean(STATE_SHOW_ALL, true);
            mHighlightUpdated = savedInstanceState.getBoolean(STATE_HIGHLIGHT_UPDATED, true);
        } else {
            mFilter = getArguments().getString(EXTRA_FILTER);
            mHighlightUpdated = Preferences.highlightUpdatedEnabled(getActivity());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_list, container, false);
        mErrorView = view.findViewById(android.R.id.empty);
        mEmptyView = view.findViewById(R.id.empty_search);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }
        });
        mRecyclerView.setHasFixedSize(true);
        final int margin = getResources().getDimensionPixelSize(R.dimen.margin);
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                outRect.set(margin, margin, margin, margin);
            }
        });
        mAdapter = new RecyclerViewAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.textColorPrimary);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorAccent);
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
                refresh();
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String managerClassName = getArguments().getString(EXTRA_ITEM_MANAGER);
        if (TextUtils.equals(managerClassName, AlgoliaClient.class.getName())) {
            mItemManager = mAlgoliaItemManager;
        } else if (TextUtils.equals(managerClassName, AlgoliaPopularClient.class.getName())) {
            mItemManager = mPopularItemManager;
        } else {
            mItemManager = mHnItemManager;
        }
        if (mItems != null) {
            bindData();
        } else {
            refresh();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        mResumed = true;
        // refresh favorite/viewed state if any changes
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_ITEMS, mItems);
        outState.putParcelableArrayList(STATE_UPDATED, mUpdated);
        outState.putStringArrayList(STATE_GREEN_ITEMS, mGreenItems);
        outState.putString(STATE_FILTER, mFilter);
        outState.putBoolean(STATE_SHOW_ALL, mShowAll);
        outState.putBoolean(STATE_HIGHLIGHT_UPDATED, mHighlightUpdated);
    }

    @Override
    public void onPause() {
        mResumed = false;
        super.onPause();
    }

    @Override
    public void onDetach() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
        mBroadcastReceiver = null;
        mMultiPaneListener = null;
        mRefreshCallback = null;
        super.onDetach();
    }

    @Override
    public void scrollToTop() {
        mRecyclerView.smoothScrollToPosition(0);
    }

    public void filter(String filter) {
        mFilter = filter;
        setItems(null); // prevent updated comparison
        mSwipeRefreshLayout.setRefreshing(true);
        refresh();
    }

    private void refresh() {
        mShowAll = true;
        mItemManager.getStories(mFilter, new ItemManager.ResponseListener<ItemManager.Item[]>() {
            @Override
            public void onResponse(final ItemManager.Item[] response) {
                if (response == null) {
                    onError(null);
                } else {
                    ArrayList<ItemManager.Item> updated = new ArrayList<>(Arrays.asList(response));
                    bindUpdated(updated);
                    setItems(updated);
                    bindData();
                    if (mRefreshCallback != null) {
                        mRefreshCallback.onRefreshed();
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                mSwipeRefreshLayout.setRefreshing(false);
                if (mItems == null || mItems.isEmpty()) {
                    // TODO make refreshing indicator visible in error view
                    mEmptyView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.INVISIBLE);
                    mErrorView.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.connection_error),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindUpdated(ArrayList<ItemManager.Item> updated) {
        if (!mHighlightUpdated) {
            return;
        }
        if (mItems == null) {
            return;
        }
        mUpdated.clear();
        mGreenItems.clear();
        for (ItemManager.Item item : updated) {
            ItemManager.Item currentRevision = mItemIdMaps.get(Long.valueOf(item.getId()));
            if (currentRevision == null) {
                mUpdated.add(item);
            } else {
                item.setLastKidCount(currentRevision.getLastKidCount());
                int lastRank = currentRevision.getRank();
                if (lastRank > item.getRank()) {
                    mGreenItems.add(item.getId());
                }
            }
        }
        if (mUpdated.isEmpty()) {
            return;
        }
        Snackbar.make(mRecyclerView,
                getString(R.string.new_stories_count, mUpdated.size()),
                Snackbar.LENGTH_LONG)
                .setAction(R.string.show_me, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mShowAll = false;
                        bindData();
                    }
                })
                .show();
    }

    private void bindData() {
        if (!mShowAll) {
            final Snackbar snackbar = Snackbar.make(mRecyclerView,
                    getString(R.string.showing_new_stories, mUpdated.size()),
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.show_all, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                    mUpdated.clear();
                    mShowAll = true;
                    bindData();
                }
            }).show();
        }
        if (mItems == null || mItems.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        mErrorView.setVisibility(View.GONE);
        mAdapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void setItems(ArrayList<ItemManager.Item> items) {
        mItems = items;
        mItemIdMaps.clear();
        if (items != null) {
            for (ItemManager.Item item : items) {
                mItemIdMaps.put(Long.valueOf(item.getId()), item);
            }
        }
    }

    private class ViewHolder extends ListRecyclerViewAdapter.ItemViewHolder {
        private final View mBookmarked;
        private final TextView mRankTextView;
        private final TextView mScoreTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mRankTextView = (TextView) itemView.findViewById(R.id.rank);
            mScoreTextView = (TextView) itemView.findViewById(R.id.score);
            mBookmarked = itemView.findViewById(R.id.bookmarked);
        }
    }

    private class RecyclerViewAdapter extends ListRecyclerViewAdapter<ViewHolder, ItemManager.Item> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(getLayoutInflater(null)
                    .inflate(R.layout.item_story, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            final ItemManager.Item story = getItem(position);
            holder.mRankTextView.setText(decorateUpdated(
                    String.valueOf(story.getRank()), mUpdated.contains(story)));
            decoratePromoted(holder, story);
            holder.mScoreTextView.setText(R.string.loading_text);
            if (story.getLocalRevision() < mLocalRevision || mChangedFavorites.contains(story.getId())) {
                story.setLocalRevision(mLocalRevision);
                mChangedFavorites.remove(story.getId());
                mFavoriteManager.check(getActivity(), story.getId(),
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
                        if (!mResumed) {
                            return;
                        }

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
            if (mShowAll) {
                return mItems != null ? mItems.size() : 0;
            } else {
                return mUpdated.size();
            }
        }

        @Override
        protected void handleItemClick(ItemManager.Item item, ViewHolder holder) {
            super.handleItemClick(item, holder);
            markAsViewed(item, holder);
        }

        @Override
        protected void handleCommentButtonClick(ItemManager.Item item, ViewHolder holder) {
            super.handleCommentButtonClick(item, holder);
            markAsViewed(item, holder);
        }

        @Override
        protected void onItemSelected(ItemManager.Item item, View itemView) {
            mMultiPaneListener.onItemSelected(item);
        }

        @Override
        protected boolean isSelected(String itemId) {
            return mMultiPaneListener.getSelectedItem() != null &&
                    itemId.equals(mMultiPaneListener.getSelectedItem().getId());
        }

        private void markAsViewed(ItemManager.Item item, ViewHolder holder) {
            mSessionManager.view(getActivity(), item.getId());
            item.setIsViewed(true);
            decorateViewed(holder, item);
        }

        private void decorateViewed(ViewHolder holder, ItemManager.Item story) {
            boolean viewed = mViewed.contains(story.getId()) ||
                    story.isViewed() != null && story.isViewed();
            ((TextView) holder.mTitleTextView.getCurrentView())
                    .setTextColor(viewed ? mSecondaryTextColorResId : mPrimaryTextColorResId);
        }

        private void decorateFavorite(ViewHolder holder, ItemManager.Item story) {
            holder.mBookmarked.setVisibility(story.isFavorite() ? View.VISIBLE : View.INVISIBLE);
        }

        private Spannable decorateUpdated(String text, boolean updated) {
            SpannableStringBuilder sb = new SpannableStringBuilder(text);
            if (mHighlightUpdated && updated) {
                sb.append("*");
                sb.setSpan(new AsteriskSpan(getActivity()), sb.length() - 1, sb.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return sb;
        }

        private void decoratePromoted(ViewHolder holder, ItemManager.Item story) {
            if (mHighlightUpdated && mGreenItems.contains(story.getId())) {
                holder.mRankTextView.setTextColor(mPromotedColorResId);
            } else {
                holder.mRankTextView.setTextColor(mPrimaryTextColorResId);
            }
        }

        @Override
        protected void bindViewHolder(final ViewHolder holder, final ItemManager.Item story) {
            super.bindViewHolder(holder, story);
            if (story.isViewed() == null) {
                mSessionManager.isViewed(getActivity(), story.getId(),
                        new SessionManager.OperationCallbacks() {
                            @Override
                            public void onCheckComplete(boolean isViewed) {
                                story.setIsViewed(isViewed);
                                decorateViewed(holder, story);
                            }
                        });
            } else {
                decorateViewed(holder, story);
            }
            holder.mScoreTextView.setText(getString(R.string.score, story.getScore()));
            if (story.getKidCount() > 0) {
                ((Button) holder.mCommentButton).setText(decorateUpdated(
                        getString(R.string.comments_count, story.getKidCount()),
                        story.hasNewKids()));
                holder.mCommentButton.setVisibility(View.VISIBLE);
            } else {
                holder.mCommentButton.setVisibility(View.GONE);
            }
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                private boolean mUndo;

                @Override
                public boolean onLongClick(View v) {
                    final int toastMessageResId;
                    if (!story.isFavorite()) {
                        mFavoriteManager.add(getActivity(), story);
                        toastMessageResId = R.string.toast_saved;
                    } else {
                        mFavoriteManager.remove(getActivity(), story.getId());
                        toastMessageResId = R.string.toast_removed;
                    }
                    if (!mUndo) {
                        Snackbar.make(mRecyclerView, toastMessageResId, Snackbar.LENGTH_SHORT)
                                .setAction(R.string.undo, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        mUndo = true;
                                        holder.itemView.performLongClick();
                                    }
                                })
                                .show();
                    }
                    story.setFavorite(!story.isFavorite());
                    decorateFavorite(holder, story);
                    mUndo = false;
                    return true;
                }
            });
        }

        private ItemManager.Item getItem(int position) {
            if (mShowAll) {
                return mItems.get(position);
            } else {
                return mUpdated.get(position);
            }
        }
    }

}

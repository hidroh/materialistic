package io.github.hidroh.materialistic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.github.hidroh.materialistic.data.FavoriteManager;

public class ListActivity extends BaseActivity {

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BroadcastReceiver mBroadcastReceiver;
    private int mLocalRevision = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.title_activity_list));
        setContentView(R.layout.activity_list);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }
        });
        mRecyclerView.setHasFixedSize(true);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.textColorPrimary);
        mSwipeRefreshLayout.setProgressBackgroundColor(R.color.colorAccent);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                bindData();
            }
        });
        bindData();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (TextUtils.isEmpty(intent.getAction())) {
                    return;
                }

                if (FavoriteManager.ACTION_CLEAR.equals(intent.getAction())) {
                    mLocalRevision++;
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                FavoriteManager.makeClearIntentFilter());;
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;
        super.onDestroy();
    }

    private void bindData() {
        HackerNewsClient.getInstance().getTopStories(new HackerNewsClient.ResponseListener<HackerNewsClient.Item[]>() {
            @Override
            public void onResponse(final HackerNewsClient.Item[] response) {
                mRecyclerView.setAdapter(new RecyclerViewAdapter(response));
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onError(String errorMessage) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder {
        private final View mBookmarked;
        private TextView mTitleTextView;
        private TextView mRankTextView;
        private TextView mPostedTextView;
        private Button mCommentButton;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mRankTextView = (TextView) itemView.findViewById(android.R.id.text1);
            mTitleTextView = (TextView) itemView.findViewById(android.R.id.text2);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mCommentButton = (Button) itemView.findViewById(R.id.comment);
            mBookmarked = itemView.findViewById(R.id.bookmarked);
            // TODO remember tinted drawable so we don't apply it again
            AppUtils.initTintedDrawable(getResources(), R.drawable.ic_mode_comment_grey600_48dp,
                    R.color.colorAccent);
        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<ItemViewHolder> {
        private final HackerNewsClient.Item[] mItems;

        private RecyclerViewAdapter(HackerNewsClient.Item[] items) {
            mItems = items;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ItemViewHolder(getLayoutInflater().inflate(R.layout.activity_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final ItemViewHolder holder, final int position) {
            final HackerNewsClient.Item story = mItems[position];
            holder.mRankTextView.setText(String.valueOf(position + 1));
            if (story.localRevision < mLocalRevision) {
                story.localRevision = mLocalRevision;
                FavoriteManager.check(ListActivity.this, String.valueOf(story.getId()),
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
                bindViewHolder(holder, null);
                HackerNewsClient.getInstance().getItem(String.valueOf(story.getId()),
                        new HackerNewsClient.ResponseListener<HackerNewsClient.Item>() {
                            @Override
                            public void onResponse(HackerNewsClient.Item response) {
                                story.populate(response);
                                bindViewHolder(holder, story);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // do nothing
                            }
                        });
            }
            holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent(ListActivity.this, ItemActivity.class);
                    intent.putExtra(ItemActivity.EXTRA_ITEM, story);
                    final ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation(ListActivity.this,
                                    holder.itemView, getString(R.string.transition_item_container));
                    ActivityCompat.startActivity(ListActivity.this, intent, options.toBundle());
                }
            });
        }

        @Override
        public int getItemCount() {
            return mItems.length;
        }

        private void decorateFavorite(ItemViewHolder holder, HackerNewsClient.Item story) {
            holder.mBookmarked.setVisibility(story.isFavorite() ? View.VISIBLE : View.INVISIBLE);
        }

        private void bindViewHolder(final ItemViewHolder holder, final HackerNewsClient.Item story) {
            if (story == null) {
                holder.mTitleTextView.setText(getString(R.string.loading_text));
                holder.mPostedTextView.setText(getString(R.string.loading_text));
                holder.mCommentButton.setVisibility(View.GONE);
            } else {
                holder.mTitleTextView.setText(story.getTitle());
                holder.mPostedTextView.setText(story.getDisplayedTime(ListActivity.this));
                if (story.getKidCount() > 0) {
                    holder.mCommentButton.setText(String.valueOf(story.getKidCount()));
                    holder.mCommentButton.setVisibility(View.VISIBLE);
                } else {
                    holder.mCommentButton.setVisibility(View.GONE);
                }
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppUtils.openWebUrl(ListActivity.this, story);
                    }
                });
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        final int toastMessageResId;
                        if (!story.isFavorite()) {
                            FavoriteManager.add(ListActivity.this, story);
                            toastMessageResId = R.string.toast_saved;
                        } else {
                            FavoriteManager.remove(ListActivity.this, String.valueOf(story.getId()));
                            toastMessageResId = R.string.toast_removed;
                        }
                        Toast.makeText(ListActivity.this, toastMessageResId, Toast.LENGTH_SHORT).show();
                        story.setFavorite(!story.isFavorite());
                        decorateFavorite(holder, story);
                        return true;
                    }
                });
            }
        }
    }

}

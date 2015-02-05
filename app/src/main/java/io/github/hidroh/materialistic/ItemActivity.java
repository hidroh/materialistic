package io.github.hidroh.materialistic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public class ItemActivity extends BaseItemActivity {

    public static final String EXTRA_ITEM = ItemActivity.class.getName() + ".EXTRA_ITEM";
    public static final String EXTRA_ITEM_ID = ItemActivity.class.getName() + ".EXTRA_ITEM_ID";
    public static final String EXTRA_ITEM_LEVEL = ItemActivity.class.getName() + ".EXTRA_ITEM_LEVEL";
    private static final String PARAM_ID = "id";
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private ItemManager.Item mItem;
    private int mLocalRevision = 0;
    private CardView mHeaderCardView;
    private boolean mFavoriteBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);
        mEmptyView = findViewById(android.R.id.empty);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }
        });
        mRecyclerView.setHasFixedSize(true);

        final Intent intent = getIntent();
        String itemId = null;
        if (intent.hasExtra(EXTRA_ITEM)) {
            ItemManager.WebItem item = intent.getParcelableExtra(EXTRA_ITEM);
            itemId = item.getId();
            if (item instanceof ItemManager.Item) {
                mItem = (ItemManager.Item) item;
                bindData(mItem);
                return;
            }
        }

        if (TextUtils.isEmpty(itemId)) {
            itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        }

        if (TextUtils.isEmpty(itemId)) {
            if (intent.getAction() != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
                itemId = intent.getData() != null ? intent.getData().getQueryParameter(PARAM_ID) : null;
            }
        }

        if (!TextUtils.isEmpty(itemId)) {
            HackerNewsClient.getInstance(this).getItem(itemId, new ItemManager.ResponseListener<ItemManager.Item>() {
                @Override
                public void onResponse(ItemManager.Item response) {
                    mItem = response;
                    supportInvalidateOptionsMenu();
                    bindData(mItem);
                    bindFavorite();
                }

                @Override
                public void onError(String errorMessage) {
                    // do nothing
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindFavorite();
    }

    @Override
    protected void onPause() {
        mFavoriteBound = false;
        super.onPause();
    }

    private void bindFavorite() {
        if (mItem == null) {
            return;
        }

        if (!mItem.isShareable()) {
            return;
        }

        if (mFavoriteBound) { // prevent binding twice from onResponse and onResume
            return;
        }

        mFavoriteBound = true;
        FavoriteManager.check(this, mItem.getId(), new FavoriteManager.OperationCallbacks() {
            @Override
            public void onCheckComplete(boolean isFavorite) {
                super.onCheckComplete(isFavorite);
                decorateFavorite(isFavorite);
                mItem.setFavorite(isFavorite);
                mHeaderCardView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        final int toastMessageResId;
                        if (!mItem.isFavorite()) {
                            FavoriteManager.add(ItemActivity.this, mItem);
                            toastMessageResId = R.string.toast_saved;
                        } else {
                            FavoriteManager.remove(ItemActivity.this, mItem.getId());
                            toastMessageResId = R.string.toast_removed;
                        }
                        Toast.makeText(ItemActivity.this, toastMessageResId, Toast.LENGTH_SHORT).show();
                        mItem.setFavorite(!mItem.isFavorite());
                        decorateFavorite(mItem.isFavorite());
                        return true;
                    }
                });

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return mItem != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_external) {
            AppUtils.openWebUrlExternal(this, mItem.getUrl());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void bindData(final ItemManager.Item story) {
        if (story == null) {
            return;
        }

        final TextView titleTextView = (TextView) findViewById(android.R.id.text2);
        mHeaderCardView = (CardView) findViewById(R.id.header_card_view);
        if (story.isShareable()) {
            titleTextView.setText(story.getDisplayedTitle());
            titleTextView.setTextAppearance(this, R.style.textTitleStyle);
            if (!TextUtils.isEmpty(story.getSource())) {
                TextView sourceTextView = (TextView) findViewById(R.id.source);
                sourceTextView.setText(story.getSource());
                sourceTextView.setVisibility(View.VISIBLE);
            }
        } else {
            AppUtils.setHtmlText(titleTextView, story.getDisplayedTitle());
        }
        mHeaderCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (story.isShareable()) {
                    AppUtils.openWebUrl(ItemActivity.this, story);
                } else {
                    toggle();
                }
            }

            private void toggle() {
                final boolean isExpanded = titleTextView.getEllipsize() == null;
                titleTextView.setMaxLines(isExpanded ?
                        getResources().getInteger(R.integer.header_max_lines) : Integer.MAX_VALUE);
                titleTextView.setEllipsize(isExpanded ? TextUtils.TruncateAt.END : null);
                // TODO need to be scrollable if text is very long
                AppUtils.setHtmlText(titleTextView, story.getDisplayedTitle());
            }
        });

        int level = getIntent().getIntExtra(EXTRA_ITEM_LEVEL, 0);
        int stackResId = -1;
        int marginTop = getResources().getDimensionPixelSize(R.dimen.cardview_header_elevation) * Math.min(level, 4);
        // TODO can improve?
        switch (level) {
            case 0:
                break;
            case 1:
                stackResId = R.layout.header_stack_1;
                break;
            case 2:
                stackResId = R.layout.header_stack_2;
                break;
            case 3:
                stackResId = R.layout.header_stack_3;
                break;
            case 4:
            default:
                stackResId = R.layout.header_stack_4;
                break;
        }
        if (stackResId != -1) {
            getLayoutInflater().inflate(stackResId, (ViewGroup) findViewById(R.id.item_view), true);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mHeaderCardView.getLayoutParams();
            params.topMargin = marginTop;
            mHeaderCardView.setLayoutParams(params);
            mHeaderCardView.bringToFront();
        }

        final TextView postedTextView = (TextView) findViewById(R.id.posted);
        postedTextView.setText(story.getDisplayedTime(this));
        switch (story.getType()) {
            case job:
                postedTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_work_grey600_18dp, 0, 0, 0);
                break;
            case poll:
                postedTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_poll_grey600_18dp, 0, 0, 0);
                break;
        }
        bindKidData(story.getKidItems());
    }

    private void decorateFavorite(boolean isFavorite) {
        mHeaderCardView.findViewById(R.id.bookmarked)
                .setVisibility(isFavorite ? View.VISIBLE : View.INVISIBLE);
    }

    private void bindKidData(final ItemManager.Item[] items) {
        if (items == null || items.length == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        mRecyclerView.setAdapter(new RecyclerView.Adapter<ItemViewHolder>() {
            @Override
            public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new ItemViewHolder(getLayoutInflater()
                        .inflate(R.layout.activity_sub_item, parent, false));
            }

            @Override
            public void onBindViewHolder(final ItemViewHolder holder, int position) {
                final ItemManager.Item item = items[position];
                if (item.getLocalRevision() < mLocalRevision) {
                    bindKidItem(holder, null);
                    HackerNewsClient.getInstance(ItemActivity.this).getItem(item.getId(),
                            new ItemManager.ResponseListener<ItemManager.Item>() {
                                @Override
                                public void onResponse(ItemManager.Item response) {
                                    if (response == null) {
                                        return;
                                    }

                                    item.populate(response);
                                    item.setLocalRevision(mLocalRevision);
                                    bindKidItem(holder, item);
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    // do nothing
                                }
                            });
                } else {
                    bindKidItem(holder, item);
                }
            }

            private void bindKidItem(final ItemViewHolder holder, final ItemManager.Item item) {
                if (item == null) {
                    holder.mPostedTextView.setText(getString(R.string.loading_text));
                    holder.mContentTextView.setText(getString(R.string.loading_text));
                    holder.mCommentButton.setVisibility(View.INVISIBLE);
                } else {
                    holder.mPostedTextView.setText(item.getDisplayedTime(ItemActivity.this));
                    AppUtils.setTextWithLinks(holder.mContentTextView, item.getText());
                    if (item.getKidCount() > 0) {
                        holder.mCommentButton.setText(String.valueOf(item.getKidCount()));
                        holder.mCommentButton.setVisibility(View.VISIBLE);
                        holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final Intent intent = new Intent(ItemActivity.this, ItemActivity.class);
                                intent.putExtra(ItemActivity.EXTRA_ITEM, item);
                                intent.putExtra(ItemActivity.EXTRA_ITEM_LEVEL,
                                        getIntent().getIntExtra(EXTRA_ITEM_LEVEL, 0) + 1);
                                final ActivityOptionsCompat options = ActivityOptionsCompat
                                        .makeSceneTransitionAnimation(ItemActivity.this,
                                                holder.itemView, getString(R.string.transition_item_container));
                                ActivityCompat.startActivity(ItemActivity.this, intent, options.toBundle());
                            }
                        });
                    }
                }
            }

            @Override
            public int getItemCount() {
                return items.length;
            }
        });
    }

    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView mPostedTextView;
        private final TextView mContentTextView;
        private final Button mCommentButton;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mContentTextView = (TextView) itemView.findViewById(android.R.id.text1);
            mCommentButton = (Button) itemView.findViewById(R.id.comment);
            mCommentButton.setVisibility(View.INVISIBLE);
        }
    }
}

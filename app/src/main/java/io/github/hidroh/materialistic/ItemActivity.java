package io.github.hidroh.materialistic;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public class ItemActivity extends BaseItemActivity implements ItemObserver {

    public static final String EXTRA_ITEM = ItemActivity.class.getName() + ".EXTRA_ITEM";
    public static final String EXTRA_ITEM_ID = ItemActivity.class.getName() + ".EXTRA_ITEM_ID";
    public static final String EXTRA_ITEM_LEVEL = ItemActivity.class.getName() + ".EXTRA_ITEM_LEVEL";
    private static final String PARAM_ID = "id";
    private ItemManager.Item mItem;
    private View mHeaderCardView;
    private boolean mFavoriteBound;
    private boolean mIsResumed = true;
    private boolean mOrientationChanged = false;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    @Inject FavoriteManager mFavoriteManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);

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
            mItemManager.getItem(itemId, new ItemManager.ResponseListener<ItemManager.Item>() {
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mOrientationChanged = !mOrientationChanged;
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindFavorite();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        getMenuInflater().inflate(R.menu.menu_share, menu);
        if (mItem != null) {
            ((ShareActionProvider) MenuItemCompat.getActionProvider(
                    menu.findItem(R.id.menu_share)))
                    .setShareIntent(AppUtils.makeShareIntent(
                            getString(R.string.share_format,
                                    mItem.getDisplayedTitle(),
                                    String.format(HackerNewsClient.WEB_ITEM_PATH, mItem.getId()))));
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_share).setVisible(mItem != null);
        return mItem != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_external) {
            AppUtils.openWebUrlExternal(this,
                    String.format(HackerNewsClient.WEB_ITEM_PATH, mItem.getId()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mIsResumed = true;
    }

    @Override
    protected void onPause() {
        mFavoriteBound = false;
        mIsResumed = false;
        super.onPause();
    }

    @Override
    public void supportFinishAfterTransition() {
        if (mOrientationChanged) {
            /**
             * if orientation changed, finishing activity with shared element
             * transition may cause NPE if the original element is not visible in the returned
             * activity due to new orientation, we just finish without transition here
             */
            finish();
        } else {
            super.supportFinishAfterTransition();
        }
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
        mFavoriteManager.check(this, mItem.getId(), new FavoriteManager.OperationCallbacks() {
            @Override
            public void onCheckComplete(boolean isFavorite) {
                super.onCheckComplete(isFavorite);
                decorateFavorite(isFavorite);
                mItem.setFavorite(isFavorite);
                mHeaderCardView.setOnLongClickListener(new View.OnLongClickListener() {
                    private boolean mUndo;

                    @Override
                    public boolean onLongClick(View v) {
                        final int toastMessageResId;
                        if (!mItem.isFavorite()) {
                            mFavoriteManager.add(ItemActivity.this, mItem);
                            toastMessageResId = R.string.toast_saved;
                        } else {
                            mFavoriteManager.remove(ItemActivity.this, mItem.getId());
                            toastMessageResId = R.string.toast_removed;
                        }
                        if (!mUndo) {
                            Snackbar.make(v, toastMessageResId, Snackbar.LENGTH_SHORT)
                                    .setAction(R.string.undo, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            mUndo = true;
                                            mHeaderCardView.performLongClick();
                                        }
                                    })
                                    .show();
                        }
                        mItem.setFavorite(!mItem.isFavorite());
                        decorateFavorite(mItem.isFavorite());
                        mUndo = false;
                        return true;
                    }
                });

            }
        });
    }

    @Override
    public void onKidChanged(int kidCount) {
        setTitle(getString(R.string.title_activity_item_count, kidCount));
    }

    private void bindData(final ItemManager.Item story) {
        if (story == null) {
            return;
        }

        if (story.getKidCount() > 0) {
            setTitle(getString(R.string.title_activity_item_count, story.getKidCount()));
        }
        final TextView titleTextView = (TextView) findViewById(android.R.id.text2);
        mHeaderCardView = findViewById(R.id.header_card_view);
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
                if (AppUtils.isHackerNewsUrl(story)) {
                    openWeb(story);
                } else {
                    AppUtils.openWebUrl(ItemActivity.this, story);
                }
            }
        });

        decorateHeaderStack();
        final TextView postedTextView = (TextView) findViewById(R.id.posted);
        postedTextView.setText(story.getDisplayedTime(this));
        switch (story.getType()) {
            case ItemManager.Item.JOB_TYPE:
                postedTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_work_grey600_18dp, 0, 0, 0);
                break;
            case ItemManager.Item.POLL_TYPE:
                postedTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_poll_grey600_18dp, 0, 0, 0);
                break;
        }
        final Bundle args = new Bundle();
        args.putInt(EXTRA_ITEM_LEVEL, getIntent().getIntExtra(EXTRA_ITEM_LEVEL, 0));
        if (mIsResumed) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.sub_item_view,
                            ItemFragment.instantiate(this, mItem, args),
                            ItemFragment.class.getName())
                    .commit();
        }
    }

    private void openWeb(ItemManager.Item item) {
        final Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.EXTRA_ITEM, item);
        final ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this,
                        mHeaderCardView, getString(R.string.transition_item_container));
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }

    private void decorateHeaderStack() {
        int level = Math.min(getIntent().getIntExtra(EXTRA_ITEM_LEVEL, 0), 4);
        int stackMargin = getResources().getDimensionPixelSize(R.dimen.cardview_header_margin);
        ViewGroup itemView = (ViewGroup) findViewById(R.id.item_view);
        for (int i = 0; i < level; i++) {
            View stackView = getLayoutInflater().inflate(R.layout.header_stack, itemView, false);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) stackView.getLayoutParams();
            params.topMargin = stackMargin * i;
            stackView.setLayoutParams(params);
            itemView.addView(stackView);
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)
                mHeaderCardView.getLayoutParams();
        params.topMargin = stackMargin * level;
        mHeaderCardView.setLayoutParams(params);
        mHeaderCardView.bringToFront();
    }

    private void decorateFavorite(boolean isFavorite) {
        mHeaderCardView.findViewById(R.id.bookmarked)
                .setVisibility(isFavorite ? View.VISIBLE : View.INVISIBLE);
    }
}

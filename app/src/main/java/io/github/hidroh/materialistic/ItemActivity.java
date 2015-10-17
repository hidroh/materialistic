package io.github.hidroh.materialistic;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public class ItemActivity extends BaseItemActivity implements Scrollable {

    public static final String EXTRA_ITEM = ItemActivity.class.getName() + ".EXTRA_ITEM";
    public static final String EXTRA_ITEM_ID = ItemActivity.class.getName() + ".EXTRA_ITEM_ID";
    public static final String EXTRA_ITEM_LEVEL = ItemActivity.class.getName() + ".EXTRA_ITEM_LEVEL";
    public static final String EXTRA_OPEN_ARTICLE = ItemActivity.class.getName() + ".EXTRA_OPEN_ARTICLE";
    private static final String PARAM_ID = "id";
    private ItemManager.Item mItem;
    private ImageView mBookmark;
    private boolean mFavoriteBound;
    private boolean mOrientationChanged = false;
    private boolean mExternalBrowser;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    @Inject FavoriteManager mFavoriteManager;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    private TabLayout mTabLayout;
    private AppBarLayout mAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExternalBrowser = Preferences.externalBrowserEnabled(this);
        setContentView(R.layout.activity_item);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP);
        mBookmark = (ImageView) findViewById(R.id.bookmarked);
        mAppBar = (AppBarLayout) findViewById(R.id.appbar);
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
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
        mExternalBrowser = Preferences.externalBrowserEnabled(this);
        bindFavorite();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share, menu);
        getMenuInflater().inflate(R.menu.menu_item, menu);
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
            mAlertDialogBuilder
                    .setMessage(R.string.view_in_browser)
                    .setPositiveButton(R.string.article, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppUtils.openWebUrlExternal(ItemActivity.this, mItem.getUrl());
                        }
                    })
                    .setNegativeButton(R.string.comments, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AppUtils.openWebUrlExternal(ItemActivity.this,
                                    String.format(HackerNewsClient.WEB_ITEM_PATH, mItem.getId()));
                        }
                    })
                    .create()
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        mFavoriteBound = false;
        super.onPause();
    }

    @Override
    public void scrollToTop() {
        mAppBar.setExpanded(true, true);
    }

    private void bindFavorite() {
        if (mItem == null) {
            return;
        }

        if (!mItem.isShareable()) {
            return;
        }

        mBookmark.setVisibility(View.VISIBLE);
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
                mBookmark.setOnClickListener(new View.OnClickListener() {
                    private boolean mUndo;

                    @Override
                    public void onClick(View v) {
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
                                            mBookmark.performClick();
                                        }
                                    })
                                    .show();
                        }
                        mItem.setFavorite(!mItem.isFavorite());
                        decorateFavorite(mItem.isFavorite());
                        mUndo = false;
                    }
                });

            }
        });
    }

    private void bindData(final ItemManager.Item story) {
        if (story == null) {
            return;
        }

        final TextView titleTextView = (TextView) findViewById(android.R.id.text2);
        if (story.isShareable()) {
            titleTextView.setText(story.getDisplayedTitle());
            if (!TextUtils.isEmpty(story.getSource())) {
                TextView sourceTextView = (TextView) findViewById(R.id.source);
                sourceTextView.setText(story.getSource());
                sourceTextView.setVisibility(View.VISIBLE);
            }
        } else {
            AppUtils.setHtmlText(titleTextView, story.getDisplayedTitle());
        }

        final TextView postedTextView = (TextView) findViewById(R.id.posted);
        postedTextView.setText(story.getDisplayedTime(this, false));
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
        final Fragment[] fragments = new Fragment[2];
        Bundle args = new Bundle();
        args.putInt(EXTRA_ITEM_LEVEL, getIntent().getIntExtra(EXTRA_ITEM_LEVEL, 0));
        args.putParcelable(ItemFragment.EXTRA_ITEM, story);
        fragments[0] = Fragment.instantiate(ItemActivity.this, ItemFragment.class.getName(), args);
        fragments[1] = WebFragment.instantiate(ItemActivity.this, story);
        final ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments[position];
            }

            @Override
            public int getCount() {
                return story.isShareable() && !mExternalBrowser ? 2 : 1;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                if (position == 0) {
                    return getString(R.string.comments_count, story.getKidCount());
                } else {
                    return getString(R.string.article);
                }
            }
        });
        mTabLayout.setupWithViewPager(viewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                ((Scrollable) fragments[viewPager.getCurrentItem()]).scrollToTop();
                scrollToTop();
            }
        });
        if (viewPager.getAdapter().getCount() < 2) {
            AppBarLayout.LayoutParams p = (AppBarLayout.LayoutParams) mTabLayout.getLayoutParams();
            p.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
            mTabLayout.setLayoutParams(p);
        } else if (getIntent().getBooleanExtra(EXTRA_OPEN_ARTICLE, false)) {
            viewPager.setCurrentItem(1);
        }
        if (story.isShareable() && mExternalBrowser) {
            findViewById(R.id.header_card_view).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppUtils.openWebUrlExternal(ItemActivity.this, story.getUrl());
                }
            });
        } else {
            findViewById(R.id.header_card_view).setClickable(false);
        }
    }

    private void decorateFavorite(boolean isFavorite) {
        mBookmark.setImageResource(isFavorite ?
                R.drawable.ic_bookmark_white_24dp : R.drawable.ic_bookmark_outline_white_24dp);
    }
}

package io.github.hidroh.materialistic;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public class ItemActivity extends BaseItemActivity implements Scrollable {

    public static final String EXTRA_ITEM = ItemActivity.class.getName() + ".EXTRA_ITEM";
    public static final String EXTRA_ITEM_LEVEL = ItemActivity.class.getName() + ".EXTRA_ITEM_LEVEL";
    public static final String EXTRA_OPEN_COMMENTS = ItemActivity.class.getName() + ".EXTRA_OPEN_COMMENTS";
    private static final String PARAM_ID = "id";
    private static final String STATE_ITEM = "state:item";
    private static final String STATE_ITEM_ID = "state:itemId";
    private ItemManager.Item mItem;
    private String mItemId;
    private ImageView mBookmark;
    private boolean mFavoriteBound;
    private boolean mExternalBrowser;
    private Preferences.StoryViewMode mStoryViewMode;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    @Inject FavoriteManager mFavoriteManager;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    private TabLayout mTabLayout;
    private AppBarLayout mAppBar;
    private CoordinatorLayout mCoordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExternalBrowser = Preferences.externalBrowserEnabled(this);
        if (getIntent().getBooleanExtra(EXTRA_OPEN_COMMENTS, false)) {
            mStoryViewMode = Preferences.StoryViewMode.Comment;
        } else {
            mStoryViewMode = Preferences.getDefaultStoryView(this);
        }
        setContentView(R.layout.activity_item);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP);
        mBookmark = (ImageView) findViewById(R.id.bookmarked);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.content_frame);
        mAppBar = (AppBarLayout) findViewById(R.id.appbar);
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        final Intent intent = getIntent();
        if (savedInstanceState != null) {
            mItem = savedInstanceState.getParcelable(STATE_ITEM);
            mItemId = savedInstanceState.getString(STATE_ITEM_ID);
        } else {
            if (Intent.ACTION_VIEW.equalsIgnoreCase(intent.getAction())) {
                mItemId = intent.getData() != null ? intent.getData().getQueryParameter(PARAM_ID) : null;
            } else if (intent.hasExtra(EXTRA_ITEM)) {
                ItemManager.WebItem item = intent.getParcelableExtra(EXTRA_ITEM);
                mItemId = item.getId();
                if (item instanceof ItemManager.Item) {
                    mItem = (ItemManager.Item) item;
                }
            }
        }

        if (mItem != null) {
            bindData(mItem);
        } else if (!TextUtils.isEmpty(mItemId)) {
            mItemManager.getItem(mItemId, new ItemManager.ResponseListener<ItemManager.Item>() {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share, menu);
        getMenuInflater().inflate(R.menu.menu_item, menu);
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
        if (item.getItemId() == R.id.menu_share) {
            AppUtils.share(ItemActivity.this, mAlertDialogBuilder, mItem);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_ITEM, mItem);
        outState.putString(STATE_ITEM_ID, mItemId);
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
                            Snackbar.make(mCoordinatorLayout, toastMessageResId, Snackbar.LENGTH_SHORT)
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
        final Fragment[] fragments = new Fragment[3];
        final ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                if (position == 0) {
                    Bundle args = new Bundle();
                    args.putInt(EXTRA_ITEM_LEVEL, getIntent().getIntExtra(EXTRA_ITEM_LEVEL, 0));
                    args.putParcelable(ItemFragment.EXTRA_ITEM, story);
                    return Fragment.instantiate(ItemActivity.this, ItemFragment.class.getName(), args);
                } else if (position == getCount() - 1) {
                    Bundle readabilityArgs = new Bundle();
                    readabilityArgs.putString(ReadabilityFragment.EXTRA_URL, story.getUrl());
                    return Fragment.instantiate(ItemActivity.this,
                            ReadabilityFragment.class.getName(), readabilityArgs);
                } else {
                    return WebFragment.instantiate(ItemActivity.this, story);
                }
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                fragments[position] = (Fragment) super.instantiateItem(container, position);
                return fragments[position];
            }

            @Override
            public int getCount() {
                return story.isShareable() && !mExternalBrowser ? 3 : 2;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                if (position == 0) {
                    return getString(R.string.comments_count, story.getKidCount());
                } else if (position == getCount() - 1) {
                    return getString(R.string.readability);
                } else {
                    return getString(R.string.article);
                }
            }
        });
        mTabLayout.setupWithViewPager(viewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment activeFragment = fragments[viewPager.getCurrentItem()];
                if (activeFragment != null) {
                    ((Scrollable) activeFragment).scrollToTop();
                }
                scrollToTop();
            }
        });
        switch (mStoryViewMode) {
            case Article:
                if (viewPager.getAdapter().getCount() == 3) {
                    viewPager.setCurrentItem(1);
                }
                break;
            case Readability:
                viewPager.setCurrentItem(viewPager.getAdapter().getCount() - 1);
                break;
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

package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ItemManager;

/**
 * List activity that renders alternative layouts for portrait/landscape
 */
public abstract class BaseListActivity extends DrawerActivity implements MultiPaneListener {

    protected static final String LIST_FRAGMENT_TAG = BaseListActivity.class.getName() +
            ".LIST_FRAGMENT_TAG";
    private static final String STATE_SELECTED_ITEM = "state:selectedItem";
    private boolean mIsMultiPane;
    private boolean mIsResumed;
    protected ItemManager.WebItem mSelectedItem;
    private boolean mDefaultOpenArticle;
    private boolean mExternalBrowser;
    private ViewPager mViewPager;
    private CoordinatorLayout mContentView;
    @Inject ActionViewResolver mActionViewResolver;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    private TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExternalBrowser = Preferences.externalBrowserEnabled(this);
        setTitle(getDefaultTitle());
        super.setContentView(R.layout.activity_list);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        mContentView = (CoordinatorLayout) findViewById(R.id.content_frame);
        mViewPager = (ViewPager) findViewById(R.id.content);
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        onCreateView();
        final Fragment fragment;
        if (savedInstanceState == null) {
            fragment = instantiateListFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.list,
                            fragment,
                            LIST_FRAGMENT_TAG)
                    .commit();
        } else {
            mSelectedItem = savedInstanceState.getParcelable(STATE_SELECTED_ITEM);
            fragment = getSupportFragmentManager().findFragmentByTag(LIST_FRAGMENT_TAG);
        }
        if (fragment instanceof Scrollable) {
            findViewById(R.id.toolbar).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((Scrollable) fragment).scrollToTop();
                }
            });
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleConfigurationChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDefaultOpenArticle = !Preferences.isDefaultOpenComments(this);
        mExternalBrowser = Preferences.externalBrowserEnabled(this);
        if (isSearchable()) {
            // close search view
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mIsResumed = true;
        handleConfigurationChanged();
    }

    @Override
    protected void onPause() {
        mIsResumed = false;
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_SELECTED_ITEM, mSelectedItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getResources().getBoolean(R.bool.multi_pane)) {
            getMenuInflater().inflate(R.menu.menu_list_land, menu);
            menu.findItem(R.id.menu_share).getIcon()
                    .mutate()
                    .setColorFilter(ContextCompat.getColor(this,
                                    AppUtils.getThemedResId(this, android.R.attr.textColorPrimary)),
                            PorterDuff.Mode.SRC_IN);
        }

        if (isSearchable()) {
            getMenuInflater().inflate(R.menu.menu_search, menu);
            MenuItem menuSearch = menu.findItem(R.id.menu_search);
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) mActionViewResolver.getActionView(menuSearch);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(
                    new ComponentName(this, SearchActivity.class)));
            searchView.setIconified(true);
            searchView.setQuery("", false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!getResources().getBoolean(R.bool.multi_pane)) {
            return isSearchable() || super.onPrepareOptionsMenu(menu);
        }

        menu.findItem(R.id.menu_share).setVisible(isItemOptionsMenuVisible());
        return isSearchable() || super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_share) {
            AppUtils.share(BaseListActivity.this, mAlertDialogBuilder, mSelectedItem);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setContentView(int layoutResID) {
        addContentView(layoutResID);
    }

    @Override
    public void onItemSelected(ItemManager.WebItem item) {
        if (getSelectedItem() != null && item.getId().equals(getSelectedItem().getId())) {
            return;
        }

        mSelectedItem = item;
        if (mIsMultiPane) {
            handleMultiPaneItemSelected(item);
        } else {
            if (mExternalBrowser) {
                AppUtils.openWebUrlExternal(this, item.getUrl());
            } else {
                openItem(item);
            }
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    public void clearSelection() {
        mSelectedItem = null;
        if (mIsMultiPane) {
            setTitle(getDefaultTitle());
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    public ItemManager.WebItem getSelectedItem() {
        if (!mIsMultiPane) {
            return null; // item selection not applicable for single pane
        }

        return mSelectedItem;
    }

    /**
     * Checks if activity should have search view
     * @return true if is searchable, false otherwise
     */
    protected boolean isSearchable() {
        return true;
    }

    /**
     * Gets default title to be displayed in list-only layout
     * @return displayed title
     */
    protected abstract String getDefaultTitle();

    /**
     * Creates list fragment to host list data
     * @return list fragment
     */
    protected abstract Fragment instantiateListFragment();

    /**
     * Checks if item options menu should be displayed
     * @return  true to display item options menu, false otherwise
     */
    protected abstract boolean isItemOptionsMenuVisible();

    protected View addContentView(@LayoutRes int layoutResID) {
        View view = getLayoutInflater().inflate(layoutResID, mContentView, false);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)
                view.getLayoutParams();
        params.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        view.setLayoutParams(params);
        mContentView.addView(view);
        return view;
    }

    /**
     * Recreates view on first load or on orientation change that triggers layout change
     */
    protected void onCreateView() {}

    private void handleConfigurationChanged() {
        if (!mIsResumed) {
            return;
        }

        // only recreate view if orientation change triggers layout change
        if (mIsMultiPane == getResources().getBoolean(R.bool.multi_pane)) {
            return;
        }

        mIsMultiPane = getResources().getBoolean(R.bool.multi_pane);
        final RelativeLayout.LayoutParams params;
        if (mIsMultiPane) {
            params = new RelativeLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.list_width),
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            if (mSelectedItem != null) {
                handleMultiPaneItemSelected(mSelectedItem);
            }
        } else {
            setTitle(getDefaultTitle());
            params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
        }
        findViewById(android.R.id.list).setLayoutParams(params);
        supportInvalidateOptionsMenu();
    }

    private void handleMultiPaneItemSelected(final ItemManager.WebItem item) {
        setTitle(item.getDisplayedTitle());
        findViewById(R.id.empty).setVisibility(View.GONE);
        final Fragment[] fragments = new Fragment[3];
        mViewPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                if (position == 0) {
                    Bundle args = new Bundle();
                    args.putParcelable(ItemFragment.EXTRA_ITEM, item);
                    return Fragment.instantiate(BaseListActivity.this,
                            ItemFragment.class.getName(), args);
                } else if (position == getCount() - 1) {
                    Bundle readabilityArgs = new Bundle();
                    readabilityArgs.putString(ReadabilityFragment.EXTRA_URL, item.getUrl());
                    return Fragment.instantiate(BaseListActivity.this,
                            ReadabilityFragment.class.getName(), readabilityArgs);
                } else {
                    return WebFragment.instantiate(BaseListActivity.this, item);
                }
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                fragments[position] = (Fragment) super.instantiateItem(container, position);
                return fragments[position];
            }

            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                if (position == 0) {
                    return getString(R.string.title_activity_item);
                } else if (position == getCount() - 1) {
                    return getString(R.string.readability);
                } else {
                    return getString(R.string.article);
                }
            }
        });
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment activeFragment = fragments[mViewPager.getCurrentItem()];
                if (activeFragment != null) {
                    ((Scrollable) activeFragment).scrollToTop();
                }
            }
        });
        if (mDefaultOpenArticle) {
            // TODO add option to default to readability
            mViewPager.setCurrentItem(1);
        }
    }

    private void openItem(ItemManager.WebItem item) {
        final Intent intent = new Intent(this, ItemActivity.class);
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        intent.putExtra(ItemActivity.EXTRA_OPEN_ARTICLE, mDefaultOpenArticle);
        startActivity(intent);
    }
}

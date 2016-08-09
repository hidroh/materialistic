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

package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.widget.ItemPagerAdapter;
import io.github.hidroh.materialistic.widget.NavFloatingActionButton;
import io.github.hidroh.materialistic.widget.PopupMenu;
import io.github.hidroh.materialistic.widget.ViewPager;

/**
 * List activity that renders alternative layouts for portrait/landscape
 */
public abstract class BaseListActivity extends DrawerActivity implements MultiPaneListener {

    protected static final String LIST_FRAGMENT_TAG = BaseListActivity.class.getName() +
            ".LIST_FRAGMENT_TAG";
    private static final String STATE_SELECTED_ITEM = "state:selectedItem";
    private static final String STATE_STORY_VIEW_MODE = "state:storyViewMode";
    private static final String STATE_EXTERNAL_BROWSER = "state:externalBrowser";
    private static final String STATE_FULLSCREEN = "state:fullscreen";
    private boolean mIsMultiPane;
    protected WebItem mSelectedItem;
    private Preferences.StoryViewMode mStoryViewMode;
    private boolean mExternalBrowser;
    private ViewPager mViewPager;
    @Inject ActionViewResolver mActionViewResolver;
    @Inject PopupMenu mPopupMenu;
    @Inject SessionManager mSessionManager;
    @Inject CustomTabsDelegate mCustomTabsDelegate;
    @Inject KeyDelegate mKeyDelegate;
    private AppBarLayout mAppBar;
    private TabLayout mTabLayout;
    private FloatingActionButton mReplyButton;
    private NavFloatingActionButton mNavButton;
    private View mListView;
    private boolean mFullscreen;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mFullscreen = intent.getBooleanExtra(BaseWebFragment.EXTRA_FULLSCREEN, false);
            setFullscreen();
        }
    };

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setTitle(getDefaultTitle());
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        findViewById(R.id.toolbar).setOnClickListener(v -> {
            Scrollable scrollable = getScrollableList();
            if (scrollable != null) {
                scrollable.scrollToTop();
            }
        });
        mAppBar = (AppBarLayout) findViewById(R.id.appbar);
        mIsMultiPane = getResources().getBoolean(R.bool.multi_pane);
        if (mIsMultiPane) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                    new IntentFilter(BaseWebFragment.ACTION_FULLSCREEN));
            mListView = findViewById(android.R.id.list);
            mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
            mTabLayout.setVisibility(View.GONE);
            mViewPager = (ViewPager) findViewById(R.id.content);
            mViewPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.divider));
            mViewPager.setPageMarginDrawable(R.color.blackT12);
            mViewPager.setVisibility(View.GONE);
            mReplyButton = (FloatingActionButton) findViewById(R.id.reply_button);
            mNavButton = (NavFloatingActionButton) findViewById(R.id.navigation_button);
            mNavButton.setNavigable(direction ->
                    // if callback is fired navigable should not be null
                    ((Navigable) ((ItemPagerAdapter) mViewPager.getAdapter()).getItem(0))
                            .onNavigate(direction));
            toggleFab(false);
        }
        if (savedInstanceState == null) {
            mStoryViewMode = Preferences.getDefaultStoryView(this);
            mExternalBrowser = Preferences.externalBrowserEnabled(this);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.list,
                            instantiateListFragment(),
                            LIST_FRAGMENT_TAG)
                    .commit();
        } else {
            mStoryViewMode = Preferences.StoryViewMode.values()[
                    savedInstanceState.getInt(STATE_STORY_VIEW_MODE, 0)];
            mExternalBrowser = savedInstanceState.getBoolean(STATE_EXTERNAL_BROWSER);
            mSelectedItem = savedInstanceState.getParcelable(STATE_SELECTED_ITEM);
            mFullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN);
            if (mIsMultiPane) {
                openMultiPaneItem();
            } else {
                unbindViewPager();
            }
        }
        mPreferenceObservable.subscribe(this, this::onPreferenceChanged,
                R.string.pref_navigation,
                R.string.pref_external,
                R.string.pref_story_display);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!Preferences.isReleaseNotesSeen(this)) {
            startActivity(new Intent(this, ReleaseNotesActivity.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCustomTabsDelegate.bindCustomTabsService(this);
        mKeyDelegate.attach(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mIsMultiPane) {
            getMenuInflater().inflate(R.menu.menu_item, menu);
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
        if (mIsMultiPane) {
            menu.findItem(R.id.menu_share).setVisible(mSelectedItem != null);
            menu.findItem(R.id.menu_external).setVisible(mSelectedItem != null);
        }
        return isSearchable() || super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_share) {
            AppUtils.share(this, mPopupMenu, findViewById(R.id.menu_share), mSelectedItem);
            return true;
        }
        if (item.getItemId() == R.id.menu_external) {
            AppUtils.openExternal(this, mPopupMenu, findViewById(R.id.menu_external),
                    mSelectedItem, mCustomTabsDelegate.getSession());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_SELECTED_ITEM, mSelectedItem);
        outState.putInt(STATE_STORY_VIEW_MODE, mStoryViewMode.ordinal());
        outState.putBoolean(STATE_EXTERNAL_BROWSER, mExternalBrowser);
        outState.putBoolean(STATE_FULLSCREEN, mFullscreen);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCustomTabsDelegate.unbindCustomTabsService(this);
        mKeyDelegate.detach(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPreferenceObservable.unsubscribe(this);
        if (mIsMultiPane) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        }
    }
    @Override
    public void onBackPressed() {
        if (!mIsMultiPane || !mFullscreen) {
            super.onBackPressed();
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(
                    WebFragment.ACTION_FULLSCREEN).putExtra(WebFragment.EXTRA_FULLSCREEN, false));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mKeyDelegate.setScrollable(getScrollableList(), mAppBar);
        mKeyDelegate.setBackInterceptor(getBackInterceptor());
        return mKeyDelegate.onKeyDown(keyCode, event) ||
                super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mKeyDelegate.onKeyUp(keyCode, event) ||
                super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mKeyDelegate.onKeyLongPress(keyCode, event) ||
                super.onKeyLongPress(keyCode, event);
    }

    @NonNull
    @Override
    public ActionBar getSupportActionBar() {
        //noinspection ConstantConditions
        return super.getSupportActionBar();
    }

    @Override
    public void onItemSelected(@Nullable WebItem item) {
        WebItem previousItem = mSelectedItem;
        mSelectedItem = item;
        if (mIsMultiPane) {
            if (previousItem != null && item != null &&
                    TextUtils.equals(item.getId(), previousItem.getId())) {
                return;
            }
            if (previousItem == null && item != null ||
                    previousItem != null && item == null) {
                supportInvalidateOptionsMenu();
            }
            openMultiPaneItem();
        } else if (item != null) {
            openSinglePaneItem();
        }
    }

    @Override
    public WebItem getSelectedItem() {
        return mSelectedItem;
    }

    @Override
    public boolean isMultiPane() {
        return mIsMultiPane;
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
     * Gets cache mode for {@link ItemManager}
     * @return  cache mode
     */
    @ItemManager.CacheMode
    protected int getItemCacheMode() {
        return ItemManager.MODE_DEFAULT;
    }

    private void setFullscreen() {
        mAppBar.setExpanded(!mFullscreen, true);
        mTabLayout.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
        mListView.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
        mKeyDelegate.setAppBarEnabled(!mFullscreen);
        mViewPager.setSwipeEnabled(!mFullscreen);
        if (mFullscreen) {
            mReplyButton.hide();
        } else {
            mReplyButton.show();
        }
    }

    private Scrollable getScrollableList() {
        // TODO landscape behavior?
        return (Scrollable) getSupportFragmentManager().findFragmentByTag(LIST_FRAGMENT_TAG);
    }

    private KeyDelegate.BackInterceptor getBackInterceptor() {
        if (mViewPager == null ||
                mViewPager.getAdapter() == null ||
                mViewPager.getCurrentItem() < 0) {
            return null;
        }
        Fragment item = ((ItemPagerAdapter) mViewPager.getAdapter())
                .getItem(mViewPager.getCurrentItem());
        if (item instanceof KeyDelegate.BackInterceptor) {
            return (KeyDelegate.BackInterceptor) item;
        } else {
            return null;
        }
    }

    private void openSinglePaneItem() {
        if (mExternalBrowser) {
            AppUtils.openWebUrlExternal(this, mSelectedItem, mSelectedItem.getUrl(), mCustomTabsDelegate.getSession());
        } else {
            startActivity(new Intent(this, ItemActivity.class)
                    .putExtra(ItemActivity.EXTRA_CACHE_MODE, getItemCacheMode())
                    .putExtra(ItemActivity.EXTRA_ITEM, mSelectedItem));
        }
    }

    private void openMultiPaneItem() {
        if (mSelectedItem == null) {
            setTitle(getDefaultTitle());
            findViewById(R.id.empty_selection).setVisibility(View.VISIBLE);
            mTabLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mViewPager.setAdapter(null);
            toggleFab(false);
        } else {
            setTitle(mSelectedItem.getDisplayedTitle());
            findViewById(R.id.empty_selection).setVisibility(View.GONE);
            mTabLayout.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
            bindViewPager();
            mSessionManager.view(this, mSelectedItem.getId());
        }
    }

    private void bindViewPager() {
        final ItemPagerAdapter adapter = new ItemPagerAdapter(this,
                getSupportFragmentManager(), mSelectedItem, true, getItemCacheMode());
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                toggleFab(true);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment fragment = adapter.getItem(mViewPager.getCurrentItem());
                if (fragment != null) {
                    ((Scrollable) fragment).scrollToTop();
                }
            }
        });
        switch (mStoryViewMode) {
            case Article:
                mViewPager.setCurrentItem(1);
                break;
            case Readability:
                mViewPager.setCurrentItem(2);
                break;
        }
        toggleFab(true);
        if (mFullscreen) {
            setFullscreen();
        }
    }

    private void toggleFab(boolean on) {
        if (on) {
            AppUtils.toggleFab(mNavButton, navigationVisible());
            AppUtils.toggleFab(mReplyButton, true);
            AppUtils.toggleFabAction(mReplyButton, mSelectedItem, mViewPager.getCurrentItem() == 0);
        } else {
            AppUtils.toggleFab(mNavButton, false);
            AppUtils.toggleFab(mReplyButton, false);
        }
    }

    private void unbindViewPager() {
        // fragment manager always restores view pager fragments,
        // even when view pager no longer exists (e.g. after rotation),
        // so we have to explicitly remove those with view pager ID
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //noinspection Convert2streamapi
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.getId() == R.id.content) {
                transaction.remove(fragment);
            }
        }
        transaction.commit();
    }

    private void onPreferenceChanged(int key, boolean contextChanged) {
        switch (key) {
            case R.string.pref_external:
                mExternalBrowser = Preferences.externalBrowserEnabled(this);
                break;
            case R.string.pref_story_display:
                mStoryViewMode = Preferences.getDefaultStoryView(this);
                break;
            case R.string.pref_navigation:
                if (mNavButton != null) {
                    AppUtils.toggleFab(mNavButton, navigationVisible());
                }
                break;
        }
    }

    private boolean navigationVisible() {
        return mViewPager.getCurrentItem() == 0 && Preferences.navigationEnabled(this);
    }
}

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

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

import io.github.hidroh.materialistic.annotation.Synthetic;
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
    @Synthetic boolean mFullscreen;
    private boolean mMultiWindowEnabled;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mFullscreen = intent.getBooleanExtra(WebFragment.EXTRA_FULLSCREEN, false);
            setFullscreen();
        }
    };
    private ItemPagerAdapter mAdapter;

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
                    new IntentFilter(WebFragment.ACTION_FULLSCREEN));
            mListView = findViewById(android.R.id.list);
            mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
            mTabLayout.setVisibility(View.GONE);
            mViewPager = (ViewPager) findViewById(R.id.content);
            mViewPager.setVisibility(View.GONE);
            mReplyButton = (FloatingActionButton) findViewById(R.id.reply_button);
            mNavButton = (NavFloatingActionButton) findViewById(R.id.navigation_button);
            mNavButton.setNavigable(direction ->
                    // if callback is fired navigable should not be null
                    ((Navigable) ((ItemPagerAdapter) mViewPager.getAdapter()).getItem(0))
                            .onNavigate(direction));
            AppUtils.toggleFab(mNavButton, false);
            AppUtils.toggleFab(mReplyButton, false);
        }
        mMultiWindowEnabled = Preferences.multiWindowEnabled(this);
        mStoryViewMode = Preferences.getDefaultStoryView(this);
        mExternalBrowser = Preferences.externalBrowserEnabled(this);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.list,
                            instantiateListFragment(),
                            LIST_FRAGMENT_TAG)
                    .commit();
        } else {
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
                R.string.pref_story_display,
                R.string.pref_multi_window);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!Preferences.isReleaseNotesSeen(this)) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.content_frame),
                    R.string.hint_update, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.title_activity_release,
                    v -> {
                        snackbar.dismiss();
                        startActivity(new Intent(BaseListActivity.this, ReleaseNotesActivity.class));
                    })
                    .setActionTextColor(ContextCompat.getColor(this, R.color.orange500))
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            Preferences.setReleaseNotesSeen(BaseListActivity.this);
                        }
                    })
                    .show();
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
            getMenuInflater().inflate(R.menu.menu_item_compact, menu);
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
            View anchor = findViewById(R.id.menu_share);
            AppUtils.share(this, mPopupMenu, anchor == null ?
                    findViewById(R.id.toolbar) : anchor, mSelectedItem);
            return true;
        }
        if (item.getItemId() == R.id.menu_external) {
            View anchor = findViewById(R.id.menu_external);
            AppUtils.openExternal(this, mPopupMenu, anchor == null ?
                    findViewById(R.id.toolbar) : anchor,
                    mSelectedItem, mCustomTabsDelegate.getSession());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_SELECTED_ITEM, mSelectedItem);
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

    @Synthetic
    void setFullscreen() {
        mAppBar.setExpanded(!mFullscreen, true);
        mTabLayout.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
        mListView.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
        mKeyDelegate.setAppBarEnabled(!mFullscreen);
        mViewPager.setSwipeEnabled(!mFullscreen);
        AppUtils.toggleFab(mReplyButton, !mFullscreen);
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
        if (mExternalBrowser && mStoryViewMode != Preferences.StoryViewMode.Comment) {
            AppUtils.openWebUrlExternal(this, mSelectedItem, mSelectedItem.getUrl(), mCustomTabsDelegate.getSession());
        } else {
            Intent intent = new Intent(this, ItemActivity.class)
                    .putExtra(ItemActivity.EXTRA_CACHE_MODE, getItemCacheMode())
                    .putExtra(ItemActivity.EXTRA_ITEM, mSelectedItem);
            startActivity(mMultiWindowEnabled ? AppUtils.multiWindowIntent(this, intent) : intent);
        }
    }

    private void openMultiPaneItem() {
        if (mSelectedItem == null) {
            setTitle(getDefaultTitle());
            findViewById(R.id.empty_selection).setVisibility(View.VISIBLE);
            mTabLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mViewPager.setAdapter(null);
            AppUtils.toggleFab(mNavButton, false);
            AppUtils.toggleFab(mReplyButton, false);
        } else {
            setTitle(mSelectedItem.getDisplayedTitle());
            findViewById(R.id.empty_selection).setVisibility(View.GONE);
            mTabLayout.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
            bindViewPager();
            mSessionManager.view(mSelectedItem.getId());
        }
    }

    private void bindViewPager() {
        if (mAdapter != null) {
            mAdapter.unbind(mTabLayout);
        }
        mAdapter = new ItemPagerAdapter(this, getSupportFragmentManager(), new ItemPagerAdapter.Builder()
                .setItem(mSelectedItem)
                .setCacheMode(getItemCacheMode())
                .setShowArticle(true)
                .setDefaultViewMode(mStoryViewMode));
        mAdapter.bind(mViewPager, mTabLayout, mNavButton, mReplyButton);
        if (mFullscreen) {
            setFullscreen();
        }
    }

    @SuppressLint("RestrictedApi")
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
                boolean enabled = Preferences.navigationEnabled(this);
                if (!enabled) {
                    NavFloatingActionButton.resetPosition(this);
                }
                if (mNavButton != null) {
                    AppUtils.toggleFab(mNavButton, mViewPager.getCurrentItem() == 0 && enabled);
                }
                break;
            case R.string.pref_multi_window:
                mMultiWindowEnabled = Preferences.multiWindowEnabled(this);
                break;
        }
    }
}

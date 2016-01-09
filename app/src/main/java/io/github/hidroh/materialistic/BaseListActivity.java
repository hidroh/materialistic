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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.widget.ItemPagerAdapter;

/**
 * List activity that renders alternative layouts for portrait/landscape
 */
public abstract class BaseListActivity extends DrawerActivity implements MultiPaneListener {

    protected static final String LIST_FRAGMENT_TAG = BaseListActivity.class.getName() +
            ".LIST_FRAGMENT_TAG";
    private static final String STATE_SELECTED_ITEM = "state:selectedItem";
    private static final String STATE_STORY_VIEW_MODE = "state:storyViewMode";
    private static final String STATE_EXTERNAL_BROWSER = "state:externalBrowser";
    private boolean mIsMultiPane;
    protected ItemManager.WebItem mSelectedItem;
    private Preferences.StoryViewMode mStoryViewMode;
    private boolean mExternalBrowser;
    private ViewPager mViewPager;
    @Inject ActionViewResolver mActionViewResolver;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    @Inject SessionManager mSessionManager;
    private TabLayout mTabLayout;
    private FloatingActionButton mReplyButton;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (TextUtils.equals(key, getString(R.string.pref_external))) {
                mExternalBrowser = Preferences.externalBrowserEnabled(BaseListActivity.this);
            } else if (TextUtils.equals(key, getString(R.string.pref_story_display))) {
                mStoryViewMode = Preferences.getDefaultStoryView(BaseListActivity.this);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setTitle(getDefaultTitle());
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        findViewById(R.id.toolbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(LIST_FRAGMENT_TAG);
                if (fragment instanceof Scrollable) {
                    ((Scrollable) fragment).scrollToTop();
                }
            }
        });
        mIsMultiPane = getResources().getBoolean(R.bool.multi_pane);
        if (mIsMultiPane) {
            mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
            mTabLayout.setVisibility(View.GONE);
            mViewPager = (ViewPager) findViewById(R.id.content);
            mViewPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.divider));
            mViewPager.setPageMarginDrawable(R.color.blackT12);
            mViewPager.setVisibility(View.GONE);
            mReplyButton = (FloatingActionButton) findViewById(R.id.reply_button);
            AppUtils.toggleFab(mReplyButton, false);
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
            if (mIsMultiPane) {
                openMultiPaneItem(mSelectedItem);
            } else {
                unbindViewPager();
            }
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mPreferenceListener);
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
            AppUtils.share(BaseListActivity.this, mAlertDialogBuilder, mSelectedItem);
            return true;
        }
        if (item.getItemId() == R.id.menu_external) {
            AppUtils.openExternal(BaseListActivity.this, mAlertDialogBuilder, mSelectedItem);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    @NonNull
    @Override
    public ActionBar getSupportActionBar() {
        //noinspection ConstantConditions
        return super.getSupportActionBar();
    }

    @Override
    public void onItemSelected(@Nullable ItemManager.WebItem item) {
        if (mIsMultiPane) {
            ItemManager.WebItem previousItem = mSelectedItem;
            if (previousItem != null && item != null &&
                    TextUtils.equals(item.getId(), previousItem.getId())) {
                return;
            }
            if (previousItem == null && item != null ||
                    previousItem != null && item == null) {
                supportInvalidateOptionsMenu();
            }
            openMultiPaneItem(item);
        } else if (item != null) {
            openSinglePaneItem(item);
        }
        mSelectedItem = item;
    }

    @Override
    public ItemManager.WebItem getSelectedItem() {
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

    private void openSinglePaneItem(ItemManager.WebItem item) {
        if (mExternalBrowser) {
            AppUtils.openWebUrlExternal(this, item.getDisplayedTitle(), item.getUrl());
        } else {
            startActivity(new Intent(this, ItemActivity.class)
                    .putExtra(ItemActivity.EXTRA_ITEM, item));
        }
    }

    private void openMultiPaneItem(final ItemManager.WebItem item) {
        if (item == null) {
            setTitle(getDefaultTitle());
            findViewById(R.id.empty_selection).setVisibility(View.VISIBLE);
            mTabLayout.setVisibility(View.GONE);
            mViewPager.setVisibility(View.GONE);
            mViewPager.setAdapter(null);
            AppUtils.toggleFab(mReplyButton, false);
        } else {
            setTitle(item.getDisplayedTitle());
            findViewById(R.id.empty_selection).setVisibility(View.GONE);
            mTabLayout.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.VISIBLE);
            AppUtils.toggleFab(mReplyButton, true);
            mReplyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(BaseListActivity.this, ComposeActivity.class)
                            .putExtra(ComposeActivity.EXTRA_PARENT_ID, item.getId())
                            .putExtra(ComposeActivity.EXTRA_PARENT_TEXT, item.getDisplayedTitle()));
                }
            });
            bindViewPager(item);
            mSessionManager.view(this, item.getId());
        }
    }

    private void bindViewPager(ItemManager.WebItem item) {
        final ItemPagerAdapter adapter = new ItemPagerAdapter(this,
                getSupportFragmentManager(), item, true);
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
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
    }

    private void unbindViewPager() {
        // fragment manager always restores view pager fragments,
        // even when view pager no longer exists (e.g. after rotation),
        // so we have to explicitly remove those with view pager ID
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.getId() == R.id.content) {
                transaction.remove(fragment);
            }
        }
        transaction.commit();
    }

}

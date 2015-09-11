package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import javax.inject.Inject;

public abstract class BaseActivity extends InjectableActivity {

    private RelativeLayout mContentView;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private View mDrawer;
    @Inject
    ActionViewResolver mActionViewResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_base);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        mContentView = (RelativeLayout) findViewById(R.id.content_frame);
        mDrawer = findViewById(R.id.drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.open_drawer,
                R.string.close_drawer);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSearchable()) {
            // close search view
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
        if (isSearchable()) {
            return true;
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setContentView(int layoutResID) {
        addContentView(layoutResID);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawer)) {
            closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    protected View addContentView(@LayoutRes int layoutResID) {
        View view = getLayoutInflater().inflate(layoutResID, mContentView, false);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.toolbar);
        view.setLayoutParams(params);
        mContentView.addView(view);
        return view;
    }

    protected void closeDrawers() {
        mDrawerLayout.closeDrawers();
    }

    /**
     * Checks if activity should have search view
     * @return true if is searchable, false otherwise
     */
    protected abstract boolean isSearchable();
}

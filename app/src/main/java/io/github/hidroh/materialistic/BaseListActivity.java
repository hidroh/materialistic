package io.github.hidroh.materialistic;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

import io.github.hidroh.materialistic.data.ItemManager;

/**
 * List activity that renders alternative layouts for portrait/landscape
 */
public abstract class BaseListActivity extends BaseActivity implements MultiPaneListener {

    private static final String LIST_FRAGMENT_TAG = BaseListActivity.class.getName() + ".LIST_FRAGMENT_TAG";
    private boolean mIsMultiPane;
    private boolean mIsResumed;
    protected ItemManager.WebItem mSelectedItem;
    private boolean mDefaultOpenComments;
    private boolean mStoryMode;
    private boolean mExternalBrowser;
    private ViewSwitcher mViewSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        mExternalBrowser = Preferences.externalBrowserEnabled(this);
        setTitle(getDefaultTitle());
        setContentView(R.layout.activity_list);
        mViewSwitcher = (ViewSwitcher) findViewById(R.id.content);
        onCreateView();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.list,
                        instantiateListFragment(),
                        LIST_FRAGMENT_TAG)
                .commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleConfigurationChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDefaultOpenComments = Preferences.isDefaultOpenComments(this);
        mExternalBrowser = Preferences.externalBrowserEnabled(this);
        mStoryMode = !mDefaultOpenComments;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!getResources().getBoolean(R.bool.multi_pane)) {
            return super.onCreateOptionsMenu(menu);
        }

        getMenuInflater().inflate(R.menu.menu_list_land, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!getResources().getBoolean(R.bool.multi_pane)) {
            return super.onPrepareOptionsMenu(menu);
        }

        menu.findItem(R.id.menu_comment).setVisible(isItemOptionsMenuVisible() && mStoryMode);
        menu.findItem(R.id.menu_story).setVisible(isItemOptionsMenuVisible() && !mStoryMode);
        menu.findItem(R.id.menu_share).setVisible(isItemOptionsMenuVisible());
        if (mSelectedItem != null && mSelectedItem.isShareable()) {
            ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(
                    menu.findItem(R.id.menu_share));
            shareActionProvider.setShareIntent(AppUtils.makeShareIntent(
                    getString(R.string.share_format,
                            mSelectedItem.getDisplayedTitle(),
                            mSelectedItem.getUrl())));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_comment ||
                item.getItemId() == R.id.menu_story) {
            mStoryMode = !mStoryMode;
            mViewSwitcher.showNext();
            supportInvalidateOptionsMenu();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(ItemManager.WebItem item, View sharedElement) {
        if (getSelectedItem() != null && item.getId().equals(getSelectedItem().getId())) {
            return;
        }

        mSelectedItem = item;
        if (mIsMultiPane) {
            handleMultiPaneItemSelected(item);
            mStoryMode = !mDefaultOpenComments;
        } else {
            if (mExternalBrowser) {
                AppUtils.openWebUrlExternal(this, item.getUrl());
            } else {
                openItem(item, sharedElement);
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

    @Override
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

    private void handleMultiPaneItemSelected(ItemManager.WebItem item) {
        setTitle(item.getDisplayedTitle());
        findViewById(R.id.empty).setVisibility(View.GONE);
        mViewSwitcher.reset();
        mViewSwitcher.setAnimateFirstView(false);
        mViewSwitcher.setDisplayedChild(0);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(mDefaultOpenComments ? R.id.first : R.id.second,
                        ItemFragment.instantiate(this, item, null),
                        ItemFragment.class.getName())
                .replace(mDefaultOpenComments ? R.id.second : R.id.first,
                        WebFragment.instantiate(this, item),
                        WebFragment.class.getName())
                .commit();
    }

    private void openItem(ItemManager.WebItem item, View sharedElement) {
        final Intent intent = new Intent(this, ItemActivity.class);
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        intent.putExtra(ItemActivity.EXTRA_OPEN_ARTICLE, !mDefaultOpenComments);
        final ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this,
                        sharedElement, getString(R.string.transition_item_container));
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }
}

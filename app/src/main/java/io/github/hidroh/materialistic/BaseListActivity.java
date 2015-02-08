package io.github.hidroh.materialistic;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;

import io.github.hidroh.materialistic.data.ItemManager;

/**
 * List activity that renders alternative layouts for portrait/landscape
 */
public abstract class BaseListActivity extends BaseActivity implements ItemOpenListener {

    private static final String LIST_FRAGMENT_TAG = BaseListActivity.class.getName() + ".LIST_FRAGMENT_TAG";
    private boolean mIsMultiPane;
    private WebFragment mWebFragment;
    private ItemFragment mItemFragment;
    private boolean mIsStoryMode = true;
    private boolean mIsResumed;
    protected ItemManager.WebItem mSelectedItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        setTitle(getDefaultTitle());
        setContentView(R.layout.activity_list);
        onCreateView();
        beginFragmentTransaction()
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
            return false;
        }

        getMenuInflater().inflate(R.menu.menu_list_land, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!getResources().getBoolean(R.bool.multi_pane)) {
            return false;
        }

        menu.findItem(R.id.menu_comment).setVisible(mIsStoryMode && isItemOptionsMenuVisible());
        menu.findItem(R.id.menu_story).setVisible(!mIsStoryMode && isItemOptionsMenuVisible());
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
        if (item.getItemId() == R.id.menu_comment) {
            openComment(beginToggleFragmentTransaction());
            return true;
        }

        if (item.getItemId() == R.id.menu_story) {
            openStory(beginToggleFragmentTransaction());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemOpen(ItemManager.WebItem item) {
        FragmentTransaction transaction = beginSwapFragmentTransaction();
        removeFragment(transaction, WebFragment.class.getName());
        removeFragment(transaction, ItemFragment.class.getName());
        if (item == null) {
            setTitle(getDefaultTitle());
            transaction.commit();
            mSelectedItem = null;
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
        } else {
            if (mIsMultiPane) {
                setTitle(item.getDisplayedTitle());
            }
            mSelectedItem = item;
            findViewById(R.id.empty).setVisibility(View.GONE);
            mWebFragment = WebFragment.instantiate(this, item);
            Bundle args = new Bundle();
            args.putInt(ItemFragment.EXTRA_MARGIN,
                    getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin));
            mItemFragment = ItemFragment.instantiate(this, item, args);
            transaction
                    .add(R.id.content, mItemFragment, ItemFragment.class.getName())
                    .add(R.id.content, mWebFragment, WebFragment.class.getName());
            if (PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(getString(R.string.pref_item_click), false)) {
                openComment(transaction);
            } else {
                openStory(transaction);
            }
        }
        supportInvalidateOptionsMenu();
    }

    /**
     * Gets default title to be displayed in list-only layout
     * @return
     */
    protected abstract String getDefaultTitle();

    /**
     * Creates list fragment to host list data
     * @return
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
        if (mIsMultiPane != getResources().getBoolean(R.bool.multi_pane)) {
            mIsMultiPane = getResources().getBoolean(R.bool.multi_pane);
            final RelativeLayout.LayoutParams params;
            if (mIsMultiPane) {
                params = new RelativeLayout.LayoutParams(
                        getResources().getDimensionPixelSize(R.dimen.list_width),
                        RelativeLayout.LayoutParams.MATCH_PARENT);
            } else {
                setTitle(getDefaultTitle());
                params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
            }
            findViewById(android.R.id.list).setLayoutParams(params);
            onItemOpen(null);
        }
    }

    private void openStory(FragmentTransaction transaction) {
        transaction.hide(mItemFragment).show(mWebFragment).commit();
        mIsStoryMode = true;
        supportInvalidateOptionsMenu();
    }

    private void openComment(FragmentTransaction transaction) {
        transaction.hide(mWebFragment).show(mItemFragment).commit();
        mIsStoryMode = false;
        supportInvalidateOptionsMenu();
    }

    private FragmentTransaction beginSwapFragmentTransaction() {
        final FragmentTransaction transaction = beginFragmentTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        return transaction;
    }

    private FragmentTransaction beginToggleFragmentTransaction() {
        final FragmentTransaction transaction = beginFragmentTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right);
        return transaction;
    }
}

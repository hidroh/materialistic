package io.github.hidroh.materialistic;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;

public class ListActivity extends BaseActivity implements ListFragment.ItemOpenListener {

    private boolean mIsMultiPane;
    private WebFragment mWebFragment;
    private ItemFragment mItemFragment;
    private boolean mIsStoryMode = true;
    private RelativeLayout mContentContainer;
    private boolean mIsResumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        // delay setting title here to allow launcher to get app name
        setTitle(getString(R.string.title_activity_list));
        createView();
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
        menu.findItem(R.id.menu_comment).setVisible(mIsStoryMode && mWebFragment != null);
        menu.findItem(R.id.menu_story).setVisible(!mIsStoryMode && mItemFragment != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_comment) {
            openComment(beginFragmentTransaction());
            return true;
        }

        if (item.getItemId() == R.id.menu_story) {
            openStory(beginFragmentTransaction());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemOpen(ItemManager.Item story) {
        findViewById(R.id.empty).setVisibility(View.GONE);
        mWebFragment = WebFragment.instantiate(this, story);
        Bundle args = new Bundle();
        args.putInt(ItemFragment.EXTRA_MARGIN,
                getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin));
        mItemFragment = ItemFragment.instantiate(this, story, args);
        FragmentTransaction transaction = beginFragmentTransaction()
                .add(R.id.content, mItemFragment, ItemFragment.class.getName())
                .add(R.id.content, mWebFragment, WebFragment.class.getName());
        removeFragment(transaction, WebFragment.class.getName());
        removeFragment(transaction, ItemFragment.class.getName());
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_item_click), false)) {
            openComment(transaction);
        } else {
            openStory(transaction);
        }
    }

    private void handleConfigurationChanged() {
        if (!mIsResumed) {
            return;
        }

        if (mIsMultiPane != getResources().getBoolean(R.bool.multi_pane)) {
            createView();
        }
    }

    private void createView() {
        mIsMultiPane = getResources().getBoolean(R.bool.multi_pane);
        mContentView.removeAllViews();
        if (mIsMultiPane) {
            setContentView(R.layout.activity_list_land);
            mContentContainer = (RelativeLayout) findViewById(R.id.content);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.list,
                            ListFragment.instantiate(this, HackerNewsClient.getInstance(this)),
                            ListFragment.class.getName())
                    .commit();
        } else {
            mItemFragment = null;
            mWebFragment = null;
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame,
                            ListFragment.instantiate(this, HackerNewsClient.getInstance(this)),
                            ListFragment.class.getName())
                    .commit();
        }
        supportInvalidateOptionsMenu();
    }

    private void openStory(FragmentTransaction transaction) {
        transaction.hide(mItemFragment).show(mWebFragment).commit();
        mContentContainer.setBackgroundColor(getResources().getColor(android.R.color.white));
        mIsStoryMode = true;
        supportInvalidateOptionsMenu();
    }

    private void openComment(FragmentTransaction transaction) {
        transaction.hide(mWebFragment).show(mItemFragment).commit();
        mContentContainer.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        mIsStoryMode = false;
        supportInvalidateOptionsMenu();
    }

    private FragmentTransaction beginFragmentTransaction() {
        final FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        return transaction;
    }

    private void removeFragment(FragmentTransaction transaction, String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            transaction.remove(fragment);
        }
    }
}

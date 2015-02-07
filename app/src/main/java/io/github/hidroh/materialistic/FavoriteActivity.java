package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.RelativeLayout;

public class FavoriteActivity extends BaseActivity {

    private FavoriteFragment mFavoriteFragment;
    private boolean mIsResumed;
    private boolean mIsMultiPane;
    private RelativeLayout mContentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createView();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (mFavoriteFragment != null) {
            mFavoriteFragment.filter(intent.getStringExtra(SearchManager.QUERY));
        }
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
        if (mFavoriteFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(mFavoriteFragment).commit();
        }
        mFavoriteFragment = (FavoriteFragment) Fragment.instantiate(this, FavoriteFragment.class.getName());
        if (mIsMultiPane) {
            setContentView(R.layout.activity_list_land);
            mContentContainer = (RelativeLayout) findViewById(R.id.content);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.list,
                            mFavoriteFragment,
                            FavoriteFragment.class.getName())
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame,
                            mFavoriteFragment,
                            FavoriteFragment.class.getName())
                    .commit();
        }
        supportInvalidateOptionsMenu();
    }
}

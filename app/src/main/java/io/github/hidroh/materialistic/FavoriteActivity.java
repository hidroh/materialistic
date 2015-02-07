package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class FavoriteActivity extends BaseActivity {

    private FavoriteFragment mFavoriteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mFavoriteFragment = (FavoriteFragment) Fragment.instantiate(this, FavoriteFragment.class.getName());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.content_frame,
                            mFavoriteFragment,
                            FavoriteFragment.class.getName())
                    .commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (mFavoriteFragment != null) {
            mFavoriteFragment.filter(intent.getStringExtra(SearchManager.QUERY));
        }
    }
}

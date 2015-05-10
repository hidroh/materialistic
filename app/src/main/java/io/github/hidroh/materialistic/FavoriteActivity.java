package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;

public class FavoriteActivity extends BaseListActivity implements FavoriteFragment.DataChangedListener {

    private FavoriteFragment mFavoriteFragment;
    private View mEmptyView;
    private View mEmptySearchView;
    private boolean mIsEmpty;
    private String mFilter;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (mFavoriteFragment != null) {
            mFavoriteFragment.filter(intent.getStringExtra(SearchManager.QUERY));
        }
    }

    @Override
    protected void onCreateView() {
        mEmptyView = addContentView(R.layout.empty_favorite);
        mEmptyView.findViewById(R.id.header_card_view).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final View bookmark = mEmptyView.findViewById(R.id.bookmarked);
                bookmark.setVisibility(bookmark.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                return true;
            }
        });
        mEmptyView.setVisibility(View.INVISIBLE);
        mEmptySearchView = addContentView(R.layout.empty_favorite_search);
        mEmptySearchView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_favorite);
    }

    @Override
    protected Fragment instantiateListFragment() {
        mFavoriteFragment = FavoriteFragment.instantiate(this, mFilter);
        return mFavoriteFragment;
    }

    @Override
    protected boolean isItemOptionsMenuVisible() {
        return mSelectedItem != null && !mIsEmpty;
    }

    @Override
    protected boolean isSearchable() {
        return false;
    }

    @Override
    public void onDataChanged(boolean isEmpty, String filter) {
        mFilter = filter;
        mIsEmpty = isEmpty;
        if (isEmpty) {
            if (TextUtils.isEmpty(filter)) {
                mEmptySearchView.setVisibility(View.INVISIBLE);
                mEmptyView.setVisibility(View.VISIBLE);
                mEmptyView.bringToFront();
            } else {
                mEmptyView.setVisibility(View.INVISIBLE);
                mEmptySearchView.setVisibility(View.VISIBLE);
                mEmptySearchView.bringToFront();
            }
        } else {
            mEmptyView.setVisibility(View.INVISIBLE);
            mEmptySearchView.setVisibility(View.INVISIBLE);
        }

        supportInvalidateOptionsMenu();
    }
}

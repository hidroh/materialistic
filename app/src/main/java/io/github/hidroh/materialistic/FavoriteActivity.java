package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;

public class FavoriteActivity extends BaseListActivity implements FavoriteFragment.DataChangedListener {

    private static final String STATE_FILTER = "state:filter";
    private View mEmptyView;
    private View mEmptySearchView;
    private boolean mIsEmpty;
    private String mFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFilter = savedInstanceState.getString(STATE_FILTER);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        FavoriteFragment fragment = (FavoriteFragment) getSupportFragmentManager()
                .findFragmentByTag(LIST_FRAGMENT_TAG);
        if (fragment != null) {
            fragment.filter(intent.getStringExtra(SearchManager.QUERY));
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILTER, mFilter);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_favorite);
    }

    @Override
    protected Fragment instantiateListFragment() {
        Bundle args = new Bundle();
        args.putString(FavoriteFragment.EXTRA_FILTER, mFilter);
        return Fragment.instantiate(this, FavoriteFragment.class.getName(), args);
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

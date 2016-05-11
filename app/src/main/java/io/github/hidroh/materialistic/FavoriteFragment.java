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

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.Favorite;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.MaterialisticProvider;
import io.github.hidroh.materialistic.widget.FavoriteRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;

public class FavoriteFragment extends BaseListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>,
        FavoriteRecyclerViewAdapter.ActionModeDelegate {
    public static final String EXTRA_FILTER = FavoriteFragment.class.getName() + ".EXTRA_FILTER";
    private static final String STATE_FILTER = "state:filter";
    private static final String STATE_SEARCH_VIEW_EXPANDED = "state:searchViewExpanded";

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Favorite> favorites =
                    intent.getParcelableArrayListExtra(FavoriteManager.ACTION_GET_EXTRA_DATA);
            export(favorites);
        }
    };
    private final FavoriteRecyclerViewAdapter mAdapter = new FavoriteRecyclerViewAdapter(this);
    private ProgressDialog mProgressDialog;
    private ActionMode mActionMode;
    private String mFilter;
    private boolean mSearchViewExpanded;
    @Inject FavoriteManager mFavoriteManager;
    @Inject ActionViewResolver mActionViewResolver;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    private View mEmptySearchView;
    private View mEmptyView;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver,
                FavoriteManager.makeGetIntentFilter());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFilter = savedInstanceState.getString(STATE_FILTER);
            mSearchViewExpanded = savedInstanceState.getBoolean(STATE_SEARCH_VIEW_EXPANDED);
        } else {
            mFilter = getArguments().getString(EXTRA_FILTER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater(savedInstanceState)
                .inflate(R.layout.fragment_favorite, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mEmptySearchView = view.findViewById(R.id.empty_search);
        mEmptyView = view.findViewById(R.id.empty);
        mEmptyView.findViewById(R.id.header_card_view)
                .setOnLongClickListener(v -> {
                    View bookmark = mEmptyView.findViewById(R.id.bookmarked);
                    bookmark.setVisibility(bookmark.getVisibility() == View.VISIBLE ?
                                    View.INVISIBLE : View.VISIBLE);
                    return true;
                });
        mEmptyView.setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().restartLoader(FavoriteManager.LOADER, null, this);
    }

    @Override
    protected void createOptionsMenu(final Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);
        createSearchView(menu.findItem(R.id.menu_search));
        if (mAdapter.getItemCount() > 0) {
            inflater.inflate(R.menu.menu_favorite, menu);
            super.createOptionsMenu(menu, inflater);
        }
    }

    @Override
    protected void prepareOptionsMenu(Menu menu) {
        // allow clearing filter if empty, or filter if non-empty
        menu.findItem(R.id.menu_search).setVisible(!TextUtils.isEmpty(mFilter) ||
                mAdapter.getItemCount() > 0);
        if (mAdapter.getItemCount() > 0) {
            super.prepareOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clear) {
            clear();
            return true;
        }
        if (item.getItemId() == R.id.menu_email) {
            startExport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILTER, mFilter);
        outState.putBoolean(STATE_SEARCH_VIEW_EXPANDED, mSearchViewExpanded);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        mRecyclerView.setAdapter(null); // detach adapter
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (!TextUtils.isEmpty(mFilter)) {
            return new FavoriteManager.CursorLoader(getActivity(), mFilter);
        }
        return new FavoriteManager.CursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swapCursor(data == null ? null : new FavoriteManager.Cursor(data));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swapCursor(null);
    }

    /**
     * Filters list data by given query
     * @param query query used to filter data
     */
    public void filter(String query) {
        mSearchViewExpanded = false;
        mFilter = query;
        getLoaderManager().restartLoader(FavoriteManager.LOADER, null, this);
    }

    @Override
    protected ListRecyclerViewAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public boolean startActionMode(ActionMode.Callback callback) {
        if (mSearchViewExpanded) {
            return false;
        }
        if (mActionMode == null) {
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(callback);
        }
        return true;
    }

    @Override
    public boolean isInActionMode() {
        return mActionMode != null && !mSearchViewExpanded;
    }

    @Override
    public void stopActionMode() {
        mActionMode = null;
    }

    private void swapCursor(FavoriteManager.Cursor cursor) {
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(),
                    MaterialisticProvider.URI_FAVORITE);
        }
        mAdapter.setCursor(cursor);
        if (!isDetached()) {
            toggleEmptyView(mAdapter.getItemCount() == 0, mFilter);
            getActivity().supportInvalidateOptionsMenu();
        }
    }

    private void toggleEmptyView(boolean isEmpty, String filter) {
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
    }

    private void createSearchView(MenuItem menuSearch) {
        final SearchView searchView = (SearchView) mActionViewResolver.getActionView(menuSearch);
        searchView.setQueryHint(getString(R.string.hint_search_saved_stories));
        searchView.setSearchableInfo(((SearchManager) getActivity()
                .getSystemService(Context.SEARCH_SERVICE))
                .getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconified(!mSearchViewExpanded);
        searchView.setQuery(mFilter, false);
        searchView.setOnSearchClickListener(v -> {
            mSearchViewExpanded = true;
            v.requestFocus();
        });
        searchView.setOnCloseListener(() -> {
            // trigger a dummy empty search intent, as empty query does not get submitted
            searchView.setQuery(FavoriteActivity.EMPTY_QUERY, true);
            return false;
        });
    }

    private void clear() {
        mAlertDialogBuilder
                .init(getActivity())
                .setMessage(R.string.confirm_clear)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> mFavoriteManager.clear(getActivity(), mFilter))
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    private void startExport() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(getActivity(), null,
                    getString(R.string.preparing), true, true);
        } else {
            mProgressDialog.show();
        }
        mFavoriteManager.get(getActivity(), mFilter);
    }

    private void export(ArrayList<Favorite> favorites) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        final Intent intent = AppUtils.makeEmailIntent(
                getString(R.string.favorite_email_subject),
                makeEmailContent(favorites));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private String makeEmailContent(ArrayList<Favorite> favorites) {
        return TextUtils.join("\n\n", favorites);
    }
}

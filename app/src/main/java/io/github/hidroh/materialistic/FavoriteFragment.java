package io.github.hidroh.materialistic;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
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

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;

public class FavoriteFragment extends BaseListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_FILTER = FavoriteFragment.class.getName() + ".EXTRA_FILTER";
    private static final String STATE_FILTER = "state:filter";
    private FavoriteManager.Cursor mCursor;
    private final RecyclerViewAdapter mAdapter = new RecyclerViewAdapter();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (FavoriteManager.ACTION_GET.equals(intent.getAction())) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                final Intent emailIntent = AppUtils.makeEmailIntent(
                        getString(R.string.favorite_email_subject),
                        makeEmailContent(
                                (FavoriteManager.Favorite[]) intent.getParcelableArrayExtra(
                                        FavoriteManager.ACTION_GET_EXTRA_DATA)));
                if (emailIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(emailIntent);
                }
            } else if (FavoriteManager.ACTION_CLEAR.equals(intent.getAction())) {
                getLoaderManager().restartLoader(FavoriteManager.LOADER, null, FavoriteFragment.this);
            }
        }
    };
    private ProgressDialog mProgressDialog;
    private ActionMode mActionMode;
    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.menu_favorite_action, menu);
            mMenuTintDelegate.onOptionsMenuCreated(menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_clear) {
                mAlertDialogBuilder
                        .init(getActivity())
                        .setMessage(R.string.confirm_clear_selected)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!mIsResumed) {
                                    // TODO should dismiss dialog on orientation changed
                                    return;
                                }

                                mMultiPaneListener.clearSelection();
                                mFavoriteManager.remove(getActivity(), mSelected);
                                actionMode.finish();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mSelected.clear();
            mAdapter.notifyDataSetChanged();
            mActionMode = null;
        }
    };
    private Set<String> mSelected = new HashSet<>();
    private String mFilter;
    private boolean mSearchViewVisible;
    private boolean mIsResumed;
    private MultiPaneListener mMultiPaneListener;
    private DataChangedListener mDataChangedListener;
    @Inject FavoriteManager mFavoriteManager;
    @Inject ActionViewResolver mActionViewResolver;
    @Inject AlertDialogBuilder mAlertDialogBuilder;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        mMultiPaneListener = (MultiPaneListener) context;
        mDataChangedListener = (DataChangedListener) context;
        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver,
                FavoriteManager.makeGetIntentFilter());
        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver,
                FavoriteManager.makeClearIntentFilter());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFilter = savedInstanceState.getString(STATE_FILTER);
        } else {
            mFilter = getArguments().getString(EXTRA_FILTER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = getLayoutInflater(savedInstanceState)
                .inflate(R.layout.fragment_favorite, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (!mIsResumed) {
                    return;
                }

                mDataChangedListener.onDataChanged(mAdapter.getItemCount() == 0, mFilter);
            }
        });
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsResumed = true;
        getLoaderManager().restartLoader(FavoriteManager.LOADER, null, this);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_favorite, menu);
        final MenuItem menuSearch = menu.findItem(R.id.menu_search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) mActionViewResolver.getActionView(menuSearch);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchViewVisible = true;
                invalidateMenuItems(menu);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mSearchViewVisible = false;
                if (!TextUtils.isEmpty(mFilter)) {
                    mFilter = null;
                    getLoaderManager()
                            .restartLoader(FavoriteManager.LOADER, null, FavoriteFragment.this);
                }
                invalidateMenuItems(menu);
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        invalidateMenuItems(menu);
        menu.findItem(R.id.menu_search).setVisible(!TextUtils.isEmpty(mFilter) ||
                mAdapter.getItemCount() > 0);
        super.onPrepareOptionsMenu(menu);
    }

    private void invalidateMenuItems(Menu menu) {
        final boolean menuEnabled = !mSearchViewVisible && mAdapter.getItemCount() > 0;
        menu.findItem(R.id.menu_clear).setVisible(menuEnabled);
        menu.findItem(R.id.menu_email).setVisible(menuEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clear) {
            mAlertDialogBuilder
                    .init(getActivity())
                    .setMessage(R.string.confirm_clear)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mFavoriteManager.clear(getActivity(), mFilter);
                            mCursor = null;
                            mAdapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
            return true;
        }

        if (item.getItemId() == R.id.menu_email) {
            if (mProgressDialog == null) {
                mProgressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.preparing), true, true);
            } else {
                mProgressDialog.show();
            }
            mFavoriteManager.get(getActivity(), mFilter);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_FILTER, mFilter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != FavoriteManager.LOADER) {
            return null;
        }

        if (TextUtils.isEmpty(mFilter)) {
            ((DrawerActivity) getActivity()).getSupportActionBar().setSubtitle(null);
            return new FavoriteManager.CursorLoader(getActivity());
        } else {
            ((DrawerActivity) getActivity()).getSupportActionBar().setSubtitle(mFilter);
            return new FavoriteManager.CursorLoader(getActivity(), mFilter);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() != FavoriteManager.LOADER) {
            return;
        }

        if (data == null) {
            mCursor = null;
        } else {
            mCursor = new FavoriteManager.Cursor(data);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() != FavoriteManager.LOADER) {
            return;
        }

        mCursor = null;
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        mIsResumed = false;
        super.onPause();
    }

    @Override
    public void onDetach() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        mMultiPaneListener = null;
        mDataChangedListener = null;
        if (mActionMode != null) {
            mActionMode.finish();
        }
        super.onDetach();
    }

    /**
     * Filters list data by given query
     * @param query query used to filter data
     */
    public void filter(String query) {
        mFilter = query;
        mSearchViewVisible = false;
        getLoaderManager().restartLoader(FavoriteManager.LOADER, null, this);
    }

    @Override
    protected RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    private String makeEmailContent(FavoriteManager.Favorite[] favorites) {
        return TextUtils.join("\n\n", favorites);
    }

    private DrawerActivity getBaseActivity() {
        return (DrawerActivity) getActivity();
    }

    private class RecyclerViewAdapter extends ListRecyclerViewAdapter<ListRecyclerViewAdapter.ItemViewHolder, FavoriteManager.Favorite> {
        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ItemViewHolder(getLayoutInflater(null)
                    .inflate(R.layout.item_favorite, parent, false));
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        @Override
        protected void bindItem(final ItemViewHolder holder) {
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    FavoriteManager.Favorite favorite = getItem(holder.getAdapterPosition());
                    if (mActionMode == null && !mSearchViewVisible) {
                        mActionMode = getBaseActivity().startSupportActionMode(mActionModeCallback);
                        toggle(favorite.getId(), holder.getAdapterPosition());
                        if (mMultiPaneListener.getSelectedItem() != null) {
                            mSelected.add(mMultiPaneListener.getSelectedItem().getId());
                        }
                        return true;
                    }

                    return false;
                }
            });
        }

        @Override
        protected boolean isItemAvailable(FavoriteManager.Favorite item) {
            return item != null;
        }

        @Override
        protected void handleItemClick(FavoriteManager.Favorite item, ItemViewHolder holder) {
            if (mActionMode == null) {
                mSearchViewVisible = false;
                super.handleItemClick(item, holder);
            } else {
                toggle(item.getId(), holder.getLayoutPosition());
            }
        }

        @Override
        protected void onItemSelected(FavoriteManager.Favorite item, View itemView) {
            mMultiPaneListener.onItemSelected(item);
        }

        @Override
        protected FavoriteManager.Favorite getItem(int position) {
            if (mCursor == null) {
                return null;
            }

            if (!mCursor.moveToPosition(position)) {
                return null;
            }

            return mCursor.getFavorite();
        }

        @Override
        protected boolean isSelected(String itemId) {
            return mMultiPaneListener.getSelectedItem() != null &&
                    itemId.equals(mMultiPaneListener.getSelectedItem().getId()) ||
                    mSelected.contains(itemId);
        }

        @Override
        protected boolean shouldCompact() {
            return !mCardView;
        }

        private void toggle(String itemId, int position) {
            if (mSelected.contains(itemId)) {
                mSelected.remove(itemId);
            } else {
                mSelected.add(itemId);
            }
            notifyItemChanged(position);
        }
    }

    public interface DataChangedListener {
        void onDataChanged(boolean isEmpty, String filter);
    }
}

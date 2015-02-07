package io.github.hidroh.materialistic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import io.github.hidroh.materialistic.data.FavoriteManager;

public class FavoriteFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private FavoriteManager.Cursor mCursor;
    private RecyclerViewAdapter mAdapter;
    private BroadcastReceiver mBroadcastReceiver;
    private ProgressDialog mProgressDialog;
    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback;
    private Set<String> mSelected = new HashSet<>();
    private String mFilter;
    private SearchView mSearchView;
    private boolean mSearchViewVisible;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == null) {
                    return;
                }

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
        LocalBroadcastManager.getInstance(activity).registerReceiver(mBroadcastReceiver, FavoriteManager.makeGetIntentFilter());
        LocalBroadcastManager.getInstance(activity).registerReceiver(mBroadcastReceiver, FavoriteManager.makeClearIntentFilter());
        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                actionMode.getMenuInflater().inflate(R.menu.menu_favorite_action, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_clear) {
                    new AlertDialog.Builder(activity)
                            .setMessage(getString(R.string.confirm_clear_selected))
                            .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FavoriteManager.remove(activity, mSelected);
                                    actionMode.finish();
                                }
                            })
                            .setNegativeButton(getString(android.R.string.cancel), null)
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_favorite, container, false);
        mAdapter = new RecyclerViewAdapter();
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }

            @Override
            public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
                super.onItemsRemoved(recyclerView, positionStart, itemCount);
            }
        });
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        final View emptyView = getLayoutInflater(savedInstanceState).inflate(R.layout.empty_favorite, getBaseActivity().getContentView(), false);
        emptyView.findViewById(R.id.header_card_view).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final View bookmark = emptyView.findViewById(R.id.bookmarked);
                bookmark.setVisibility(bookmark.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                return true;
            }
        });
        emptyView.setVisibility(View.INVISIBLE);
        getBaseActivity().getContentView().addView(emptyView);
        final View emptySearchView = getLayoutInflater(savedInstanceState)
                .inflate(R.layout.empty_favorite_search, getBaseActivity().getContentView(), false);
        emptySearchView.setVisibility(View.INVISIBLE);
        getBaseActivity().getContentView().addView(emptySearchView);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mAdapter.getItemCount() == 0) {
                    recyclerView.setVisibility(View.INVISIBLE);
                    if (TextUtils.isEmpty(mFilter)) {
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        emptySearchView.setVisibility(View.VISIBLE);
                    }
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.INVISIBLE);
                    emptySearchView.setVisibility(View.INVISIBLE);
                }
                getActivity().supportInvalidateOptionsMenu();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(FavoriteManager.LOADER, null, this);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_favorite, menu);
        final MenuItem menuSearch = menu.findItem(R.id.menu_search);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menuSearch);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchViewVisible = true;
                invalidateMenuItems(menu);
            }
        });
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
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
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        invalidateMenuItems(menu);
        menu.findItem(R.id.menu_search).setVisible(!TextUtils.isEmpty(mFilter) ||
                mAdapter.getItemCount() > 0);
    }

    private void invalidateMenuItems(Menu menu) {
        final boolean menuEnabled = !mSearchViewVisible && mAdapter.getItemCount() > 0;
        menu.findItem(R.id.menu_clear).setVisible(menuEnabled);
        menu.findItem(R.id.menu_email).setVisible(menuEnabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clear) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.confirm_clear)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FavoriteManager.clear(getActivity(), mFilter);
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
            FavoriteManager.get(getActivity(), mFilter);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != FavoriteManager.LOADER) {
            return null;
        }

        if (TextUtils.isEmpty(mFilter)) {
            getActivity().setTitle(getString(R.string.title_activity_favorite));
            return new FavoriteManager.CursorLoader(getActivity());
        } else {
            getActivity().setTitle(getString(R.string.title_activity_favorite_search, mFilter));
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
            boolean any = data.moveToFirst();
            if (any) {

            } else {

            }
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
    public void onDetach() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;
        super.onDetach();
    }

    public void filter(String query) {
        mFilter = query;
        mSearchViewVisible = false;
        getLoaderManager().restartLoader(FavoriteManager.LOADER, null, this);
    }

    private String makeEmailContent(FavoriteManager.Favorite[] favorites) {
        return TextUtils.join("\n\n", favorites);
    }

    private void toggle(String itemId, int position) {
        if (mSelected.contains(itemId)) {
            mSelected.remove(itemId);
        } else {
            mSelected.add(itemId);
        }
        mAdapter.notifyItemChanged(position);
    }

    private BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<FavoriteViewHolder> {
        @Override
        public FavoriteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FavoriteViewHolder(getLayoutInflater(null)
                    .inflate(R.layout.activity_favorite_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final FavoriteViewHolder holder, final int position) {
            if (mCursor == null) {
                return;
            }

            if (!mCursor.moveToPosition(position)) {
                return;
            }

            final FavoriteManager.Favorite favorite = mCursor.getFavorite();
            holder.mPostedTextView.setText(favorite.getCreated(getActivity()));
            holder.mTitleTextView.setText(favorite.getDisplayedTitle());
            holder.mSourceTextView.setVisibility(TextUtils.isEmpty(favorite.getSource()) ?
                    View.GONE : View.VISIBLE);
            holder.mSourceTextView.setText(favorite.getSource());
            ((CardView) holder.itemView)
                    .setCardBackgroundColor(getResources().getColor(
                            mSelected.contains(favorite.getId()) ?
                                    R.color.colorPrimaryLight :
                                    R.color.cardview_light_background));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mActionMode == null) {
                        mSearchViewVisible = false;
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity())
                                .getBoolean(getString(R.string.pref_item_click), false)) {
                            openItem(favorite, holder);
                        } else {
                            AppUtils.openWebUrl(getActivity(), favorite);
                        }
                    } else {
                        toggle(favorite.getId(), position);
                    }
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mActionMode == null && !mSearchViewVisible) {
                        mActionMode = getBaseActivity().startSupportActionMode(mActionModeCallback);
                        toggle(favorite.getId(), position);
                        return true;
                    }

                    return false;
                }
            });
            holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openItem(favorite, holder);
                }
            });
        }

        private void openItem(FavoriteManager.Favorite favorite, FavoriteViewHolder holder) {
            final Intent intent = new Intent(getActivity(), ItemActivity.class);
            intent.putExtra(ItemActivity.EXTRA_ITEM_ID, favorite.getId());
            final ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(getActivity(),
                            holder.itemView, getString(R.string.transition_item_container));
            ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }
    }

    private class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private final TextView mPostedTextView;
        private final TextView mTitleTextView;
        private final ImageButton mCommentButton;
        private final TextView mSourceTextView;

        private FavoriteViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mTitleTextView = (TextView) itemView.findViewById(android.R.id.text1);
            mSourceTextView = (TextView) itemView.findViewById(R.id.source);
            mCommentButton = (ImageButton) itemView.findViewById(R.id.comment);
        }
    }
}

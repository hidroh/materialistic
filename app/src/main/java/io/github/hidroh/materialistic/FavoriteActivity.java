package io.github.hidroh.materialistic;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.hidroh.materialistic.data.FavoriteManager;

public class FavoriteActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private FavoriteManager.Cursor mCursor;
    private RecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        mAdapter = new RecyclerViewAdapter();
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public int getOrientation() {
                return LinearLayout.VERTICAL;
            }
        });
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportLoaderManager().restartLoader(FavoriteManager.LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_favorite, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_clear).setVisible(mAdapter.getItemCount() > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clear) {
            FavoriteManager.clear(this);
            mCursor = null;
            mAdapter.notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != FavoriteManager.LOADER) {
            return null;
        }

        return new FavoriteManager.CursorLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() != FavoriteManager.LOADER) {
            return;
        }

        if (data == null) {
            mCursor = null;
        } else {
            data.moveToFirst();
            mCursor = new FavoriteManager.Cursor(data);
        }
        mAdapter.notifyDataSetChanged();
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() != FavoriteManager.LOADER) {
            return;
        }

        mCursor = null;
        mAdapter.notifyDataSetChanged();
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<FavoriteViewHolder> {
        @Override
        public FavoriteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FavoriteViewHolder(getLayoutInflater()
                    .inflate(R.layout.activity_favorite_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final FavoriteViewHolder holder, int position) {
            if (mCursor == null) {
                return;
            }

            if (!mCursor.moveToPosition(position)) {
                return;
            }

            final FavoriteManager.Favorite favorite = mCursor.getFavorite();
            holder.mPostedTextView.setText(favorite.getCreated(FavoriteActivity.this));
            holder.mTitleTextView.setText(favorite.getTitle());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO consider opening in internal browser
                    AppUtils.openWebUrlExternal(FavoriteActivity.this, favorite.getUrl());
                }
            });
            holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent(FavoriteActivity.this, ItemActivity.class);
                    intent.putExtra(ItemActivity.EXTRA_ITEM_ID, favorite.getItemId());
                    final ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation(FavoriteActivity.this,
                                    holder.itemView, getString(R.string.transition_item_container));
                    ActivityCompat.startActivity(FavoriteActivity.this, intent, options.toBundle());
                }
            });
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }
    }

    private class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private final TextView mPostedTextView;
        private final TextView mTitleTextView;
        private final Button mCommentButton;

        private FavoriteViewHolder(View itemView) {
            super(itemView);
            mPostedTextView = (TextView) itemView.findViewById(R.id.posted);
            mTitleTextView = (TextView) itemView.findViewById(android.R.id.text1);
            mCommentButton = (Button) itemView.findViewById(R.id.comment);
            // TODO remember tinted drawable so we don't apply it again
            AppUtils.initTintedDrawable(getResources(), R.drawable.ic_comment_grey600_48dp,
                    R.color.colorAccent);
        }
    }
}

package io.github.hidroh.materialistic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;

public class WebActivity extends BaseItemActivity {

    public static final String EXTRA_ITEM = WebActivity.class.getName() + ".EXTRA_ITEM";
    private ItemManager.WebItem mItem;
    private boolean mIsFavorite;
    private int mFavoriteOnResId;
    private int mFavoriteOffResId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItem = getIntent().getParcelableExtra(EXTRA_ITEM);
        setTitle(mItem.getDisplayedTitle());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content,
                        WebFragment.instantiate(this, mItem),
                        WebFragment.class.getName())
                .commit();
        mFavoriteOnResId = AppUtils.getThemedResId(this, R.attr.themedMenuFavoriteOnDrawable);
        mFavoriteOffResId = AppUtils.getThemedResId(this, R.attr.themedMenuFavoriteOffDrawable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web, menu);
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(
                menu.findItem(R.id.menu_share));
        shareActionProvider.setShareIntent(AppUtils.makeShareIntent(
                getString(R.string.share_format,
                        mItem.getDisplayedTitle(),
                        mItem.getUrl())));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        if (!mItem.isShareable()) {
            menu.findItem(R.id.menu_share).setVisible(false);
        } else {
            FavoriteManager.check(this, mItem.getId(), new FavoriteManager.OperationCallbacks() {
                @Override
                public void onCheckComplete(boolean isFavorite) {
                    super.onCheckComplete(isFavorite);
                    final MenuItem menuFavorite = menu.findItem(R.id.menu_favorite);
                    menuFavorite.setVisible(true);
                    mIsFavorite = isFavorite;
                    toggleFavorite(menuFavorite);
                }
            });
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        supportInvalidateOptionsMenu();
    }

    private void toggleFavorite(MenuItem menuFavorite) {
        if (mIsFavorite) {
            menuFavorite.setIcon(mFavoriteOnResId);
            menuFavorite.setTitle(R.string.unsave_story);
        } else {
            menuFavorite.setIcon(mFavoriteOffResId);
            menuFavorite.setTitle(R.string.save_story);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_external) {
            AppUtils.openWebUrlExternal(this, mItem.getUrl());
            return true;
        }

        if (item.getItemId() == R.id.menu_comment) {
            final Intent intent = new Intent(this, ItemActivity.class);
            intent.putExtra(ItemActivity.EXTRA_ITEM, mItem);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.menu_favorite) {
            final int toastMessageResId;
            mIsFavorite = !mIsFavorite;
            if (mIsFavorite) {
                FavoriteManager.add(this, mItem);
                toastMessageResId = R.string.toast_saved;
            } else {
                FavoriteManager.remove(this, mItem.getId());
                toastMessageResId = R.string.toast_removed;
            }
            Toast.makeText(this, toastMessageResId, Toast.LENGTH_SHORT).show();
            toggleFavorite(item);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

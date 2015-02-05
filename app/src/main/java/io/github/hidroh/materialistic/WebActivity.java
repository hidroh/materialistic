package io.github.hidroh.materialistic;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;

public class WebActivity extends BaseItemActivity {

    public static final String EXTRA_ITEM = WebActivity.class.getName() + ".EXTRA_ITEM";
    private ItemManager.WebItem mItem;
    private boolean mIsFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        final WebView webView = (WebView) findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
        setWebViewSettings(webView.getSettings());
        mItem = getIntent().getParcelableExtra(EXTRA_ITEM);
        webView.loadUrl(mItem.getUrl());
        setTitle(mItem.getDisplayedTitle());
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
            menuFavorite.setIcon(R.drawable.ic_bookmark_white_24dp);
            menuFavorite.setTitle(R.string.unsave_story);
        } else {
            menuFavorite.setIcon(R.drawable.ic_bookmark_outline_white_24dp);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setWebViewSettings(WebSettings webSettings) {
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            webSettings.setDisplayZoomControls(false);
        }
    }

}

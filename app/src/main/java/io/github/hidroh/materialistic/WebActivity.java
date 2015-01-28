package io.github.hidroh.materialistic;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import io.github.hidroh.materialistic.data.HackerNewsClient;

public class WebActivity extends BaseItemActivity {

    public static final String EXTRA_ITEM = WebActivity.class.getName() + ".EXTRA_ITEM";
    private HackerNewsClient.WebItem mItem;

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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mItem.isShareable()) {
            menu.findItem(R.id.menu_share).setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
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

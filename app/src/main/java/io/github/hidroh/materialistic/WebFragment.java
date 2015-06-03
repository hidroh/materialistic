package io.github.hidroh.materialistic;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;

public class WebFragment extends BaseFragment {

    private static final String EXTRA_ITEM = WebFragment.class.getName() + ".EXTRA_ITEM";
    private ItemManager.WebItem mItem;
    private WebView mWebView;
    private boolean mIsHackerNewsUrl;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    @Inject AlertDialogBuilder mAlertDialogBuilder;

    public static WebFragment instantiate(Context context, ItemManager.WebItem item) {
        final WebFragment fragment = (WebFragment) Fragment.instantiate(context, WebFragment.class.getName());
        fragment.mItem = item;
        fragment.mIsHackerNewsUrl = AppUtils.isHackerNewsUrl(item);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mIsHackerNewsUrl) {
            return createLocalView(container, savedInstanceState);
        }

        final View view = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_web, container, false);
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress);
        mWebView = (WebView) view.findViewById(R.id.web_view);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    mWebView.setBackgroundColor(Color.WHITE);
                }
            }
        });
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (getActivity() == null) {
                    return;
                }

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    mAlertDialogBuilder
                            .setMessage(R.string.confirm_download)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show();
                }
            }
        });
        mWebView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_BACK) {
                    if (mWebView.canGoBack()) {
                        mWebView.goBack();
                        return true;
                    }
                }
                return false;
            }
        });
        setWebViewSettings(mWebView.getSettings());
        return view;
    }

    private View createLocalView(ViewGroup container, Bundle savedInstanceState) {
        final View view = getLayoutInflater(savedInstanceState)
                .inflate(R.layout.fragment_web_hn, container, false);
        ((TextView) view.findViewById(R.id.posted)).setText(mItem.getDisplayedTime(getActivity()));
        if (mItem.getType() == ItemManager.Item.COMMENT_TYPE) {
            view.findViewById(R.id.title).setVisibility(View.GONE);
        } else {
            ((TextView) view.findViewById(R.id.title)).setText(mItem.getDisplayedTitle());
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mIsHackerNewsUrl) {
            bindContent();
            return;
        }

        if (savedInstanceState != null) {
            ItemManager.WebItem savedItem = savedInstanceState.getParcelable(EXTRA_ITEM);
            if (savedItem != null) {
                mItem = savedItem;
            }
        }
        if (mItem != null) {
            mWebView.loadUrl(mItem.getUrl());
        }
    }

    private void bindContent() {
        if (mItem instanceof ItemManager.Item) {
            AppUtils.setTextWithLinks((TextView) getView().findViewById(R.id.text),
                    ((ItemManager.Item) mItem).getText());
        } else {
            mItemManager.getItem(mItem.getId(),
                    new ItemManager.ResponseListener<ItemManager.Item>() {
                        @Override
                        public void onResponse(ItemManager.Item response) {
                            AppUtils.setTextWithLinks((TextView) getView().findViewById(R.id.text),
                                    response.getText());
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // do nothing
                        }
                    });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ITEM, mItem);
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

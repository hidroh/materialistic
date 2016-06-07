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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.widget.AdBlockWebViewClient;
import io.github.hidroh.materialistic.widget.CacheableWebView;

public class WebFragment extends LazyLoadFragment implements Scrollable, Findable {

    private static final String EXTRA_ITEM = WebFragment.class.getName() + ".EXTRA_ITEM";
    private WebItem mItem;
    private WebView mWebView;
    private NestedScrollView mScrollView;
    private boolean mExternalRequired = false;
    private boolean mIsHackerNewsUrl;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    private VolumeNavigationDelegate.NestedScrollViewHelper mScrollableHelper;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();

    public static WebFragment instantiate(Context context, @NonNull WebItem item) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ITEM, item);
        return (WebFragment) Fragment.instantiate(context, WebFragment.class.getName(), args);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPreferenceObservable.subscribe(context, this::onPreferenceChanged,
                R.string.pref_readability_font,
                R.string.pref_readability_line_height,
                R.string.pref_readability_text_size);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mItem = getArguments().getParcelable(EXTRA_ITEM);
        } else {
            mItem = savedInstanceState.getParcelable(EXTRA_ITEM);
        }
        mIsHackerNewsUrl = AppUtils.isHackerNewsUrl(mItem);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = getLayoutInflater(savedInstanceState)
                .inflate(R.layout.fragment_web, container, false);
        mScrollView = (NestedScrollView) view.findViewById(R.id.nested_scroll_view);
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress);
        mWebView = (WebView) view.findViewById(R.id.web_view);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setWebViewClient(new AdBlockWebViewClient(Preferences.adBlockEnabled(getActivity())));
        mWebView.setWebChromeClient(new CacheableWebView.ArchiveClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
                if (!mIsHackerNewsUrl) {
                    mWebView.setBackgroundColor(Color.WHITE);
                }
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    mWebView.setVisibility(mExternalRequired ? View.GONE : View.VISIBLE);
                }
            }
        });
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (getActivity() == null) {
                return;
            }
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (intent.resolveActivity(getActivity().getPackageManager()) == null) {
                return;
            }
            mExternalRequired = true;
            mWebView.setVisibility(View.GONE);
            view.findViewById(R.id.empty).setVisibility(View.VISIBLE);
            view.findViewById(R.id.download_button).setOnClickListener(v -> startActivity(intent));
        });
        mWebView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_BACK) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                    return true;
                }
            }
            return false;
        });
        AppUtils.enableWebViewZoom(mWebView.getSettings());
        if (mIsHackerNewsUrl) {
            mWebView.getSettings().setLoadWithOverviewMode(false);
            mWebView.getSettings().setUseWideViewPort(false);
            mWebView.getSettings().setJavaScriptEnabled(false);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mScrollableHelper = new VolumeNavigationDelegate.NestedScrollViewHelper(mScrollView);
    }

    @Override
    protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_font_options, menu);
    }

    @Override
    protected void prepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_font_options).setVisible(mItem instanceof Item &&
                !TextUtils.isEmpty(((Item) mItem).getText()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_font_options) {
            showPreferences();
            return true;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ITEM, mItem);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPreferenceObservable.unsubscribe(getActivity());
    }

    @Override
    public void scrollToTop() {
        mScrollableHelper.scrollToTop();
    }

    @Override
    public boolean scrollToNext() {
        return mScrollableHelper.scrollToNext();
    }

    @Override
    public boolean scrollToPrevious() {
        return mScrollableHelper.scrollToPrevious();
    }

    @Override
    public WebView getWebView() {
        return mWebView;
    }

    @Override
    protected void load() {
        if (mIsHackerNewsUrl) {
            bindContent();
        } else {
            mWebView.loadUrl(mItem.getUrl());
        }
    }

    private void onItemLoaded(@NonNull Item response) {
        getActivity().supportInvalidateOptionsMenu();
        mItem = response;
        bindContent();
    }

    private void bindContent() {
        if (mItem instanceof Item) {
            mWebView.loadDataWithBaseURL(null, AppUtils.wrapHtml(getActivity(), ((Item) mItem).getText()),
                    "text/html", "UTF-8", null);
        } else {
            mItemManager.getItem(mItem.getId(), ItemManager.MODE_DEFAULT, new ItemResponseListener(this));
        }
    }

    private void showPreferences() {
        Bundle args = new Bundle();
        args.putInt(PopupSettingsFragment.EXTRA_XML_PREFERENCES, R.xml.preferences_readability);
        ((DialogFragment) Fragment.instantiate(getActivity(),
                PopupSettingsFragment.class.getName(), args))
                .show(getFragmentManager(), PopupSettingsFragment.class.getName());
    }

    private void onPreferenceChanged(int key, boolean contextChanged) {
        if (!contextChanged) {
            bindContent();
        }
    }

    private static class ItemResponseListener implements ResponseListener<Item> {
        private final WeakReference<WebFragment> mWebFragment;

        ItemResponseListener(WebFragment webFragment) {
            mWebFragment = new WeakReference<>(webFragment);
        }

        @Override
        public void onResponse(@Nullable Item response) {
            if (mWebFragment.get() != null && mWebFragment.get().isAttached() && response != null) {
                mWebFragment.get().onItemLoaded(response);
            }
        }

        @Override
        public void onError(String errorMessage) {
            // do nothing
        }
    }
}

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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.widget.AdBlockWebViewClient;
import io.github.hidroh.materialistic.widget.CacheableWebView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

abstract class BaseWebFragment extends LazyLoadFragment implements Scrollable {

    static final String ACTION_FULLSCREEN = BaseWebFragment.class.getName() + ".ACTION_FULLSCREEN";
    static final String EXTRA_FULLSCREEN = BaseWebFragment.class.getName() + ".EXTRA_FULLSCREEN";
    private static final String STATE_FULLSCREEN = "state:fullscreen";
    private static final String STATE_CONTENT = "state:content";
    private static final String STATE_URL = "state:url";
    private WebView mWebView;
    private NestedScrollView mScrollView;
    private boolean mExternalRequired = false;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    private VolumeNavigationDelegate.NestedScrollViewHelper mScrollableHelper;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setFullscreen(intent.getBooleanExtra(BaseWebFragment.EXTRA_FULLSCREEN, false));
        }
    };
    private ViewGroup mFullscreenView;
    private ViewGroup mScrollViewContent;
    private ViewSwitcher mViewSwitcher;
    private ImageButton mButtonRefresh;
    private ViewSwitcher mControls;
    private EditText mEditText;
    private View mButtonMore;
    private View mButtonPrevious;
    private View mButtonNext;
    private boolean mFullscreen;
    protected String mContent;
    private String mUrl;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPreferenceObservable.subscribe(context, this::onPreferenceChanged,
                R.string.pref_readability_font,
                R.string.pref_readability_line_height,
                R.string.pref_readability_text_size);
        LocalBroadcastManager.getInstance(context).registerReceiver(mReceiver,
                new IntentFilter(ACTION_FULLSCREEN));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN, false);
            mContent = savedInstanceState.getString(STATE_CONTENT);
            mUrl = savedInstanceState.getString(STATE_URL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = getLayoutInflater(savedInstanceState)
                .inflate(R.layout.fragment_web, container, false);
        mViewSwitcher = (ViewSwitcher) view.findViewById(R.id.view_switcher);
        mFullscreenView = (ViewGroup) view.findViewById(R.id.fullscreen);
        mScrollViewContent = (ViewGroup) view.findViewById(R.id.scroll_view_content);
        mScrollView = (NestedScrollView) view.findViewById(R.id.nested_scroll_view);
        mControls = (ViewSwitcher) view.findViewById(R.id.control_switcher);
        mWebView = (WebView) view.findViewById(R.id.web_view);
        mButtonRefresh = (ImageButton) view.findViewById(R.id.button_refresh);
        mButtonMore = view.findViewById(R.id.button_more);
        mButtonPrevious = view.findViewById(R.id.button_previous);
        mButtonNext = view.findViewById(R.id.button_next);
        mEditText = (EditText) view.findViewById(R.id.edittext);
        setUpWebControls(view);
        setUpWebView(view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        mScrollableHelper = new VolumeNavigationDelegate.NestedScrollViewHelper(mScrollView);
        if (mFullscreen) {
            setFullscreen(true);
        }
    }

    @Override
    protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_font_options, menu);
    }

    @Override
    protected void prepareOptionsMenu(Menu menu) {
        boolean fontOptionsVisible = !TextUtils.isEmpty(mContent);
        menu.findItem(R.id.menu_font_options).setVisible(fontOptionsVisible);
        mButtonMore.setVisibility(fontOptionsVisible ? VISIBLE : GONE);
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
        outState.putBoolean(STATE_FULLSCREEN, mFullscreen);
        outState.putString(STATE_CONTENT, mContent);
        outState.putString(STATE_URL, mUrl);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPreferenceObservable.unsubscribe(getActivity());
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    }

    @Override
    public void scrollToTop() {
        if (mFullscreen) {
            mWebView.pageUp(true);
        } else {
            mScrollableHelper.scrollToTop();
        }
    }

    @Override
    public boolean scrollToNext() {
        if (mFullscreen) {
            mWebView.pageDown(false);
            return true;
        } else {
            return mScrollableHelper.scrollToNext();
        }
    }

    @Override
    public boolean scrollToPrevious() {
        if (mFullscreen) {
            mWebView.pageUp(false);
            return true;
        } else {
            return mScrollableHelper.scrollToPrevious();
        }
    }

    void showEmptyView() {
        // override to show empty view
    }

    final void loadUrl(String url) {
        mUrl = url;
        setWebSettings(true);
        mWebView.loadUrl(url);
    }

    final void loadContent(String content) {
        mContent = content;
        getActivity().supportInvalidateOptionsMenu();
        if (!TextUtils.isEmpty(content)) {
            setWebSettings(false);
            mWebView.loadDataWithBaseURL(null, AppUtils.wrapHtml(getActivity(), content),
                    "text/html", "UTF-8", null);
        } else {
            showEmptyView();
        }
    }

    private void setUpWebControls(View view) {
        view.findViewById(R.id.button_back).setOnClickListener(v -> mWebView.goBack());
        view.findViewById(R.id.button_forward).setOnClickListener(v -> mWebView.goForward());
        view.findViewById(R.id.button_zoom_in).setOnClickListener(v -> mWebView.zoomIn());
        view.findViewById(R.id.button_zoom_out).setOnClickListener(v -> mWebView.zoomOut());
        view.findViewById(R.id.button_clear).setOnClickListener(v -> {
            reset();
            mControls.showNext();
        });
        view.findViewById(R.id.button_find).setOnClickListener(v -> {
            mEditText.requestFocus();
            toggleSoftKeyboard(true);
            mControls.showNext();
        });
        mButtonRefresh.setOnClickListener(v -> {
            if (mWebView.getProgress() == 100) {
                mWebView.loadUrl("about:blank");
                load();
            } else {
                mWebView.stopLoading();
            }
        });
        view.findViewById(R.id.button_exit).setOnClickListener(v -> {
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                    new Intent(BaseWebFragment.ACTION_FULLSCREEN)
                            .putExtra(EXTRA_FULLSCREEN, false));
        });
        mButtonPrevious.setOnClickListener(v -> mWebView.findNext(false));
        mButtonNext.setOnClickListener(v -> mWebView.findNext(true));
        mButtonMore.setOnClickListener(v -> showPreferences());
        mEditText.setOnEditorActionListener((v, actionId, event) -> { findInPage(); return true; });
    }

    private void setUpWebView(View view) {
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setWebViewClient(new AdBlockWebViewClient(Preferences.adBlockEnabled(getActivity())));
        mWebView.setWebChromeClient(new CacheableWebView.ArchiveClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setVisibility(VISIBLE);
                progressBar.setProgress(newProgress);
                if (!TextUtils.isEmpty(mUrl)) {
                    mWebView.setBackgroundColor(Color.WHITE);
                }
                if (newProgress == 100) {
                    progressBar.setVisibility(GONE);
                    mWebView.setVisibility(mExternalRequired ? GONE : VISIBLE);
                }
                mButtonRefresh.setImageResource(newProgress == 100 ?
                        R.drawable.ic_refresh_white_24dp : R.drawable.ic_clear_white_24dp);
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
            mWebView.setVisibility(GONE);
            view.findViewById(R.id.empty).setVisibility(VISIBLE);
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
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setWebSettings(boolean isRemote) {
        mWebView.getSettings().setLoadWithOverviewMode(isRemote);
        mWebView.getSettings().setUseWideViewPort(isRemote);
        mWebView.getSettings().setJavaScriptEnabled(isRemote);
    }

    private void setFullscreen(boolean isFullscreen) {
        if (!getUserVisibleHint()) {
            return;
        }
        mFullscreen = isFullscreen;
        if (isFullscreen) {
            mScrollView.removeView(mScrollViewContent);
            mFullscreenView.addView(mScrollViewContent);
        } else {
            reset();
            mFullscreenView.removeView(mScrollViewContent);
            mScrollView.addView(mScrollViewContent);
        }
        mControls.setVisibility(isFullscreen ? VISIBLE : View.GONE);
        mViewSwitcher.setDisplayedChild(isFullscreen ? 1 : 0);
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
            load();
        }
    }

    private void reset() {
        mEditText.setText(null);
        toggleSoftKeyboard(false);
        mWebView.clearMatches();
    }

    private void findInPage() {
        String query = mEditText.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mWebView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
                if (isDoneCounting) {
                    handleFindResults(numberOfMatches);
                }
            });
            mWebView.findAllAsync(query);
        } else {
            //noinspection deprecation
            handleFindResults(mWebView.findAll(query));
        }
    }

    private void handleFindResults(int numberOfMatches) {
        mButtonPrevious.setVisibility(numberOfMatches > 0 ? VISIBLE : GONE);
        mButtonNext.setVisibility(numberOfMatches > 0 ? VISIBLE : GONE);
        if (numberOfMatches == 0) {
            Toast.makeText(getContext(), R.string.no_matches, Toast.LENGTH_SHORT).show();
        } else {
            toggleSoftKeyboard(false);
        }
    }

    private void toggleSoftKeyboard(boolean visible) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (visible) {
            imm.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);
        } else {
            imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        }
    }
}

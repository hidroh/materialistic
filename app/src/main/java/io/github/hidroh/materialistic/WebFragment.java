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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.widget.AdBlockWebViewClient;
import io.github.hidroh.materialistic.widget.CacheableWebView;
import io.github.hidroh.materialistic.widget.PopupMenu;
import io.github.hidroh.materialistic.widget.WebView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class WebFragment extends LazyLoadFragment
        implements Scrollable, KeyDelegate.BackInterceptor {
    public static final String EXTRA_ITEM = WebFragment.class.getName() +".EXTRA_ITEM";
    private static final String STATE_EMPTY = "state:empty";
    private static final String STATE_READABILITY = "state:readability";
    public static final String EXTRA_RETAIN_INSTANCE = WebFragment.class.getName() + ".EXTRA_RETAIN_INSTANCE";
    static final String ACTION_FULLSCREEN = WebFragment.class.getName() + ".ACTION_FULLSCREEN";
    static final String EXTRA_FULLSCREEN = WebFragment.class.getName() + ".EXTRA_FULLSCREEN";
    private static final String STATE_FULLSCREEN = "state:fullscreen";
    private static final String STATE_CONTENT = "state:content";
    private static final int DEFAULT_PROGRESS = 20;
    @Synthetic WebView mWebView;
    private NestedScrollView mScrollView;
    @Synthetic boolean mExternalRequired = false;
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    @Inject PopupMenu mPopupMenu;
    private KeyDelegate.NestedScrollViewHelper mScrollableHelper;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setFullscreen(intent.getBooleanExtra(WebFragment.EXTRA_FULLSCREEN, false));
        }
    };
    private ViewGroup mFullscreenView;
    private ViewGroup mScrollViewContent;
    @Synthetic ImageButton mButtonRefresh;
    private ViewSwitcher mControls;
    private EditText mEditText;
    private View mButtonMore;
    private View mButtonNext;
    protected ProgressBar mProgressBar;
    private boolean mFullscreen;
    protected String mContent;
    private AppUtils.SystemUiHelper mSystemUiHelper;
    private View mFragmentView;
    @Inject ReadabilityClient mReadabilityClient;
    private WebItem mItem;
    private boolean mIsHackerNewsUrl, mEmpty, mReadability;

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
        setRetainInstance(getArguments().getBoolean(EXTRA_RETAIN_INSTANCE, false));
        if (savedInstanceState != null) {
            mFullscreen = savedInstanceState.getBoolean(STATE_FULLSCREEN, false);
            mContent = savedInstanceState.getString(STATE_CONTENT);
            mEmpty = savedInstanceState.getBoolean(STATE_EMPTY, false);
            mReadability = savedInstanceState.getBoolean(STATE_READABILITY, false);
            mItem = savedInstanceState.getParcelable(EXTRA_ITEM);
        } else {
            mReadability = Preferences.getDefaultStoryView(getActivity()) ==
                    Preferences.StoryViewMode.Readability;
            mItem = getArguments().getParcelable(EXTRA_ITEM);
        }
        mIsHackerNewsUrl = AppUtils.isHackerNewsUrl(mItem);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (isNewInstance()) {
            mFragmentView = inflater.inflate(R.layout.fragment_web, container, false);
            mFullscreenView = (ViewGroup) mFragmentView.findViewById(R.id.fullscreen);
            mScrollViewContent = (ViewGroup) mFragmentView.findViewById(R.id.scroll_view_content);
            mScrollView = (NestedScrollView) mFragmentView.findViewById(R.id.nested_scroll_view);
            mControls = (ViewSwitcher) mFragmentView.findViewById(R.id.control_switcher);
            mWebView = (WebView) mFragmentView.findViewById(R.id.web_view);
            mButtonRefresh = (ImageButton) mFragmentView.findViewById(R.id.button_refresh);
            mButtonMore = mFragmentView.findViewById(R.id.button_more);
            mButtonNext = mFragmentView.findViewById(R.id.button_next);
            mButtonNext.setEnabled(false);
            mEditText = (EditText) mFragmentView.findViewById(R.id.edittext);
            setUpWebControls(mFragmentView);
            setUpWebView(mFragmentView);
        }
        return mFragmentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        if (isNewInstance()) {
            mScrollableHelper = new KeyDelegate.NestedScrollViewHelper(mScrollView);
            mSystemUiHelper = new AppUtils.SystemUiHelper(getActivity().getWindow());
            mSystemUiHelper.setEnabled(!getResources().getBoolean(R.bool.multi_pane));
            if (mFullscreen) {
                setFullscreen(true);
            }
        }
    }

    @Override
    protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_article, menu);
    }

    @Override
    protected void prepareOptionsMenu(Menu menu) {
        MenuItem menuReadability = menu.findItem(R.id.menu_readability);
        menuReadability.setVisible(modeToggleEnabled());
        mMenuTintDelegate.setIcon(menuReadability, mReadability ?
                R.drawable.ic_web_black_24dp : R.drawable.ic_chrome_reader_mode_black_24dp);
        menuReadability.setTitle(mReadability ? R.string.article : R.string.readability);
        menu.findItem(R.id.menu_font_options).setVisible(fontEnabled());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_font_options) {
            showPreferences();
            return true;
        }
        if (item.getItemId() == R.id.menu_readability) {
            mReadability = !mReadability;
            load();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mWebView.onResume();
        }
        mWebView.resumeTimers();
    }

    @Override
    public void onStop() {
        super.onStop();
        pauseWebView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_FULLSCREEN, mFullscreen);
        outState.putString(STATE_CONTENT, mContent);
        outState.putParcelable(EXTRA_ITEM, mItem);
        outState.putBoolean(STATE_EMPTY, mEmpty);
        outState.putBoolean(STATE_READABILITY, mReadability);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
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

    @Override
    public boolean onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }

    @Override
    protected void load() {
        mWebView.setVisibility(View.INVISIBLE);
        if (mIsHackerNewsUrl) {
            bindContent();
        } else if (mReadability && !mEmpty) {
            if (TextUtils.isEmpty(mContent)) {
                parse();
            } else {
                loadContent();
            }
        } else {
            loadUrl();
        }
    }

    private void loadUrl() {
        setWebSettings(true);
        mWebView.reloadUrl(mItem.getUrl());
    }

    @Synthetic
    void loadContent() {
        setWebSettings(false);
        mWebView.reloadHtml(AppUtils.wrapHtml(getActivity(), mContent));
    }

    private void parse() {
        mProgressBar.setProgress(DEFAULT_PROGRESS);
        mReadabilityClient.parse(mItem.getId(), mItem.getUrl(), new ReadabilityCallback(this));
    }

    private void bindContent() {
        if (mItem instanceof Item) {
            mContent = ((Item) mItem).getText();
            loadContent();
        } else {
            mItemManager.getItem(mItem.getId(), ItemManager.MODE_DEFAULT, new ItemResponseListener(this));
        }
    }

    private void pauseWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mWebView.onPause();
        }
        mWebView.pauseTimers();
    }

    private boolean fontEnabled() {
        return mReadability && !mEmpty && !TextUtils.isEmpty(mContent);
    }

    private boolean modeToggleEnabled() {
        return !mIsHackerNewsUrl && !mWebView.canGoBack();
    }

    private void setUpWebControls(View view) {
        view.findViewById(R.id.toolbar_web).setOnClickListener(v -> scrollToTop());
        view.findViewById(R.id.button_back).setOnClickListener(v -> mWebView.goBack());
        view.findViewById(R.id.button_forward).setOnClickListener(v -> mWebView.goForward());
        view.findViewById(R.id.button_clear).setOnClickListener(v -> {
            mSystemUiHelper.setFullscreen(true);
            reset();
            mControls.showNext();
        });
        view.findViewById(R.id.button_find).setOnClickListener(v -> {
            mEditText.requestFocus();
            toggleSoftKeyboard(true);
            mControls.showNext();
        });
        mButtonRefresh.setOnClickListener(v -> {
            if (mWebView.getProgress() < 100) {
                mWebView.stopLoading();
            } else {
                mWebView.reload();
            }
        });
        view.findViewById(R.id.button_exit).setOnClickListener(v ->
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                        new Intent(WebFragment.ACTION_FULLSCREEN)
                                .putExtra(EXTRA_FULLSCREEN, false)));
        mButtonNext.setOnClickListener(v -> mWebView.findNext(true));
        mButtonMore.setOnClickListener(v ->
                mPopupMenu.create(getActivity(), mButtonMore, Gravity.NO_GRAVITY)
                        .inflate(R.menu.menu_web)
                        .setOnMenuItemClickListener(item -> {
                            if (item.getItemId() == R.id.menu_font_options) {
                                showPreferences();
                                return true;
                            }
                            if (item.getItemId() == R.id.menu_zoom_in) {
                                mWebView.zoomIn();
                                return true;
                            }
                            if (item.getItemId() == R.id.menu_zoom_out) {
                                mWebView.zoomOut();
                                return true;
                            }
                            return false;
                        })
                        .setMenuItemVisible(R.id.menu_font_options, fontEnabled())
                        .show());
        mEditText.setOnEditorActionListener((v, actionId, event) -> { findInPage(); return true; });
    }

    private void setUpWebView(View view) {
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.setWebViewClient(new AdBlockWebViewClient(Preferences.adBlockEnabled(getActivity())) {
            @Override
            public void onPageStarted(android.webkit.WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (getActivity() != null) {
                    getActivity().supportInvalidateOptionsMenu();
                }
            }

            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                if (getActivity() != null) {
                    getActivity().supportInvalidateOptionsMenu();
                }
            }
        });
        mWebView.setWebChromeClient(new CacheableWebView.ArchiveClient() {
            @Override
            public void onProgressChanged(android.webkit.WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                mProgressBar.setProgress(newProgress);
                mProgressBar.setVisibility(newProgress == 100 ? GONE : VISIBLE);
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
        AppUtils.toggleWebViewZoom(mWebView.getSettings(), false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setWebSettings(boolean isRemote) {
        mReadability = !isRemote;
        mWebView.setBackgroundColor(isRemote ? Color.WHITE : Color.TRANSPARENT);
        mWebView.getSettings().setLoadWithOverviewMode(isRemote);
        mWebView.getSettings().setUseWideViewPort(isRemote);
        mWebView.getSettings().setJavaScriptEnabled(true);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Synthetic
    void setFullscreen(boolean isFullscreen) {
        if (getView() == null) {
            return;
        }
        mFullscreen = isFullscreen;
        mControls.setVisibility(isFullscreen ? VISIBLE : View.GONE);
        AppUtils.toggleWebViewZoom(mWebView.getSettings(), isFullscreen);
        ViewGroup.LayoutParams params = mWebView.getLayoutParams();
        if (isFullscreen) {
            mScrollView.removeView(mScrollViewContent);
            mWebView.scrollTo(mScrollView.getScrollX(), mScrollView.getScrollY());
            mFullscreenView.addView(mScrollViewContent);
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            reset();
            mFullscreenView.removeView(mScrollViewContent);
            mScrollView.addView(mScrollViewContent);
            mScrollView.post(() -> mScrollView.scrollTo(mWebView.getScrollX(), mWebView.getScrollY()));
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        mWebView.setLayoutParams(params);
    }

    private void showPreferences() {
        Bundle args = new Bundle();
        args.putInt(PopupSettingsFragment.EXTRA_TITLE, R.string.font_options);
        args.putIntArray(PopupSettingsFragment.EXTRA_XML_PREFERENCES,
                new int[]{R.xml.preferences_readability});
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
        mButtonNext.setEnabled(false);
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
        mButtonNext.setEnabled(numberOfMatches > 0);
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

    @Synthetic
    void onParsed(String content) {
        if (isAttached()) {
            mContent = content;
            if (!TextUtils.isEmpty(mContent)) {
                loadContent();
            } else {
                mEmpty = true;
                if (mReadability) {
                    Toast.makeText(getActivity(), R.string.readability_failed, Toast.LENGTH_SHORT).show();
                }
                loadUrl();
            }
        }
    }

    @Synthetic
    void onItemLoaded(@NonNull Item response) {
        getActivity().supportInvalidateOptionsMenu();
        mItem = response;
        bindContent();
    }

    static class ReadabilityCallback implements ReadabilityClient.Callback {
        private final WeakReference<WebFragment> mReadabilityFragment;

        @Synthetic
        ReadabilityCallback(WebFragment webFragment) {
            mReadabilityFragment = new WeakReference<>(webFragment);
        }

        @Override
        public void onResponse(String content) {
            if (mReadabilityFragment.get() != null && mReadabilityFragment.get().isAttached()) {
                mReadabilityFragment.get().onParsed(content);
            }
        }
    }

    static class ItemResponseListener implements ResponseListener<Item> {
        private final WeakReference<WebFragment> mFragment;

        @Synthetic
        ItemResponseListener(WebFragment webFragment) {
            mFragment = new WeakReference<>(webFragment);
        }

        @Override
        public void onResponse(@Nullable Item response) {
            if (mFragment.get() != null && mFragment.get().isAttached() && response != null) {
                mFragment.get().onItemLoaded(response);
            }
        }

        @Override
        public void onError(String errorMessage) {
            // do nothing
        }
    }
}

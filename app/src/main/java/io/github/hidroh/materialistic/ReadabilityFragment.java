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
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.text.TextUtils;
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

import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.WebItem;

public class ReadabilityFragment extends LazyLoadFragment implements Scrollable, Findable {
    public static final String EXTRA_ITEM = ReadabilityFragment.class.getName() +".EXTRA_ITEM";
    private static final String STATE_CONTENT = "state:content";
    private NestedScrollView mScrollView;
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private View mEmptyView;
    @Inject ReadabilityClient mReadabilityClient;
    private VolumeNavigationDelegate.NestedScrollViewHelper mScrollableHelper;
    private String mContent;
    private boolean mAttached;
    private final Preferences.Observable mPreferenceObservable = new Preferences.Observable();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAttached = true;
        mPreferenceObservable.subscribe(context, this::onPreferenceChanged,
                R.string.pref_readability_font,
                R.string.pref_readability_line_height,
                R.string.pref_readability_text_size);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mContent = savedInstanceState.getString(STATE_CONTENT);
        }
    }

    @Override
    protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_font_options, menu);
    }

    @Override
    protected void prepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_font_options).setVisible(!TextUtils.isEmpty(mContent));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_font_options) {
            showPreferences();
            return true;
        }
        return true;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_readability, container, false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
        mScrollView = (NestedScrollView) view.findViewById(R.id.nested_scroll_view);
        mWebView = (WebView) view.findViewById(R.id.content);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setBackgroundColor(ContextCompat.getColor(getActivity(),
                AppUtils.getThemedResId(getActivity(), R.attr.colorCardBackground)));
        mEmptyView = view.findViewById(R.id.empty);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mScrollableHelper = new VolumeNavigationDelegate.NestedScrollViewHelper(mScrollView);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CONTENT, mContent);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAttached = false;
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
        if (TextUtils.isEmpty(mContent)) {
            parse();
        } else {
            bind();
        }
    }

    private void parse() {
        WebItem item = getArguments().getParcelable(EXTRA_ITEM);
        if (item == null) {
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        mReadabilityClient.parse(item.getId(), item.getUrl(), new ReadabilityCallback(this));
    }

    private void onParsed(String content) {
        mContent = content;
        bind();
    }

    private void bind() {
        if (!mAttached) {
            return;
        }
        mProgressBar.setVisibility(View.GONE);
        getActivity().supportInvalidateOptionsMenu();
        if (!TextUtils.isEmpty(mContent)) {
            render();
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    private void render() {
        mWebView.loadDataWithBaseURL(null, AppUtils.wrapHtml(getActivity(), mContent), "text/html", "UTF-8", null);
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
            render();
        }
    }

    private static class ReadabilityCallback implements ReadabilityClient.Callback {
        private final WeakReference<ReadabilityFragment> mReadabilityFragment;

        ReadabilityCallback(ReadabilityFragment readabilityFragment) {
            mReadabilityFragment = new WeakReference<>(readabilityFragment);
        }

        @Override
        public void onResponse(String content) {
            if (mReadabilityFragment.get() != null && mReadabilityFragment.get().isAttached()) {
                mReadabilityFragment.get().onParsed(content);
            }
        }
    }
}

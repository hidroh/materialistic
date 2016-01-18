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
import android.support.annotation.AttrRes;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.WebItem;

public class ReadabilityFragment extends LazyLoadFragment implements Scrollable {
    public static final String EXTRA_ITEM = ReadabilityFragment.class.getName() +".EXTRA_ITEM";
    private static final String STATE_CONTENT = "state:content";
    private static final String STATE_TEXT_SIZE = "state:textSize";
    private static final String STATE_TYPEFACE_NAME = "state:typefaceName";
    private static final String FORMAT_HTML_COLOR = "%06X";
    private NestedScrollView mScrollView;
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private View mEmptyView;
    @Inject ReadabilityClient mReadabilityClient;
    private String mContent;
    private float mTextSize;
    private String[] mTextSizeOptionValues;
    private String mTypefaceName;
    private String[] mFontOptionValues;
    private boolean mAttached;
    private String mTextColor;
    private String mTextLinkColor;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAttached = true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mTextSize = savedInstanceState.getFloat(STATE_TEXT_SIZE);
            mContent = savedInstanceState.getString(STATE_CONTENT);
            mTypefaceName = savedInstanceState.getString(STATE_TYPEFACE_NAME);
        } else {
            mTextSize = toHtmlPx(Preferences.Theme.resolvePreferredReadabilityTextSize(getActivity()));
            mTypefaceName = Preferences.Theme.getReadabilityTypeface(getActivity());
        }
        mTextColor = toHtmlColor(android.R.attr.textColorPrimary);
        mTextLinkColor = toHtmlColor(android.R.attr.textColorLink);
    }

    @Override
    protected void createOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_font_options, menu);
        mTextSizeOptionValues = getResources().getStringArray(R.array.pref_text_size_values);
        mFontOptionValues = getResources().getStringArray(R.array.font_values);
        SubMenu subMenu = menu.findItem(R.id.menu_font_size).getSubMenu();
        String[] options = getResources().getStringArray(R.array.text_size_options);
        String initialTextSize = Preferences.Theme.getPreferredReadabilityTextSize(getActivity());
        for (int i = 0; i < options.length; i++) {
            MenuItem item = subMenu.add(R.id.menu_font_size_group, Menu.NONE, i, options[i]);
            item.setChecked(TextUtils.equals(initialTextSize, mTextSizeOptionValues[i]));
        }
        subMenu.setGroupCheckable(R.id.menu_font_size_group, true, true);
        subMenu = menu.findItem(R.id.menu_font).getSubMenu();
        options = getResources().getStringArray(R.array.font_options);
        String initialTypeface = Preferences.Theme.getReadabilityTypeface(getActivity());
        for (int i = 0; i < options.length; i++) {
            MenuItem item = subMenu.add(R.id.menu_font_group, Menu.NONE, i, options[i]);
            item.setChecked(TextUtils.equals(initialTypeface, mFontOptionValues[i]));
        }
        subMenu.setGroupCheckable(R.id.menu_font_group, true, true);
    }

    @Override
    protected void prepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_font_options).setVisible(!TextUtils.isEmpty(mContent));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_font_size) {
            return true;
        }
        if (item.getGroupId() == R.id.menu_font_size_group) {
            item.setChecked(true);
            String choice = mTextSizeOptionValues[item.getOrder()];
            mTextSize = toHtmlPx(Preferences.Theme.resolveTextSize(choice));
            Preferences.Theme.savePreferredReadabilityTextSize(getActivity(), choice);
            render();
        } else if (item.getGroupId() == R.id.menu_font_group) {
            item.setChecked(true);
            mTypefaceName = mFontOptionValues[item.getOrder()];
            Preferences.Theme.savePreferredReadabilityTypeface(getActivity(), mTypefaceName);
            render();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putFloat(STATE_TEXT_SIZE, mTextSize);
        outState.putString(STATE_CONTENT, mContent);
        outState.putString(STATE_TYPEFACE_NAME, mTypefaceName);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAttached = false;
    }

    @Override
    public void scrollToTop() {
        mScrollView.smoothScrollTo(0, 0);
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
        mWebView.loadDataWithBaseURL(null, wrap(mContent), "text/html", "UTF-8", null);
    }

    private String wrap(String html) {
        return getString(R.string.readability_html,
                mTypefaceName,
                mTextSize,
                mTextColor,
                mTextLinkColor,
                html,
                toHtmlPx(getResources().getDimension(R.dimen.activity_vertical_margin)),
                toHtmlPx(getResources().getDimension(R.dimen.activity_horizontal_margin)));
    }

    private String toHtmlColor(@AttrRes int colorAttr) {
        return String.format(FORMAT_HTML_COLOR, 0xFFFFFF & ContextCompat.getColor(getActivity(),
                AppUtils.getThemedResId(getActivity(), colorAttr)));
    }

    private float toHtmlPx(@StyleRes int textStyleAttr) {
        return toHtmlPx(AppUtils.getDimension(getActivity(), textStyleAttr, R.attr.contentTextSize));
    }

    private float toHtmlPx(float dimen) {
        return dimen / getResources().getDisplayMetrics().density;
    }

    private static class ReadabilityCallback implements ReadabilityClient.Callback {
        private final WeakReference<ReadabilityFragment> mReadabilityFragment;

        public ReadabilityCallback(ReadabilityFragment readabilityFragment) {
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

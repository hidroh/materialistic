package io.github.hidroh.materialistic;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ReadabilityClient;

public class ReadabilityFragment extends LazyLoadFragment implements Scrollable {
    public static final String EXTRA_URL = ReadabilityFragment.class.getName() + ".EXTRA_URL";
    private static final String STATE_CONTENT = "state:content";
    private static final String STATE_TEXT_SIZE = "state:textSize";
    private static final String STATE_TYPEFACE_NAME = "state:typefaceName";
    private NestedScrollView mScrollView;
    private TextView mTextView;
    private ProgressBar mProgressBar;
    @Inject ReadabilityClient mReadabilityClient;
    private String mContent;
    private float mTextSize;
    private String[] mTextSizeOptionValues;
    private String mTypefaceName;
    private String[] mFontOptionValues;
    private boolean mAttached;

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
            mTextSize = AppUtils.getDimension(getActivity(),
                    Preferences.Theme.resolvePreferredTextSizeResId(getActivity()),
                    R.attr.contentTextSize);
            mTypefaceName = Preferences.Theme.getTypeface(getActivity());
        }
        mTextSizeOptionValues = getResources().getStringArray(R.array.pref_text_size_values);
        mFontOptionValues = getResources().getStringArray(R.array.font_values);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_font_options, menu);
        SubMenu subMenu = menu.findItem(R.id.menu_font_size).getSubMenu();
        String[] options = getResources().getStringArray(R.array.text_size_options);
        for (int i = 0; i < options.length; i++) {
            subMenu.add(R.id.menu_font_size_group, Menu.NONE, i, options[i]);
        }
        subMenu = menu.findItem(R.id.menu_font).getSubMenu();
        options = getResources().getStringArray(R.array.font_options);
        for (int i = 0; i < options.length; i++) {
            subMenu.add(R.id.menu_font_group, Menu.NONE, i, options[i]);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_font_options).setVisible(!TextUtils.isEmpty(mContent));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_font_size) {
            return true;
        }
        if (item.getGroupId() == R.id.menu_font_size_group) {
            mTextSize = AppUtils.getDimension(getActivity(),
                    Preferences.Theme.resolveTextSizeResId(mTextSizeOptionValues[item.getOrder()]),
                    R.attr.contentTextSize);
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        } else if (item.getGroupId() == R.id.menu_font_group) {
            mTypefaceName = mFontOptionValues[item.getOrder()];
            mTextView.setTypeface(AppUtils.createTypeface(getActivity(), mTypefaceName));
        }
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_readability, container, false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress);
        mProgressBar.getIndeterminateDrawable()
                .setColorFilter(ContextCompat.getColor(getActivity(), R.color.redA200),
                        PorterDuff.Mode.SRC_IN);
        mScrollView = (NestedScrollView) view.findViewById(R.id.nested_scroll_view);
        mTextView = (TextView) view.findViewById(R.id.content);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
        mTextView.setTypeface(AppUtils.createTypeface(getActivity(), mTypefaceName));
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
        mReadabilityClient.parse(getArguments().getString(EXTRA_URL),
                new ReadabilityClient.Callback() {
                    @Override
                    public void onResponse(String content) {
                        mContent = content;
                        bind();
                    }
                });
    }

    private void bind() {
        if (!mAttached) {
            return;
        }
        mProgressBar.setVisibility(View.GONE);
        getActivity().supportInvalidateOptionsMenu();
        if (!TextUtils.isEmpty(mContent)) {
            AppUtils.setTextWithLinks(mTextView, mContent);
        } else {
            mTextView.setText(R.string.readability_failed);
            mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mTextView.setTextAppearance(R.style.TextAppearance_App_Empty);
            } else {
                mTextView.setTextAppearance(getActivity(), R.style.TextAppearance_App_Empty);
            }
        }
    }
}

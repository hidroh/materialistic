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

package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import io.github.hidroh.materialistic.ComposeActivity;
import io.github.hidroh.materialistic.Findable;
import io.github.hidroh.materialistic.ItemFragment;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.ReadabilityFragment;
import io.github.hidroh.materialistic.WebFragment;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.WebItem;

public class ItemPagerAdapter extends FragmentStatePagerAdapter {
    private final Fragment[] mFragments = new Fragment[3];
    private final Context mContext;
    private final WebItem mItem;
    private final boolean mShowArticle;
    private final int mCacheMode;
    private FindDialog mFindDialog;

    public ItemPagerAdapter(Context context, FragmentManager fm,
                            WebItem item, boolean showArticle, int cacheMode) {
        super(fm);
        mContext = context;
        mItem = item;
        mShowArticle = showArticle;
        mCacheMode = cacheMode;
    }

    @Override
    public Fragment getItem(int position) {
        if (mFragments[position] != null) {
            return mFragments[position];
        }
        if (position == 0) {
            Bundle args = new Bundle();
            args.putParcelable(ItemFragment.EXTRA_ITEM, mItem);
            args.putInt(ItemFragment.EXTRA_CACHE_MODE, mCacheMode);
            return Fragment.instantiate(mContext,
                    ItemFragment.class.getName(), args);
        }
        if (position == getCount() - 1) {
            Bundle readabilityArgs = new Bundle();
            readabilityArgs.putParcelable(ReadabilityFragment.EXTRA_ITEM, mItem);
            return Fragment.instantiate(mContext,
                    ReadabilityFragment.class.getName(), readabilityArgs);
        }
        return WebFragment.instantiate(mContext, mItem);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        mFragments[position] = (Fragment) super.instantiateItem(container, position);
        return mFragments[position];
    }

    @Override
    public int getCount() {
        if (mItem.isStoryType()) {
            return mShowArticle ? 3 : 2;
        } else {
            return 1;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            if (mItem instanceof Item) {
                int count = ((Item) mItem).getKidCount();
                return mContext.getResources()
                        .getQuantityString(R.plurals.comments_count, count, count);
            }
            return mContext.getString(R.string.title_activity_item);
        }
        if (position == getCount() - 1) {
            return mContext.getString(R.string.readability);
        }
        return mContext.getString(R.string.article);
    }

    public void onFabClick(FloatingActionButton fab, int pagePosition) {
        if (mFragments[pagePosition] instanceof Findable) {
            if (mFindDialog == null) {
                mFindDialog = new FindDialog(mContext);
            }
            fab.hide();
            mFindDialog.setWebView(((Findable) mFragments[pagePosition]).getWebView());
            mFindDialog.setOnDismissListener(dialog -> fab.show());
            mFindDialog.show();
        } else {
            mContext.startActivity(new Intent(mContext, ComposeActivity.class)
                    .putExtra(ComposeActivity.EXTRA_PARENT_ID, mItem.getId())
                    .putExtra(ComposeActivity.EXTRA_PARENT_TEXT,
                            mItem instanceof Item ? ((Item) mItem).getText() : null));
        }
    }

    @SuppressWarnings("ConstantConditions")
    static class FindDialog extends BottomSheetDialog {
        private final View mButtonClear;
        private final EditText mEditText;
        private WebView mWebView;

        FindDialog(@NonNull Context context) {
            super(context);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            setContentView(R.layout.dialog_find);
            mButtonClear = findViewById(R.id.button_clear);
            mButtonClear.setOnClickListener(v -> reset());
            mEditText = (EditText) findViewById(R.id.edittext);
            mEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // no op
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mButtonClear.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // no op
                }
            });
            mEditText.setOnEditorActionListener((v, actionId, event) -> { findInPage(); return true; });
            setOnDismissListener(null);
        }

        @Override
        public void setOnDismissListener(OnDismissListener listener) {
            super.setOnDismissListener(dialog -> {
                if (listener != null) {
                    listener.onDismiss(dialog);
                }
                if (mWebView == null) {
                    return;
                }
                if (mEditText.length() == 0) {
                    mWebView.clearMatches();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mWebView.setFindListener(null);
                }
            });
        }

        void setWebView(WebView webView) {
            if (mWebView != webView) {
                mWebView = webView;
                reset();
            }
        }

        private void reset() {
            mEditText.setText(null);
        }

        private void findInPage() {
            if (mWebView == null) {
                return;
            }
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
            if (numberOfMatches == 0) {
                Toast.makeText(getContext(), R.string.no_matches, Toast.LENGTH_SHORT).show();
            } else {
                ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
                dismiss();
            }
        }
    }
}

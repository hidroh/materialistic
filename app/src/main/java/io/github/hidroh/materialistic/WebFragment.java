/*
 * Copyright (c) 2016 Ha Duy Trung
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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.WebItem;

public class WebFragment extends BaseWebFragment {
    public static final String EXTRA_ITEM = BaseWebFragment.class.getName() + ".EXTRA_ITEM";
    private WebItem mItem;
    private boolean mIsHackerNewsUrl;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mItem = getArguments().getParcelable(EXTRA_ITEM);
        } else {
            mItem = savedInstanceState.getParcelable(EXTRA_ITEM);
        }
        mIsHackerNewsUrl = AppUtils.isHackerNewsUrl(mItem);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_ITEM, mItem);
    }

    @Override
    protected void load() {
        if (mIsHackerNewsUrl) {
            bindContent();
        } else {
            loadUrl(mItem.getUrl());
        }
    }

    private void bindContent() {
        if (mItem instanceof Item) {
            loadContent(((Item) mItem).getText());
        } else {
            mItemManager.getItem(mItem.getId(), ItemManager.MODE_DEFAULT, new ItemResponseListener(this));
        }
    }

    @Synthetic
    void onItemLoaded(@NonNull Item response) {
        getActivity().supportInvalidateOptionsMenu();
        mItem = response;
        bindContent();
    }

    static class ItemResponseListener implements ResponseListener<Item> {
        private final WeakReference<WebFragment> mWebFragment;

        @Synthetic
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

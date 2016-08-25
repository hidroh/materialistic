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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.WebItem;

public class ReadabilityFragment extends BaseWebFragment implements Scrollable {
    public static final String EXTRA_ITEM = ReadabilityFragment.class.getName() +".EXTRA_ITEM";
    private static final String STATE_EMPTY = "state:empty";
    @Inject ReadabilityClient mReadabilityClient;
    private boolean mEmpty;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mEmpty = savedInstanceState.getBoolean(STATE_EMPTY, false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_EMPTY, mEmpty);
    }

    @Override
    protected void load() {
        if (mEmpty) {
            loadAlternateContent();
        } else if (TextUtils.isEmpty(mContent)) {
            parse();
        } else {
            loadContent(mContent);
        }
    }

    @Override
    void showEmptyView() {
        mEmpty = true;
        Toast.makeText(getActivity(), R.string.readability_failed, Toast.LENGTH_SHORT).show();
        loadAlternateContent();
    }

    private void parse() {
        WebItem item = getArguments().getParcelable(EXTRA_ITEM);
        if (item == null) {
            return;
        }
        mProgressBar.setIndeterminate(true);
        mReadabilityClient.parse(item.getId(), item.getUrl(), new ReadabilityCallback(this));
    }

    @Synthetic
    void onParsed(String content) {
        if (isAttached()) {
            mProgressBar.setIndeterminate(false);
            loadContent(content);
        }
    }

    private void loadAlternateContent() {
        WebItem item = getArguments().getParcelable(EXTRA_ITEM);
        if (item == null) {
            return;
        }
        loadUrl(item.getUrl());
    }

    static class ReadabilityCallback implements ReadabilityClient.Callback {
        private final WeakReference<ReadabilityFragment> mReadabilityFragment;

        @Synthetic
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

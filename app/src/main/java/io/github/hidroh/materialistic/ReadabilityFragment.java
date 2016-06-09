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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.WebItem;

public class ReadabilityFragment extends BaseWebFragment implements Scrollable {
    public static final String EXTRA_ITEM = ReadabilityFragment.class.getName() +".EXTRA_ITEM";
    @Inject ReadabilityClient mReadabilityClient;

    @Override
    protected void load() {
        if (TextUtils.isEmpty(mContent)) {
            parse();
        } else {
            loadContent(mContent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ViewStub emptyStub = new ViewStub(getActivity(), R.layout.empty_readability);
        emptyStub.setId(R.id.empty_readability);
        emptyStub.setInflatedId(R.id.empty_readability);
        //noinspection ConstantConditions
        ((ViewGroup) view.findViewById(R.id.web_view_container)).addView(emptyStub);
        return view;
    }

    @Override
    void showEmptyView() {
        //noinspection ConstantConditions
        getView().findViewById(R.id.empty_readability).setVisibility(View.VISIBLE);
    }

    private void parse() {
        WebItem item = getArguments().getParcelable(EXTRA_ITEM);
        if (item == null) {
            return;
        }
        mReadabilityClient.parse(item.getId(), item.getUrl(), new ReadabilityCallback(this));
    }

    private void onParsed(String content) {
        if (isAttached()) {
            loadContent(content);
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

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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

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

    public ItemPagerAdapter(Context context, FragmentManager fm,
                            WebItem item, boolean showArticle) {
        super(fm);
        mContext = context;
        mItem = item;
        mShowArticle = showArticle;
    }

    @Override
    public Fragment getItem(int position) {
        if (mFragments[position] != null) {
            return mFragments[position];
        }
        if (position == 0) {
            Bundle args = new Bundle();
            args.putParcelable(ItemFragment.EXTRA_ITEM, mItem);
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
}

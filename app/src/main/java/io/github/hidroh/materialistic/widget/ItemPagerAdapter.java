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
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.ViewGroup;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.ItemFragment;
import io.github.hidroh.materialistic.LazyLoadFragment;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.Scrollable;
import io.github.hidroh.materialistic.WebFragment;
import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.WebItem;

public class ItemPagerAdapter extends FragmentStatePagerAdapter {
    private final Fragment[] mFragments = new Fragment[3];
    private final Context mContext;
    private final WebItem mItem;
    private final boolean mShowArticle;
    private final int mCacheMode;
    private final int mDefaultItem;
    private final boolean mRetainInstance;
    private TabLayout.OnTabSelectedListener mTabListener;

    public ItemPagerAdapter(Context context, FragmentManager fm, @NonNull Builder builder) {
        super(fm);
        mContext = context;
        mItem = builder.item;
        mShowArticle = builder.showArticle;
        mCacheMode = builder.cacheMode;
        mRetainInstance = builder.retainInstance;
        mDefaultItem = Math.min(getCount()-1,
                builder.defaultViewMode == Preferences.StoryViewMode.Comment ? 0 : 1);
    }

    @Override
    public Fragment getItem(int position) {
        if (mFragments[position] != null) {
            return mFragments[position];
        }
        String fragmentName;
        Bundle args = new Bundle();
        args.putBoolean(LazyLoadFragment.EXTRA_EAGER_LOAD, mDefaultItem == position);
        if (position == 0) {
            args.putParcelable(ItemFragment.EXTRA_ITEM, mItem);
            args.putInt(ItemFragment.EXTRA_CACHE_MODE, mCacheMode);
            args.putBoolean(ItemFragment.EXTRA_RETAIN_INSTANCE, mRetainInstance);
            fragmentName = ItemFragment.class.getName();
        } else {
            args.putParcelable(WebFragment.EXTRA_ITEM, mItem);
            args.putBoolean(WebFragment.EXTRA_RETAIN_INSTANCE, mRetainInstance);
            fragmentName = WebFragment.class.getName();
        }
        return Fragment.instantiate(mContext, fragmentName, args);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        mFragments[position] = (Fragment) super.instantiateItem(container, position);
        return mFragments[position];
    }

    @Override
    public int getCount() {
        return mItem.isStoryType() && !mShowArticle ? 1 : 2;
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
        return mContext.getString(mItem.isStoryType() ? R.string.article : R.string.full_text);
    }

    public void bind(ViewPager viewPager, TabLayout tabLayout,
                     FloatingActionButton navigationFab, FloatingActionButton genericFab) {
        viewPager.setPageMargin(viewPager.getResources().getDimensionPixelOffset(R.dimen.divider));
        viewPager.setPageMarginDrawable(R.color.blackT12);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(this);
        tabLayout.setupWithViewPager(viewPager);
        mTabListener = new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                toggleFabs(viewPager.getCurrentItem() == 0, navigationFab, genericFab);
                Fragment fragment = getItem(viewPager.getCurrentItem());
                if (fragment != null) {
                    ((LazyLoadFragment) fragment).loadNow();
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Fragment fragment = getItem(viewPager.getCurrentItem());
                if (fragment != null) {
                    ((Scrollable) fragment).scrollToTop();
                }
            }
        };
        tabLayout.addOnTabSelectedListener(mTabListener);
        viewPager.setCurrentItem(mDefaultItem);
        toggleFabs(mDefaultItem == 0, navigationFab, genericFab);

    }

    @Synthetic
    void toggleFabs(boolean isComments,
                    FloatingActionButton navigationFab,
                    FloatingActionButton genericFab) {
        AppUtils.toggleFab(navigationFab, isComments &&
                Preferences.navigationEnabled(navigationFab.getContext()));
        AppUtils.toggleFab(genericFab, true);
        AppUtils.toggleFabAction(genericFab, mItem, isComments);
    }

    public void unbind(TabLayout tabLayout) {
        if (mTabListener != null) {
            tabLayout.removeOnTabSelectedListener(mTabListener);
        }
    }

    public static class Builder {
        WebItem item;
        boolean showArticle;
        int cacheMode;
        Preferences.StoryViewMode defaultViewMode;
        boolean retainInstance;

        public Builder setItem(@NonNull WebItem item) {
            this.item = item;
            return this;
        }

        public Builder setShowArticle(boolean showArticle) {
            this.showArticle = showArticle;
            return this;
        }

        public Builder setCacheMode(int cacheMode) {
            this.cacheMode = cacheMode;
            return this;
        }

        public Builder setDefaultViewMode(Preferences.StoryViewMode viewMode) {
            this.defaultViewMode = viewMode;
            return this;
        }

        public Builder setRetainInstance(boolean retainInstance) {
            this.retainInstance = retainInstance;
            return this;
        }
    }
}

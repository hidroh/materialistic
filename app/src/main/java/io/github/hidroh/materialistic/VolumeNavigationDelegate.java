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

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.annotation.IntDef;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper that intercepts key events and interprets them into navigation actions
 */
public class VolumeNavigationDelegate {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DIRECTION_NONE,
            DIRECTION_UP,
            DIRECTION_DOWN
    })
    @interface Direction {}
    private static final int DIRECTION_NONE = 0;
    private static final int DIRECTION_UP = 1;
    private static final int DIRECTION_DOWN = 2;

    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener;
    private String mPreferenceKey;
    private boolean mEnabled;
    private Scrollable mScrollable;
    private AppBarLayout mAppBarLayout;

    public VolumeNavigationDelegate() {
        mPreferenceListener = (sharedPreferences, key) -> {
            if (TextUtils.equals(key, mPreferenceKey)) {
                mEnabled = sharedPreferences.getBoolean(key, false);
            }
        };
    }

    /**
     * Attaches this delegate to given activity lifecycle
     * Should call {@link #detach(Activity)} accordingly
     * @param activity    active activity to receive key events
     * @see {@link #detach(Activity)}
     */
    public void attach(Activity activity) {
        mPreferenceKey = activity.getString(R.string.pref_volume);
        mEnabled = PreferenceManager.getDefaultSharedPreferences(activity)
                .getBoolean(mPreferenceKey, false);
        PreferenceManager.getDefaultSharedPreferences(activity)
                .registerOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    /**
     * Detaches this delegate from given activity lifecycle
     * Should already call {@link #attach(Activity)}
     * @param activity    active activity that has been receiving key events
     * @see {@link #attach(Activity)}
     */
    public void detach(Activity activity) {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
        mScrollable = null;
        mAppBarLayout = null;
    }

    /**
     * Binds navigation objects that would be scrolled by key events
     * @param scrollable      vertically scrollable instance
     * @param appBarLayout    optional AppBarLayout that expands/collapses while scrolling
     */
    public void setScrollable(Scrollable scrollable, AppBarLayout appBarLayout) {
        mScrollable = scrollable;
        mAppBarLayout = appBarLayout;
    }

    /**
     * Calls from {@link Activity#onKeyDown(int, KeyEvent)} to delegate
     * @param keyCode    event key code
     * @param event      key event
     * @return  true if is intercepted as navigation, false otherwise
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mEnabled) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            event.startTracking();
            return true;
        }
        return false;
    }

    /**
     * Calls from {@link Activity#onKeyUp(int, KeyEvent)} to delegate
     * @param keyCode    event key code
     * @param event      key event
     * @return  true if is intercepted as navigation, false otherwise
     */
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!mEnabled) {
            return false;
        }
        boolean notLongPress = (event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && notLongPress) {
            shortPress(DIRECTION_UP);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && notLongPress) {
            shortPress(DIRECTION_DOWN);
            return true;
        }
        return false;
    }

    /**
     * Calls from {@link Activity#onKeyLongPress(int, KeyEvent)} to delegate
     * @param keyCode    event key code
     * @param event      key event
     * @return  true if is intercepted as navigation, false otherwise
     */
    @SuppressWarnings("UnusedParameters")
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (!mEnabled) {
            return false;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            longPress(DIRECTION_UP);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            longPress(DIRECTION_DOWN);
            return true;
        }
        return false;
    }

    private void shortPress(@Direction int direction) {
        if (mScrollable == null) {
            return;
        }
        switch (direction) {
            case DIRECTION_UP:
                if (!mScrollable.scrollToPrevious() && mAppBarLayout != null) {
                    mAppBarLayout.setExpanded(true, true);
                }
                break;
            case DIRECTION_DOWN:
                if (mAppBarLayout != null &&
                        mAppBarLayout.getHeight() == mAppBarLayout.getBottom()) {
                    mAppBarLayout.setExpanded(false, true);
                } else {
                    mScrollable.scrollToNext();
                }
                break;
            case DIRECTION_NONE:
            default:
                break;
        }
    }

    private void longPress(@Direction int direction) {
        switch (direction) {
            case DIRECTION_DOWN:
            case DIRECTION_NONE:
            default:
                break;
            case DIRECTION_UP:
                if (mAppBarLayout != null) {
                    mAppBarLayout.setExpanded(true, true);
                }
                if (mScrollable != null) {
                    mScrollable.scrollToTop();
                }
                break;
        }
    }

    /**
     * Helper class to navigate vertical RecyclerView
     */
    public static class RecyclerViewHelper implements Scrollable {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({
                SCROLL_ITEM,
                SCROLL_PAGE
        })
        @interface ScrollMode {}
        public static final int SCROLL_ITEM = 0;
        public static final int SCROLL_PAGE = 1;

        private final RecyclerView mRecyclerView;
        private final LinearLayoutManager mLayoutManager;
        private final @ScrollMode int mScrollMode;

        public RecyclerViewHelper(RecyclerView recyclerView, @ScrollMode int scrollMode) {
            mRecyclerView = recyclerView;
            if (!(mRecyclerView.getLayoutManager() instanceof LinearLayoutManager)) {
                throw new IllegalArgumentException("Only LinearLayoutManager supported");
            }
            mLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            mScrollMode = scrollMode;
        }

        @Override
        public void scrollToTop() {
            mRecyclerView.smoothScrollToPosition(0);
        }

        @Override
        public boolean scrollToNext() {
            int pos = mScrollMode == SCROLL_ITEM ?
                    mLayoutManager.findFirstVisibleItemPosition() :
                    mLayoutManager.findLastCompletelyVisibleItemPosition(),
                    next = pos != RecyclerView.NO_POSITION ?
                            pos + 1 : RecyclerView.NO_POSITION;
            if (next != RecyclerView.NO_POSITION &&
                    next < mRecyclerView.getAdapter().getItemCount()) {
                mRecyclerView.smoothScrollToPosition(next);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean scrollToPrevious() {
            switch (mScrollMode) {
                case SCROLL_ITEM:
                default:
                    int pos = mLayoutManager.findFirstVisibleItemPosition(),
                            previous = pos != RecyclerView.NO_POSITION ?
                                    pos - 1 : RecyclerView.NO_POSITION;
                    if (previous >= 0) {
                        mRecyclerView.smoothScrollToPosition(previous);
                        return true;
                    } else {
                        return false;
                    }
                case SCROLL_PAGE:
                    if (mLayoutManager.findFirstVisibleItemPosition() <= 0) {
                        return false;
                    } else {
                        mRecyclerView.smoothScrollBy(0, -mRecyclerView.getHeight());
                        return true;
                    }
            }
        }
    }

    /**
     * Helper class to navigate vertical NestedScrollView
     */
    public static class NestedScrollViewHelper implements Scrollable {

        private final NestedScrollView mScrollView;

        public NestedScrollViewHelper(NestedScrollView nestedScrollView) {
            mScrollView = nestedScrollView;
        }

        @Override
        public void scrollToTop() {
            mScrollView.smoothScrollTo(0, 0);
        }

        @Override
        public boolean scrollToNext() {
            return mScrollView.pageScroll(View.FOCUS_DOWN);
        }

        @Override
        public boolean scrollToPrevious() {
            return mScrollView.pageScroll(View.FOCUS_UP);
        }
    }
}

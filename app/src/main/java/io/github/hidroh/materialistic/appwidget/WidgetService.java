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

package io.github.hidroh.materialistic.appwidget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.Injectable;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class WidgetService extends RemoteViewsService {
    static final String EXTRA_SECTION = "extra:section";
    static final String EXTRA_LIGHT_THEME = "extra:lightTheme";
    static final String EXTRA_CUSTOM_QUERY = "extra:customQuery";
    @Inject @Named(ActivityModule.HN) ItemManager mItemManager;
    @Inject @Named(ActivityModule.ALGOLIA) ItemManager mSearchManager;

    @Override
    public void onCreate() {
        super.onCreate();
        ((Injectable) getApplication())
                .getApplicationGraph()
                .plus(new ActivityModule(this))
                .inject(this);
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(getApplicationContext(),
                intent.getBooleanExtra(EXTRA_CUSTOM_QUERY, false) ? mSearchManager : mItemManager,
                intent.getStringExtra(EXTRA_SECTION),
                intent.getBooleanExtra(EXTRA_LIGHT_THEME, false));
    }

    static class ListRemoteViewsFactory implements RemoteViewsFactory {

        private static final String SCORE = "%1$dp";
        private static final String COMMENT = "%1$dc";
        private static final String SUBTITLE_SEPARATOR = " - ";
        private static final int MAX_ITEMS = 10;
        private final Context mContext;
        private final ItemManager mItemManager;
        private final String mFilter;
        private final boolean mLightTheme;
        private final int mHotThreshold;
        private Item[] mItems;

        ListRemoteViewsFactory(Context context, ItemManager itemManager, String section, boolean lightTheme) {
            mContext = context;
            mItemManager = itemManager;
            mLightTheme = lightTheme;
            if (TextUtils.equals(section,
                    context.getString(R.string.pref_widget_section_value_best))) {
                mFilter = ItemManager.BEST_FETCH_MODE;
                mHotThreshold = AppUtils.HOT_THRESHOLD_HIGH;
            } else if (TextUtils.equals(section,
                    context.getString(R.string.pref_widget_section_value_top))) {
                mFilter = ItemManager.TOP_FETCH_MODE;
                mHotThreshold = AppUtils.HOT_THRESHOLD_NORMAL;
            } else {
                mFilter = section;
                mHotThreshold = AppUtils.HOT_THRESHOLD_NORMAL;
            }
        }

        @Override
        public void onCreate() {
            // no op
        }

        @Override
        public void onDataSetChanged() {
            mItems = mItemManager.getStories(mFilter, ItemManager.MODE_NETWORK);
        }

        @Override
        public void onDestroy() {
            // no op
        }

        @Override
        public int getCount() {
            return mItems != null ? Math.min(mItems.length, MAX_ITEMS) : 0;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(),
                    mLightTheme ? R.layout.item_widget_light : R.layout.item_widget);
            Item item = getItem(position);
            if (item == null) {
                return remoteViews;
            }
            if (!isItemAvailable(item)) {
                Item remoteItem = mItemManager.getItem(item.getId(), ItemManager.MODE_NETWORK);
                if (remoteItem != null) {
                    item.populate(remoteItem);
                } else {
                    return remoteViews;
                }
            }
            remoteViews.setTextViewText(R.id.title, item.getDisplayedTitle());
            remoteViews.setTextViewText(R.id.score, new SpannableStringBuilder()
                    .append(getSpan(item.getScore(), SCORE, mHotThreshold * AppUtils.HOT_FACTOR))
                    .append(SUBTITLE_SEPARATOR)
                    .append(getSpan(item.getKidCount(), COMMENT, mHotThreshold)));
            remoteViews.setOnClickFillInIntent(R.id.item_view, new Intent().setData(
                    AppUtils.createItemUri(item.getId())));
            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(mContext.getPackageName(), R.layout.item_widget);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            Item item = getItem(position);
            return item != null ? item.getLongId() : 0L;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        private Item getItem(int position) {
            return mItems != null && position < mItems.length ? mItems[position] : null;
        }

        private boolean isItemAvailable(Item item) {
            return item != null && item.getLocalRevision() > 0;
        }

        private SpannableString getSpan(int value, String format, int hotThreshold) {
            String text = String.format(Locale.US, format, value);
            SpannableString spannable = new SpannableString(text);
            if (value >= hotThreshold) {
                spannable.setSpan(new ForegroundColorSpan(
                                ContextCompat.getColor(mContext, R.color.orange500)),
                        0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannable;
        }
    }
}

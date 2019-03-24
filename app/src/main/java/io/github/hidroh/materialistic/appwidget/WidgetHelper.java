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
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import java.util.Locale;

import io.github.hidroh.materialistic.BestActivity;
import io.github.hidroh.materialistic.ListActivity;
import io.github.hidroh.materialistic.NewActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.SearchActivity;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;

class WidgetHelper {
    private static final String SP_NAME = "WidgetConfiguration_%1$d";
    private static final int DEFAULT_FREQUENCY_HOUR = 6;
    private final Context mContext;
    private final AppWidgetManager mAppWidgetManager;
    private final AlarmManager mAlarmManager;

    WidgetHelper(Context context) {
        mContext = context;
        mAppWidgetManager = AppWidgetManager.getInstance(context);
        mAlarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
    }

    static String getConfigName(int appWidgetId) {
        return String.format(Locale.US, SP_NAME, appWidgetId);
    }

    void configure(int appWidgetId) {
        scheduleUpdate(appWidgetId);
        update(appWidgetId);
    }

    void update(int appWidgetId) {
        WidgetConfig config = WidgetConfig.createWidgetConfig(mContext,
                getConfig(appWidgetId, R.string.pref_widget_theme),
                getConfig(appWidgetId, R.string.pref_widget_section),
                getConfig(appWidgetId, R.string.pref_widget_query));
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), config.widgetLayout);
        updateTitle(remoteViews, config);
        updateCollection(appWidgetId, remoteViews, config);
        mAppWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void refresh(int appWidgetId) {
        mAppWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list);
        update(appWidgetId);
    }

    void remove(int appWidgetId) {
        cancelScheduledUpdate(appWidgetId);
        clearConfig(appWidgetId);
    }

    private void scheduleUpdate(int appWidgetId) {
        String frequency = getConfig(appWidgetId, R.string.pref_widget_frequency);
        long frequencyHourMillis = DateUtils.HOUR_IN_MILLIS * (TextUtils.isEmpty(frequency) ?
                DEFAULT_FREQUENCY_HOUR : Integer.valueOf(frequency));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getJobScheduler().schedule(new JobInfo.Builder(appWidgetId,
                    new ComponentName(mContext.getPackageName(), WidgetRefreshJobService.class.getName()))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(frequencyHourMillis)
                    .build());
        } else {
            mAlarmManager.setInexactRepeating(AlarmManager.RTC,
                    System.currentTimeMillis() + frequencyHourMillis,
                    frequencyHourMillis,
                    createRefreshPendingIntent(appWidgetId));
        }

    }

    private void cancelScheduledUpdate(int appWidgetId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getJobScheduler().cancel(appWidgetId);
        } else {
            mAlarmManager.cancel(createRefreshPendingIntent(appWidgetId));
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private JobScheduler getJobScheduler() {
        return (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    private String getConfig(int appWidgetId, @StringRes int key) {
        return mContext.getSharedPreferences(getConfigName(appWidgetId), MODE_PRIVATE)
                .getString(mContext.getString(key), null);
    }

    private void clearConfig(int appWidgetId) {
        mContext.getSharedPreferences(getConfigName(appWidgetId), MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private void updateTitle(RemoteViews remoteViews, WidgetConfig config) {
        remoteViews.setTextViewText(R.id.title, config.title);
        remoteViews.setOnClickPendingIntent(R.id.title,
                PendingIntent.getActivity(mContext, 0, config.customQuery ?
                        new Intent(mContext, config.destination)
                                .putExtra(SearchManager.QUERY, config.title) :
                        new Intent(mContext, config.destination), 0));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void updateCollection(int appWidgetId, RemoteViews remoteViews, WidgetConfig config) {
        remoteViews.setTextViewText(R.id.subtitle,
                DateUtils.formatDateTime(mContext, System.currentTimeMillis(),
                        DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME));
        remoteViews.setOnClickPendingIntent(R.id.button_refresh,
                createRefreshPendingIntent(appWidgetId));
        Intent intent = new Intent(mContext, WidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .putExtra(WidgetService.EXTRA_CUSTOM_QUERY, config.customQuery)
                .putExtra(WidgetService.EXTRA_SECTION, config.section)
                .putExtra(WidgetService.EXTRA_LIGHT_THEME, config.isLightTheme);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            remoteViews.setRemoteAdapter(android.R.id.list, intent);
        } else {
            //noinspection deprecation
            remoteViews.setRemoteAdapter(appWidgetId, android.R.id.list, intent);
        }
        remoteViews.setEmptyView(android.R.id.list, R.id.empty);
        remoteViews.setPendingIntentTemplate(android.R.id.list,
                PendingIntent.getActivity(mContext, 0, new Intent(Intent.ACTION_VIEW), 0));
    }

    private PendingIntent createRefreshPendingIntent(int appWidgetId) {
        return PendingIntent.getBroadcast(mContext, appWidgetId,
                new Intent(WidgetProvider.ACTION_REFRESH_WIDGET)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        .setPackage(mContext.getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static class WidgetConfig {
        final boolean customQuery;
        final Class<? extends Activity> destination;
        final String title;
        final boolean isLightTheme;
        final @LayoutRes int widgetLayout;
        final String section;

        @NonNull
        static WidgetConfig createWidgetConfig(Context context, String theme, String section, String query) {
            int widgetLayout;
            boolean isLightTheme = false;
            if (TextUtils.equals(theme, context.getString(R.string.pref_widget_theme_value_dark))) {
                widgetLayout = R.layout.appwidget_dark;
            } else if (TextUtils.equals(theme, context.getString(R.string.pref_widget_theme_value_light))) {
                widgetLayout = R.layout.appwidget_light;
                isLightTheme = true;
            } else {
                widgetLayout = R.layout.appwidget;
            }
            String title;
            Class<? extends Activity> destination;
            if (!TextUtils.isEmpty(query)) {
                title = query;
                section = query;
                destination = SearchActivity.class;
            } else if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_best))) {
                title = context.getString(R.string.title_activity_best);
                destination = BestActivity.class;
            } else if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_top))) {
                title = context.getString(R.string.title_activity_list);
                destination = ListActivity.class;
            } else {
                // legacy "new stories" widget
                title = context.getString(R.string.title_activity_new);
                destination = NewActivity.class;
            }
            return new WidgetConfig(destination, title, section, isLightTheme, widgetLayout);
        }

        private WidgetConfig(Class<? extends Activity> destination, String title, String section,
                             boolean isLightTheme, int widgetLayout) {
            this.destination = destination;
            this.title = title;
            this.section = section;
            this.isLightTheme = isLightTheme;
            this.widgetLayout = widgetLayout;
            this.customQuery = destination == SearchActivity.class;
        }
    }
}

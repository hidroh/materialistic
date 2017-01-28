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
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import java.util.Locale;

import io.github.hidroh.materialistic.BestActivity;
import io.github.hidroh.materialistic.ListActivity;
import io.github.hidroh.materialistic.NewActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.SearchActivity;
import io.github.hidroh.materialistic.annotation.Synthetic;

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

    void configure(int appWidgetId) {
        scheduleUpdate(appWidgetId);
        update(appWidgetId);
    }

    void update(int appWidgetId) {
        WidgetConfig config = WidgetConfig.createWidgetConfig(mContext, appWidgetId);
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
        String frequency = WidgetConfig.getConfig(mContext, appWidgetId, R.string.pref_widget_frequency);
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

    private void clearConfig(int appWidgetId) {
        mContext.getSharedPreferences(WidgetConfig.getConfigName(appWidgetId), MODE_PRIVATE)
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
                .putExtra(WidgetService.EXTRA_CONFIG, config.toBundle());
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
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static class WidgetConfig {
        private static final String EXTRA_CUSTOM_QUERY = "extra:customQuery";
        private static final String EXTRA_TITLE = "extra:title";
        private static final String EXTRA_IS_LIGHT_THEME = "extra:isLightTheme";
        private static final String EXTRA_WIDGET_LAYOUT = "extra:widgetLayout";
        private static final String EXTRA_SECTION = "extra:section";
        boolean customQuery;
        @Synthetic Class<? extends Activity> destination;
        @Synthetic String title;
        boolean isLightTheme;
        @Synthetic @LayoutRes int widgetLayout;
        String section;

        @Synthetic WidgetConfig(Bundle bundle) {
            customQuery = bundle.getBoolean(EXTRA_CUSTOM_QUERY);
            title = bundle.getString(EXTRA_TITLE);
            isLightTheme = bundle.getBoolean(EXTRA_IS_LIGHT_THEME);
            widgetLayout = bundle.getInt(EXTRA_WIDGET_LAYOUT);
            section = bundle.getString(EXTRA_SECTION);
            destination = ListActivity.class; // not part of bundle
        }

        @NonNull
        static WidgetConfig createWidgetConfig(Context context, int appWidgetId) {
            String theme = getConfig(context, appWidgetId, R.string.pref_widget_theme),
                    section = getConfig(context, appWidgetId, R.string.pref_widget_section),
                    query = getConfig(context, appWidgetId, R.string.pref_widget_query);
            WidgetConfig config = new WidgetConfig();
            if (TextUtils.equals(theme, context.getString(R.string.pref_widget_theme_value_dark))) {
                config.widgetLayout = R.layout.appwidget_dark;
            } else if (TextUtils.equals(theme, context.getString(R.string.pref_widget_theme_value_light))) {
                config.widgetLayout = R.layout.appwidget_light;
                config.isLightTheme = true;
            } else {
                config.widgetLayout = R.layout.appwidget;
            }
            config.section = section;
            if (!TextUtils.isEmpty(query)) {
                config.title = query;
                config.section = query;
                config.destination = SearchActivity.class;
                config.customQuery = true;
            } else if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_best))) {
                config.title = context.getString(R.string.title_activity_best);
                config.destination = BestActivity.class;
            } else if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_top))) {
                config.title = context.getString(R.string.title_activity_list);
                config.destination = ListActivity.class;
            } else {
                // legacy "new stories" widget
                config.title = context.getString(R.string.title_activity_new);
                config.destination = NewActivity.class;
            }
            return config;
        }

        @Synthetic static String getConfig(Context context, int appWidgetId, @StringRes int key) {
            return context.getSharedPreferences(getConfigName(appWidgetId), MODE_PRIVATE)
                    .getString(context.getString(key), null);
        }

        static String getConfigName(int appWidgetId) {
            return String.format(Locale.US, SP_NAME, appWidgetId);
        }

        private WidgetConfig() {}

        Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putBoolean(EXTRA_CUSTOM_QUERY, customQuery);
            bundle.putString(EXTRA_TITLE, title);
            bundle.putBoolean(EXTRA_IS_LIGHT_THEME, isLightTheme);
            bundle.putInt(EXTRA_WIDGET_LAYOUT, widgetLayout);
            bundle.putString(EXTRA_SECTION, section);
            return bundle;
        }
    }
}

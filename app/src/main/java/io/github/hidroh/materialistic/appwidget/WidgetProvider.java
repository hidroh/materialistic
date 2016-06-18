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
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import io.github.hidroh.materialistic.BestActivity;
import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.ListActivity;
import io.github.hidroh.materialistic.NewActivity;
import io.github.hidroh.materialistic.R;

public class WidgetProvider extends AppWidgetProvider {

    private static final String ACTION_REFRESH_WIDGET = BuildConfig.APPLICATION_ID + ".ACTION_REFRESH_WIDGET";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), ACTION_REFRESH_WIDGET) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            AppWidgetManager.getInstance(context)
                    .notifyAppWidgetViewDataChanged(appWidgetId, android.R.id.list);
            updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            Toast.makeText(context, R.string.not_supported, Toast.LENGTH_SHORT).show();
            return;
        }
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                    .cancel(WidgetProvider.createRefreshPendingIntent(context, appWidgetId));
            WidgetConfigActivity.clearConfig(context, appWidgetId);
        }
    }

    static PendingIntent createRefreshPendingIntent(Context context, int appWidgetId) {
        return PendingIntent.getBroadcast(context, 0,
                new Intent(WidgetProvider.ACTION_REFRESH_WIDGET)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        WidgetConfig config = WidgetConfig.getWidgetConfig(context, appWidgetId);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), config.widgetLayout);
        updateTitle(context, remoteViews, config);
        updateCollection(context, appWidgetId, remoteViews, config);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private static void updateTitle(Context context, RemoteViews remoteViews, WidgetConfig config) {
        remoteViews.setTextViewText(R.id.title, config.title);
        remoteViews.setOnClickPendingIntent(R.id.title,
                PendingIntent.getActivity(context, 0, new Intent(context, config.destination), 0));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void updateCollection(Context context, int appWidgetId, RemoteViews remoteViews, WidgetConfig config) {
        remoteViews.setTextViewText(R.id.subtitle,
                DateUtils.formatDateTime(context, System.currentTimeMillis(),
                        DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME));
        remoteViews.setOnClickPendingIntent(R.id.button_refresh,
                createRefreshPendingIntent(context, appWidgetId));
        Intent intent = new Intent(context, WidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
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
                PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_VIEW), 0));
    }

    static class WidgetConfig {
        Class<? extends Activity> destination;
        String title;
        boolean isLightTheme;
        @LayoutRes int widgetLayout;
        String section;

        @NonNull
        static WidgetConfig getWidgetConfig(Context context, int appWidgetId) {
            String theme = WidgetConfigActivity.getConfig(context, appWidgetId, R.string.pref_widget_theme);
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
            String section = WidgetConfigActivity.getConfig(context, appWidgetId, R.string.pref_widget_section);
            String title;
            Class<? extends Activity> destination;
            if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_best))) {
                title = context.getString(R.string.title_activity_best);
                destination = BestActivity.class;
            } else if (TextUtils.equals(section, context.getString(R.string.pref_widget_section_value_new))) {
                title = context.getString(R.string.title_activity_new);
                destination = NewActivity.class;
            } else {
                title = context.getString(R.string.title_activity_list);
                destination = ListActivity.class;
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
        }
    }
}

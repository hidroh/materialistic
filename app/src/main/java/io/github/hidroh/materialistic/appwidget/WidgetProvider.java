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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.R;

public class WidgetProvider extends AppWidgetProvider {

    static final String ACTION_REFRESH_WIDGET = BuildConfig.APPLICATION_ID + ".ACTION_REFRESH_WIDGET";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(intent.getAction(), ACTION_REFRESH_WIDGET)) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            new WidgetHelper(context).refresh(appWidgetId);
        } else if (TextUtils.equals(intent.getAction(), AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (appWidgetIds != null) {
                WidgetHelper widgetHelper = new WidgetHelper(context);
                for (int appWidgetId : appWidgetIds) {
                    widgetHelper.configure(appWidgetId);
                }
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WidgetHelper widgetHelper = new WidgetHelper(context);
        for (int appWidgetId : appWidgetIds) {
            widgetHelper.update(appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        WidgetHelper widgetHelper = new WidgetHelper(context);
        for (int appWidgetId : appWidgetIds) {
            widgetHelper.remove(appWidgetId);
        }
    }
}

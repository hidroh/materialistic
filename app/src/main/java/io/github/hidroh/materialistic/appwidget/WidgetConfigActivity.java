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

import android.app.AlarmManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Window;

import java.util.Locale;

import io.github.hidroh.materialistic.InjectableActivity;
import io.github.hidroh.materialistic.R;

public class WidgetConfigActivity extends InjectableActivity {
    private static final String SP_NAME = "WidgetConfiguration_%1$d";
    private static final int DEFAULT_FREQUENCY_HOUR = 6;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        if (getIntent().getExtras() == null ||
                (mAppWidgetId = getIntent().getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID)) == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_widget_config);
        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            args.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.widget_preferences,
                            Fragment.instantiate(this, WidgetConfigurationFragment.class.getName(), args),
                            WidgetConfigurationFragment.class.getName())
                    .commit();
        }
        //noinspection ConstantConditions
        findViewById(R.id.button_ok).setOnClickListener(v -> configure());
    }

    @Override
    protected boolean isDialogTheme() {
        return true;
    }

    private void configure() {
        String frequency = getConfig(this, mAppWidgetId, R.string.pref_widget_frequency);
        int frequencyHour = TextUtils.isEmpty(frequency) ?
                DEFAULT_FREQUENCY_HOUR : Integer.valueOf(frequency);
        ((AlarmManager) getSystemService(ALARM_SERVICE))
                .setInexactRepeating(AlarmManager.RTC,
                        System.currentTimeMillis(),
                        DateUtils.HOUR_IN_MILLIS * frequencyHour,
                        WidgetProvider.createRefreshPendingIntent(this, mAppWidgetId));
        WidgetProvider.updateAppWidget(this, AppWidgetManager.getInstance(this), mAppWidgetId);
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
        finish();
    }

    public static String getConfig(Context context, int appWidgetId, @StringRes int key) {
        return context.getSharedPreferences(getConfigName(appWidgetId), MODE_PRIVATE)
                .getString(context.getString(key), null);
    }

    public static void clearConfig(Context context, int appWidgetId) {
        context.getSharedPreferences(getConfigName(appWidgetId), MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private static String getConfigName(int appWidgetId) {
        return String.format(Locale.US, SP_NAME, appWidgetId);
    }

    public static class WidgetConfigurationFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(getConfigName(
                    getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)));
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preferences_widget);
        }
    }
}

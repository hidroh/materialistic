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
import android.app.AlarmManager;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowJobScheduler;

import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.test.TestRunner;

import static android.content.Context.MODE_PRIVATE;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunner.class)
public class WidgetProviderTest {
    private WidgetProvider widgetProvider;
    private AlarmManager alarmManager;
    private AppWidgetManager widgetManager;
    private JobScheduler jobScheduler;
    private int appWidgetId;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Before
    public void setUp() {
        widgetProvider = new WidgetProvider();
        alarmManager = (AlarmManager) RuntimeEnvironment.application.getSystemService(Context.ALARM_SERVICE);
        jobScheduler = (JobScheduler) RuntimeEnvironment.application.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        widgetManager = AppWidgetManager.getInstance(RuntimeEnvironment.application);
        appWidgetId = shadowOf(widgetManager).createWidget(WidgetProvider.class, R.layout.appwidget);
    }

    @Config(sdk = 18)
    @Test
    public void testDeleteCancelAlarm() {
        new WidgetHelper(RuntimeEnvironment.application).configure(appWidgetId);
        assertThat(shadowOf(alarmManager).getNextScheduledAlarm()).isNotNull();
        widgetProvider.onDeleted(RuntimeEnvironment.application, new int[]{appWidgetId});
        assertThat(shadowOf(alarmManager).getNextScheduledAlarm()).isNull();
    }

    @Config(sdk = 21)
    @Test
    public void testDeleteCancelJob() {
        new WidgetHelper(RuntimeEnvironment.application).configure(appWidgetId);
        assertThat(((ShadowJobScheduler.ShadowJobSchedulerImpl) shadowOf(jobScheduler))
                .getAllPendingJobs()).isNotEmpty();
        widgetProvider.onDeleted(RuntimeEnvironment.application, new int[]{appWidgetId});
        // TODO
//        assertThat(shadowOf(jobScheduler).getAllPendingJobs()).isEmpty();
    }

    @Config(sdk = 18)
    @Test
    public void testAlarmAfterReboot() {
        // rebooting should update widget again via update broadcast
        widgetProvider.onReceive(RuntimeEnvironment.application,
                new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId}));
        assertThat(shadowOf(alarmManager).getNextScheduledAlarm()).isNotNull();
        widgetProvider.onDeleted(RuntimeEnvironment.application, new int[]{appWidgetId});
        assertThat(shadowOf(alarmManager).getNextScheduledAlarm()).isNull();
    }

    @Config(sdk = 21)
    @Test
    public void testJobAfterReboot() {
        // rebooting should update widget again via update broadcast
        widgetProvider.onReceive(RuntimeEnvironment.application,
                new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId}));
        assertThat(((ShadowJobScheduler.ShadowJobSchedulerImpl) shadowOf(jobScheduler))
                .getAllPendingJobs()).isNotEmpty();
    }

    @Test
    public void testUpdateBest() {
        RuntimeEnvironment.application.getSharedPreferences("WidgetConfiguration_" + appWidgetId, MODE_PRIVATE)
                .edit()
                .putString(RuntimeEnvironment.application.getString(R.string.pref_widget_theme),
                        RuntimeEnvironment.application.getString(R.string.pref_widget_theme_value_dark))
                .putString(RuntimeEnvironment.application.getString(R.string.pref_widget_section),
                        RuntimeEnvironment.application.getString(R.string.pref_widget_section_value_best))
                .apply();
        widgetProvider.onReceive(RuntimeEnvironment.application,
                new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId}));
        View view = shadowOf(widgetManager).getViewFor(appWidgetId);
        assertThat((TextView) view.findViewById(R.id.title))
                .containsText(R.string.title_activity_best);
        assertThat((TextView) view.findViewById(R.id.subtitle))
                .doesNotContainText(R.string.loading_text);
    }

    @Test
    public void testRefreshQuery() {
        RuntimeEnvironment.application.getSharedPreferences("WidgetConfiguration_" + appWidgetId, MODE_PRIVATE)
                .edit()
                .putString(RuntimeEnvironment.application.getString(R.string.pref_widget_theme),
                        RuntimeEnvironment.application.getString(R.string.pref_widget_theme_value_light))
                .putString(RuntimeEnvironment.application.getString(R.string.pref_widget_query), "Google")
                .apply();
        widgetProvider.onReceive(RuntimeEnvironment.application,
                new Intent(BuildConfig.APPLICATION_ID + ".ACTION_REFRESH_WIDGET")
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
        View view = shadowOf(widgetManager).getViewFor(appWidgetId);
        assertThat((TextView) view.findViewById(R.id.title))
                .containsText("Google");
        assertThat((TextView) view.findViewById(R.id.subtitle))
                .doesNotContainText(R.string.loading_text);
    }
}

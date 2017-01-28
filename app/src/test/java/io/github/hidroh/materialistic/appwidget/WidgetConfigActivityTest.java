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
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.BestActivity;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.SearchActivity;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowPreferenceFragmentCompat;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowPreferenceFragmentCompat.class})
@RunWith(TestRunner.class)
public class WidgetConfigActivityTest {
    private ActivityController<WidgetConfigActivity> controller;
    private WidgetConfigActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(WidgetConfigActivity.class);
        activity = controller
                .withIntent(new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 1))
                .create()
                .start()
                .resume()
                .visible()
                .get();
        shadowOf(AppWidgetManager.getInstance(activity))
                .createWidgets(WidgetProvider.class, R.layout.appwidget, 1);
    }

    @Test
    public void testCancel() {
        activity.onBackPressed();
        assertThat(shadowOf(activity).getResultCode()).isEqualTo(Activity.RESULT_CANCELED);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void testOk() {
        activity.findViewById(R.id.button_ok).performClick();
        assertThat(shadowOf(activity).getResultCode()).isEqualTo(Activity.RESULT_OK);
        assertThat(activity).isFinishing();
        assertThat(shadowOf((JobScheduler) activity.getSystemService(Context.JOB_SCHEDULER_SERVICE))
                .getAllPendingJobs()).isNotEmpty();
    }

    @Test
    public void testSectionTop() {
        activity.getSharedPreferences(WidgetHelper.WidgetConfig.getConfigName(1), Context.MODE_PRIVATE)
                .edit()
                .putString(activity.getString(R.string.pref_widget_section),
                        activity.getString(R.string.pref_widget_section_value_top))
                .apply();
        WidgetHelper.WidgetConfig config = WidgetHelper.WidgetConfig.createWidgetConfig(activity, 1);
        assertThat(config.section).isEqualTo(activity.getString(R.string.pref_widget_section_value_top));
        assertThat(config.customQuery).isFalse();
        assertThat(config.destination).isEqualTo(io.github.hidroh.materialistic.ListActivity.class);
        assertThat(config.title).isEqualTo(activity.getString(R.string.title_activity_list));
    }

    @Test
    public void testSectionBest() {
        activity.getSharedPreferences(WidgetHelper.WidgetConfig.getConfigName(1), Context.MODE_PRIVATE)
                .edit()
                .putString(activity.getString(R.string.pref_widget_section),
                        activity.getString(R.string.pref_widget_section_value_best))
                .apply();
        WidgetHelper.WidgetConfig config = WidgetHelper.WidgetConfig.createWidgetConfig(activity, 1);
        assertThat(config.section).isEqualTo(activity.getString(R.string.pref_widget_section_value_best));
        assertThat(config.customQuery).isFalse();
        assertThat(config.destination).isEqualTo(BestActivity.class);
        assertThat(config.title).isEqualTo(activity.getString(R.string.title_activity_best));
    }

    @Test
    public void testCustomQuery() {
        activity.getSharedPreferences(WidgetHelper.WidgetConfig.getConfigName(1), Context.MODE_PRIVATE)
                .edit()
                .putString(activity.getString(R.string.pref_widget_query), "query")
                .apply();
        WidgetHelper.WidgetConfig config = WidgetHelper.WidgetConfig.createWidgetConfig(activity, 1);
        assertThat(config.section).isEqualTo("query");
        assertThat(config.customQuery).isTrue();
        assertThat(config.destination).isEqualTo(SearchActivity.class);
        assertThat(config.title).isEqualTo("query");
    }

    @Test
    public void testTransparentTheme() {
        WidgetHelper.WidgetConfig config = WidgetHelper.WidgetConfig.createWidgetConfig(activity, 1);
        assertThat(config.widgetLayout).isEqualTo(R.layout.appwidget);
        assertThat(config.isLightTheme).isFalse();
    }

    @Test
    public void testLightTheme() {
        activity.getSharedPreferences(WidgetHelper.WidgetConfig.getConfigName(1), Context.MODE_PRIVATE)
                .edit()
                .putString(activity.getString(R.string.pref_widget_theme),
                        activity.getString(R.string.pref_widget_theme_value_light))
                .apply();
        WidgetHelper.WidgetConfig config = WidgetHelper.WidgetConfig.createWidgetConfig(activity, 1);
        assertThat(config.widgetLayout).isEqualTo(R.layout.appwidget_light);
        assertThat(config.isLightTheme).isTrue();
    }

    @Test
    public void testDarkTheme() {
        activity.getSharedPreferences(WidgetHelper.WidgetConfig.getConfigName(1), Context.MODE_PRIVATE)
                .edit()
                .putString(activity.getString(R.string.pref_widget_theme),
                        activity.getString(R.string.pref_widget_theme_value_dark))
                .apply();
        WidgetHelper.WidgetConfig config = WidgetHelper.WidgetConfig.createWidgetConfig(activity, 1);
        assertThat(config.widgetLayout).isEqualTo(R.layout.appwidget_dark);
        assertThat(config.isLightTheme).isFalse();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

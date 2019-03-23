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
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowJobScheduler;

import io.github.hidroh.materialistic.R;
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
        controller = Robolectric.buildActivity(WidgetConfigActivity.class,
                new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 1));
        activity = controller
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
        assertThat(((ShadowJobScheduler.ShadowJobSchedulerImpl)
                shadowOf((JobScheduler) activity.getSystemService(Context.JOB_SCHEDULER_SERVICE)))
                .getAllPendingJobs()).isNotEmpty();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

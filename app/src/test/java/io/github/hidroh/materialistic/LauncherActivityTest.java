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
import android.preference.PreferenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ActivityController;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.materialistic.test.ParameterizedTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(ParameterizedTestRunner.class)
public class LauncherActivityTest {
    private final int choice;
    private final Class<? extends Activity> startedActivity;
    private ActivityController<LauncherActivity> controller;
    private LauncherActivity activity;

    public LauncherActivityTest(int choice, Class<? extends Activity> startedActivity) {
        this.choice = choice;
        this.startedActivity = startedActivity;
    }

    @ParameterizedTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{R.string.pref_launch_screen_value_top, ListActivity.class},
                new Object[]{R.string.pref_launch_screen_value_best, BestActivity.class},
                new Object[]{R.string.pref_launch_screen_value_hot, PopularActivity.class},
                new Object[]{R.string.pref_launch_screen_value_new, NewActivity.class},
                new Object[]{R.string.pref_launch_screen_value_ask, AskActivity.class},
                new Object[]{R.string.pref_launch_screen_value_show, ShowActivity.class},
                new Object[]{R.string.pref_launch_screen_value_jobs, JobsActivity.class},
                new Object[]{R.string.pref_launch_screen_value_saved, FavoriteActivity.class}
        );
    }

    @Before
    public void setUp() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putString(RuntimeEnvironment.application.getString(R.string.pref_launch_screen),
                        RuntimeEnvironment.application.getString(choice))
                .apply();
        controller = Robolectric.buildActivity(LauncherActivity.class).create();
        activity = controller.get();
    }

    @Test
    public void test() {
        assertThat(activity).isFinishing();
        assertEquals(startedActivity.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    @After
    public void tearDown() {
        controller.destroy();
    }
}

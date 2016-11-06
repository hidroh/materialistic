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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.TestRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@RunWith(TestRunner.class)
public class ReleaseNotesActivityTest {
    private ActivityController<ReleaseNotesActivity> controller;
    private ReleaseNotesActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(ReleaseNotesActivity.class);
        activity = controller.create().start().resume().get();
    }

    @Test
    public void testLoveIt() {
        activity.findViewById(R.id.button_rate).performClick();
        assertThat(activity).isFinishing();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testGotIt() {
        activity.findViewById(R.id.button_ok).performClick();
        assertThat(activity).isFinishing();
    }

    @Test
    public void testFirstInstall() {
        Preferences.sReleaseNotesSeen = null;
        assertTrue(Preferences.isReleaseNotesSeen(RuntimeEnvironment.application));
    }

    @Test
    public void testUpdate() throws PackageManager.NameNotFoundException {
        Preferences.sReleaseNotesSeen = null;
        Preferences.reset(RuntimeEnvironment.application);
        PackageInfo info = RuntimeEnvironment.getPackageManager().getPackageInfo(
                RuntimeEnvironment.application.getPackageName(), 0);
        info.firstInstallTime = 0;
        info.lastUpdateTime = 1;
        assertFalse(Preferences.isReleaseNotesSeen(RuntimeEnvironment.application));
    }

    @Test
    public void testUpdateSeen() throws PackageManager.NameNotFoundException {
        Preferences.sReleaseNotesSeen = null;
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putInt(RuntimeEnvironment.application.getString(R.string.pref_latest_release),
                        BuildConfig.LATEST_RELEASE)
                .commit();
        PackageInfo info = RuntimeEnvironment.getPackageManager().getPackageInfo(
                RuntimeEnvironment.application.getPackageName(), 0);
        info.firstInstallTime = 0;
        info.lastUpdateTime = 1;
        assertTrue(Preferences.isReleaseNotesSeen(RuntimeEnvironment.application));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

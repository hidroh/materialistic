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

import android.app.Dialog;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowPreferenceFragmentCompat;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;

@Config(shadows = {ShadowPreferenceFragmentCompat.class})
@RunWith(TestRunner.class)
public class PreferencesActivityTest {
    private TestPreferencesActivity activity;
    private ActivityController<TestPreferencesActivity> controller;

    @Before
    public void setUp() {
        TestApplication.applicationGraph.inject(this);
        controller = Robolectric.buildActivity(TestPreferencesActivity.class,
                new Intent()
                        .putExtra(PreferencesActivity.EXTRA_TITLE, R.string.display)
                        .putExtra(PreferencesActivity.EXTRA_PREFERENCES, R.xml.preferences_display));
        activity = controller.create().postCreate(null).start().resume().visible().get();
    }

    @Test
    public void testPrefTheme() {
        String key = activity.getString(R.string.pref_theme);
        // trigger listener
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(key, "dark")
                .apply();
        assertTrue(activity.recreated);
    }

    @Test
    public void testHelp() {
        ((PreferencesActivity.SettingsFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(PreferencesActivity.SettingsFragment.class.getName()))
                .getPreferenceScreen()
                .findPreference(activity.getString(R.string.pref_volume_help))
                .performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertThat((TextView) dialog.findViewById(R.id.alertTitle))
                .hasText(R.string.pref_volume_title);
    }

    @Test
    public void testLazyLoadHelp() {
        ((PreferencesActivity.SettingsFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(PreferencesActivity.SettingsFragment.class.getName()))
                .getPreferenceScreen()
                .findPreference(activity.getString(R.string.pref_lazy_load_help))
                .performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertThat((TextView) dialog.findViewById(R.id.alertTitle))
                .hasText(R.string.pref_lazy_load_title);
    }

    public void tearDown() {
        AlgoliaClient.sSortByTime = true;
        controller.pause().stop().destroy();
    }

    static class TestPreferencesActivity extends PreferencesActivity {
        boolean recreated;

        @Override
        public void recreate() {
            recreated = true;
        }
    }
}

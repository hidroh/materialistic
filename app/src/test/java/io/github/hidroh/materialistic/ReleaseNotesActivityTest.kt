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

package io.github.hidroh.materialistic

import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.view.View
import junit.framework.Assert.*
import org.assertj.android.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = InjectableApplication::class)
class ReleaseNotesActivityTest {
  private lateinit var controller: ActivityController<ReleaseNotesActivity>
  private lateinit var activity: ReleaseNotesActivity

  @Before
  fun setUp() {
    controller = Robolectric.buildActivity(ReleaseNotesActivity::class.java)
    activity = controller.create().get()
  }

  @Test
  fun testLoveIt() {
    activity.findViewById<View>(R.id.button_rate).performClick()
    assertThat(activity).isFinishing
    assertNotNull(shadowOf(activity).nextStartedActivity)
  }

  @Test
  fun testGotIt() {
    activity.findViewById<View>(R.id.button_ok).performClick()
    assertThat(activity).isFinishing
  }

  @Test
  fun testFirstInstall() {
    Preferences.sReleaseNotesSeen = null
    assertTrue(Preferences.isReleaseNotesSeen(RuntimeEnvironment.application))
  }

  @Test
  @Throws(PackageManager.NameNotFoundException::class)
  fun testUpdate() {
    Preferences.sReleaseNotesSeen = null
    Preferences.reset(RuntimeEnvironment.application)
    val info = shadowOf(RuntimeEnvironment.application.packageManager)
        .getInternalMutablePackageInfo(RuntimeEnvironment.application.packageName)
    info.firstInstallTime = 0
    info.lastUpdateTime = 1
    assertFalse(Preferences.isReleaseNotesSeen(RuntimeEnvironment.application))
  }

  @Test
  @Throws(PackageManager.NameNotFoundException::class)
  fun testUpdateSeen() {
    Preferences.sReleaseNotesSeen = null
    PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
        .edit()
        .putInt(RuntimeEnvironment.application.getString(R.string.pref_latest_release),
            BuildConfig.LATEST_RELEASE)
        .commit()
    RuntimeEnvironment.application.packageManager.getPackageInfo(
        RuntimeEnvironment.application.packageName, 0).run {
      firstInstallTime = 0
      lastUpdateTime = 1
    }
    assertTrue(Preferences.isReleaseNotesSeen(RuntimeEnvironment.application))
  }
}

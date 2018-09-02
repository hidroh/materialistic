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

import android.app.Activity
import android.preference.PreferenceManager
import junit.framework.Assert.assertEquals
import org.assertj.android.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class LauncherActivityTest(private val choice: Int, private val startedActivity: Class<out Activity>) {
  private lateinit var controller: ActivityController<LauncherActivity>
  private lateinit var activity: LauncherActivity
  private val context = RuntimeEnvironment.application

  @Before
  fun setUp() {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(context.getString(R.string.pref_launch_screen),
            context.getString(choice))
        .apply()
    controller = Robolectric.buildActivity(LauncherActivity::class.java).create()
    activity = controller.get()
  }

  @Test
  fun test() {
    assertThat(activity).isFinishing
    assertEquals(startedActivity.name, shadowOf(activity).nextStartedActivity.component!!.className)
  }

  @After
  fun tearDown() {
    controller.destroy()
  }

  companion object {
    @JvmStatic @ParameterizedRobolectricTestRunner.Parameters
    fun provideParameters(): Collection<Array<Any>> {
      return listOf(
          arrayOf<Any>(R.string.pref_launch_screen_value_top, ListActivity::class.java),
          arrayOf<Any>(R.string.pref_launch_screen_value_best, BestActivity::class.java),
          arrayOf<Any>(R.string.pref_launch_screen_value_hot, PopularActivity::class.java),
          arrayOf<Any>(R.string.pref_launch_screen_value_new, NewActivity::class.java),
          arrayOf<Any>(R.string.pref_launch_screen_value_ask, AskActivity::class.java),
          arrayOf<Any>(R.string.pref_launch_screen_value_show, ShowActivity::class.java),
          arrayOf<Any>(R.string.pref_launch_screen_value_jobs, JobsActivity::class.java),
          arrayOf<Any>(R.string.pref_launch_screen_value_saved, FavoriteActivity::class.java)
      )
    }
  }
}

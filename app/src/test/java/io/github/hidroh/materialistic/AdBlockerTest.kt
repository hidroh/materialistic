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

import android.annotation.SuppressLint
import io.github.hidroh.materialistic.test.TestRunner
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import okio.Okio
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import rx.schedulers.Schedulers
import java.io.IOException

@RunWith(TestRunner::class)
class AdBlockerTest {
  @Test
  fun testBlockAd() {
    AdBlocker.init(RuntimeEnvironment.application, Schedulers.immediate())
    assertFalse(AdBlocker.isAd(""))
    assertFalse(AdBlocker.isAd("http://localhost"))
    assertFalse(AdBlocker.isAd("http://google.com"))
    assertTrue(AdBlocker.isAd("http://pagead2.g.doubleclick.net"))
  }

  @SuppressLint("NewApi")
  @Test
  @Throws(IOException::class)
  fun testCreateEmptyResource() {
    val resource = AdBlocker.createEmptyResource()
    assertThat(Okio.buffer(Okio.source(resource.data)).readUtf8()).isEmpty()
  }
}

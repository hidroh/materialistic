/*
 * Copyright (c) 2018 Ha Duy Trung
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

package io.github.hidroh.materialistic.data

import io.github.hidroh.materialistic.test.InMemoryCache
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import rx.schedulers.Schedulers

@RunWith(JUnit4::class)
class SessionManagerTest {
  private lateinit var manager: SessionManager

  @Before
  fun setUp() {
    val cache = InMemoryCache()
    cache.setViewed("1")
    cache.setViewed("2")
    manager = SessionManager(Schedulers.immediate(), cache)
  }

  @Test
  fun testIsViewedNull() {
    assertThat(manager.isViewed(null).toBlocking().single()).isFalse()
  }

  @Test
  fun testIsViewedTrue() {
    assertThat(manager.isViewed("1").toBlocking().single()).isTrue()
  }

  @Test
  fun testIsViewedFalse() {
    assertThat(manager.isViewed("-1").toBlocking().single()).isFalse()
  }

  @Test
  fun testView() {
    assertThat(manager.isViewed("3").toBlocking().single()).isFalse()
    manager.view("3")
    assertThat(manager.isViewed("3").toBlocking().single()).isTrue()
  }
}

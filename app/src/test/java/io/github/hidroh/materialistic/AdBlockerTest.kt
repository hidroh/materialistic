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
import android.content.Context
import android.content.res.AssetManager
import okio.Okio
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import rx.schedulers.Schedulers
import java.io.FileInputStream
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(application = InjectableApplication::class)
class AdBlockerTest {
  @Test
  fun testBlockAd() {
    val assetManager = mock(AssetManager::class.java)
    `when`(assetManager.open(anyString()))
        .thenReturn(FileInputStream(javaClass.classLoader.getResource("pgl.yoyo.org.txt").file))
    val context = mock(Context::class.java)
    `when`(context.assets).thenReturn(assetManager)
    AdBlocker.init(context, Schedulers.immediate())
    assertThat(AdBlocker.isAd("")).isFalse()
    assertThat(AdBlocker.isAd("http://localhost")).isFalse()
    assertThat(AdBlocker.isAd("http://google.com")).isFalse()
    assertThat(AdBlocker.isAd("http://pagead2.g.doubleclick.net")).isTrue()
  }

  @SuppressLint("NewApi")
  @Test
  @Throws(IOException::class)
  fun testCreateEmptyResource() {
    val resource = AdBlocker.createEmptyResource()
    assertThat(Okio.buffer(Okio.source(resource.data)).readUtf8()).isEmpty()
  }
}

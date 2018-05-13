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

package io.github.hidroh.materialistic.data.android

import android.net.Uri
import io.github.hidroh.materialistic.data.MaterialisticDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import rx.schedulers.Schedulers

@RunWith(JUnit4::class)
class CacheTest {
  @Mock private lateinit var database: MaterialisticDatabase
  @Mock private lateinit var readStoriesDao: MaterialisticDatabase.ReadStoriesDao
  @Mock private lateinit var savedStoriesDao: MaterialisticDatabase.SavedStoriesDao
  @Mock private lateinit var readableDao: MaterialisticDatabase.ReadableDao
  private lateinit var cache: Cache

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    cache = Cache(database, savedStoriesDao, readStoriesDao, readableDao, Schedulers.immediate())
  }

  @Test
  fun testGetReadabilityNoItem() {
    `when`(readableDao.selectByItemId(anyString())).thenReturn(null)
    assertThat(cache.getReadability("1")).isNull()
  }

  @Test
  fun testGetReadabilityNoContent() {
    `when`(readableDao.selectByItemId(anyString())).thenReturn(MaterialisticDatabase.Readable("1", null))
    assertThat(cache.getReadability("1")).isNull()
  }

  @Test
  fun testGetReadabilityWithContent() {
    `when`(readableDao.selectByItemId(anyString())).thenReturn(MaterialisticDatabase.Readable("1", "content"))
    assertThat(cache.getReadability("1")).isEqualTo("content")
  }

  @Test
  fun testPutReadability() {
    cache.putReadability("1", "content")
    verify(readableDao).insert(eq(MaterialisticDatabase.Readable("1", "content")))
  }

  @Test
  fun testIsViewedNoItem() {
    `when`(readStoriesDao.selectByItemId(anyString())).thenReturn(null)
    assertThat(cache.isViewed("1")).isFalse()
  }

  @Test
  fun testIsViewedWithItem() {
    `when`(readStoriesDao.selectByItemId(anyString())).thenReturn(MaterialisticDatabase.ReadStory("1"))
    assertThat(cache.isViewed("1")).isTrue()
  }

  @Test
  fun testSetViewed() {
    val uri = mock(Uri::class.java)
    `when`(database.createReadUri(anyString())).thenReturn(uri)
    cache.setViewed("1")
    verify(readStoriesDao).insert(eq(MaterialisticDatabase.ReadStory("1")))
    verify(database).setLiveValue(eq(uri))
  }

  @Test
  fun testIsFavoriteNoItem() {
    `when`(savedStoriesDao.selectByItemId(anyString())).thenReturn(null)
    assertThat(cache.isFavorite("1")).isFalse()
  }

  @Test
  fun testIsFavoriteWithItem() {
    val savedStory = mock(MaterialisticDatabase.SavedStory::class.java)
    `when`(savedStoriesDao.selectByItemId(anyString())).thenReturn(savedStory)
    assertThat(cache.isFavorite("1")).isTrue()
  }
}

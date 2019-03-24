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

import android.accounts.Account
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.preference.PreferenceManager
import androidx.lifecycle.Observer
import io.github.hidroh.materialistic.BuildConfig
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.data.android.Cache
import io.github.hidroh.materialistic.test.InMemoryDatabase
import io.github.hidroh.materialistic.test.TestRunner
import io.github.hidroh.materialistic.test.TestWebItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowNetworkInfo
import rx.schedulers.Schedulers

@RunWith(TestRunner::class)
class FavoriteManagerTest {
  private val context = RuntimeEnvironment.application
  private val database = InMemoryDatabase.getInstance(RuntimeEnvironment.application)
  private val savedStoriesDao = database.savedStoriesDao
  private val readStoriesDao = database.readStoriesDao
  private val readableDao = database.readableDao
  private lateinit var manager: FavoriteManager
  @Mock private lateinit var observer: Observer<Uri>

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    savedStoriesDao.insert(MaterialisticDatabase.SavedStory.from(object : TestWebItem() {
      override fun getDisplayedTitle() = "title"
      override fun getUrl() = "http://example.com"
      override fun getId() = "1"
    }))
    savedStoriesDao.insert(MaterialisticDatabase.SavedStory.from(object : TestWebItem() {
      override fun getDisplayedTitle() = "ask HN"
      override fun getUrl() = "http://example.com"
      override fun getId() = "2"
    }))
    val cache = Cache(database, savedStoriesDao, readStoriesDao, readableDao, Schedulers.immediate())
    manager = FavoriteManager(cache, Schedulers.immediate(), savedStoriesDao)
    MaterialisticDatabase.getInstance(context).liveData.observeForever(observer)
  }

  @Test
  fun testLocalItemManager() {
    val observer = mock(LocalItemManager.Observer::class.java)
    manager.attach(observer, null)
    verify(observer).onChanged()
    assertThat(manager.size).isEqualTo(2)
    assertThat(manager.getItem(0)!!.displayedTitle).contains("ask HN")
    assertThat(manager.getItem(1)!!.displayedTitle).contains("title")
    manager.detach()
  }

  @Test
  fun testExportNoQuery() {
    manager.export(context, null)
    val notifications = shadowOf(context
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .allNotifications
    assertThat(notifications).isNotEmpty()
    assertThat(shadowOf(notifications[0].contentIntent).savedIntent.action).isEqualTo(Intent.ACTION_CHOOSER)
  }

  @Test
  fun testExportEmpty() {
    manager.export(context, "blah")
    assertThat(shadowOf(context
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .allNotifications)
        .isEmpty()
  }

  @Test
  fun testCheckNoId() {
    assertThat(manager.check(null).toBlocking().single()).isFalse()
  }

  @Test
  fun testCheckTrue() {
    assertThat(manager.check("1").toBlocking().single()).isTrue()
  }

  @Test
  fun testCheckFalse() {
    assertThat(manager.check("-1").toBlocking().single()).isFalse()
  }

  @Test @Config(sdk = [18])
  fun testAdd() {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean(context.getString(R.string.pref_saved_item_sync), true)
        .putBoolean(context.getString(R.string.pref_offline_article), true)
        .apply()
    shadowOf(context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
        ConnectivityManager.TYPE_WIFI, 0, true, NetworkInfo.State.CONNECTED))
    manager.add(context, object : TestWebItem() {
      override fun getDisplayedTitle() = "new title"
      override fun getUrl() = "http://newitem.com"
      override fun getId() = "3"
    })
    verify(observer).onChanged(eq(Uri.parse("content://${BuildConfig.APPLICATION_ID}/saved/add/3")))
    assertThat(ShadowContentResolver.getStatus(Account("Materialistic", BuildConfig.APPLICATION_ID),
        SyncContentProvider.PROVIDER_AUTHORITY).syncRequests).isGreaterThan(0)
  }

  @Test
  fun testReAdd() {
    val favorite = mock(Favorite::class.java)
    `when`(favorite.id).thenReturn("3")
    `when`(favorite.url).thenReturn("http://example.com")
    `when`(favorite.displayedTitle).thenReturn("title")
    manager.add(context, favorite)
    verify(observer).onChanged(eq(Uri.parse("content://${BuildConfig.APPLICATION_ID}/saved/add/3")))
  }

  @Test
  fun testClearAll() {
    manager.clear(context, null)
    verify(observer).onChanged(eq(Uri.parse("content://${BuildConfig.APPLICATION_ID}/saved/clear")))
  }

  @Test
  fun testClearQuery() {
    manager.clear(context, "blah")
    verify(observer).onChanged(eq(Uri.parse("content://${BuildConfig.APPLICATION_ID}/saved/clear")))
  }

  @Test
  fun testRemoveWithId() {
    manager.remove(context, "1")
    verify(observer).onChanged(eq(Uri.parse("content://${BuildConfig.APPLICATION_ID}/saved/remove/1")))
  }

  @Test
  fun testRemoveMultipleNoId() {
    manager.remove(context, null as Set<String>?)
    verify(observer, never()).onChanged(any(Uri::class.java))
  }

  @Test
  fun testRemoveMultipleWithId() {
    manager.remove(context, hashSetOf("1", "2"))
    verify(observer).onChanged(eq(Uri.parse("content://${BuildConfig.APPLICATION_ID}/saved/remove/1")))
    verify(observer).onChanged(eq(Uri.parse("content://${BuildConfig.APPLICATION_ID}/saved/remove/2")))
  }

  @After
  fun tearDown() {
    MaterialisticDatabase.getInstance(context).liveData.removeObserver(observer)
  }
}

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

import io.github.hidroh.materialistic.DataModule
import io.github.hidroh.materialistic.data.LocalCache
import io.github.hidroh.materialistic.data.MaterialisticDatabase
import rx.Observable
import rx.Scheduler
import javax.inject.Inject
import javax.inject.Named

class Cache @Inject constructor(
    private val database: MaterialisticDatabase,
    private val savedStoriesDao: MaterialisticDatabase.SavedStoriesDao,
    private val readStoriesDao: MaterialisticDatabase.ReadStoriesDao,
    private val readableDao: MaterialisticDatabase.ReadableDao,
    @Named(DataModule.MAIN_THREAD) private val mainScheduler: Scheduler) : LocalCache {

  override fun getReadability(itemId: String?) = readableDao.selectByItemId(itemId)?.content

  override fun putReadability(itemId: String?, content: String?) {
    readableDao.insert(MaterialisticDatabase.Readable(itemId, content))
  }

  override fun isViewed(itemId: String?) = readStoriesDao.selectByItemId(itemId) != null

  override fun setViewed(itemId: String?) {
    readStoriesDao.insert(MaterialisticDatabase.ReadStory(itemId))
    Observable.just(itemId)
        .map { database.createReadUri(it) }
        .observeOn(mainScheduler)
        .subscribe { database.setLiveValue(it) }
  }

  override fun isFavorite(itemId: String?) = savedStoriesDao.selectByItemId(itemId) != null
}

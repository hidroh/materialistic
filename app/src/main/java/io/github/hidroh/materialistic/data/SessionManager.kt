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

import androidx.annotation.WorkerThread
import io.github.hidroh.materialistic.DataModule
import rx.Observable
import rx.Scheduler
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Data repository for session state
 */
@Singleton
class SessionManager @Inject constructor(
    @Named(DataModule.IO_THREAD)
    private val ioScheduler: Scheduler,
    private val cache: LocalCache) {

  @WorkerThread
  fun isViewed(itemId: String?): Observable<Boolean> = Observable.just(
      if (itemId.isNullOrEmpty()) {
        false
      } else {
        cache.isViewed(itemId)
      })

  /**
   * Marks an item as already being viewed
   * @param itemId    item ID that has been viewed
   */
  fun view(itemId: String?) {
    if (itemId.isNullOrEmpty()) return
    Observable.defer { Observable.just(itemId) }
        .subscribeOn(ioScheduler)
        .observeOn(ioScheduler)
        .subscribe { cache.setViewed(it) }
  }
}

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

import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

@WorkerThread
interface LocalCache {
  @Nullable
  fun getReadability(itemId: String?): String?

  fun putReadability(itemId: String?, content: String?)

  fun isViewed(itemId: String?): Boolean

  fun setViewed(itemId: String?)

  fun isFavorite(itemId: String?): Boolean
}

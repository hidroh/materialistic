/*
 * Copyright (c) 2015 Ha Duy Trung
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

package io.github.hidroh.materialistic.data;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.github.hidroh.materialistic.AndroidUtils;
import io.github.hidroh.materialistic.DataModule;
import rx.Observable;
import rx.Scheduler;

/**
 * Data repository for session state
 */
@Singleton
public class SessionManager {

    @Inject @Named(DataModule.IO_THREAD) Scheduler mIoScheduler;
    @Inject LocalCache mCache;

    @Inject
    public SessionManager() {
    }

    @WorkerThread
    @NonNull
    Observable<Boolean> isViewed(String itemId) {
        if (AndroidUtils.TextUtils.isEmpty(itemId)) {
            return Observable.just(false);
        }
        boolean result = mCache.isViewed(itemId);
        return Observable.just(result);
    }

    /**
     * Marks an item as already being viewed
     * @param itemId    item ID that has been viewed
     */
    public void view(final String itemId) {
        if (AndroidUtils.TextUtils.isEmpty(itemId)) {
            return;
        }
        Observable.defer(() -> Observable.just(itemId))
                .subscribeOn(mIoScheduler)
                .observeOn(mIoScheduler)
                .subscribe(id -> mCache.setViewed(id));
    }
}

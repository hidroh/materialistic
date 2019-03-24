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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.AndroidUtils;
import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.DataModule;
import io.github.hidroh.materialistic.annotation.Synthetic;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public interface ReadabilityClient {
    String HOST = "mercury.postlight.com";

    interface Callback {
        void onResponse(String content);
    }

    void parse(String itemId, String url, Callback callback);

    @WorkerThread
    void parse(String itemId, String url);

    class Impl implements ReadabilityClient {
        private static final CharSequence EMPTY_CONTENT = "<div></div>";
        private final MercuryService mMercuryService;
        private final LocalCache mCache;
        @Inject @Named(DataModule.IO_THREAD) Scheduler mIoScheduler;
        @Inject @Named(DataModule.MAIN_THREAD) Scheduler mMainThreadScheduler;

        interface MercuryService {
            String MERCURY_API_URL = "https://" + HOST + "/";
            String X_API_KEY = "x-api-key: ";

            @Headers({RestServiceFactory.CACHE_CONTROL_MAX_AGE_24H,
                    X_API_KEY + BuildConfig.MERCURY_TOKEN})
            @GET("parser")
            Observable<Readable> parse(@Query("url") String url);
        }

        class Readable {
            @Keep @Synthetic
            String content;
        }

        @Inject
        public Impl(LocalCache cache, RestServiceFactory factory) {
            mMercuryService = factory.rxEnabled(true)
                    .create(MercuryService.MERCURY_API_URL,
                            MercuryService.class);
            mCache = cache;
        }

        @Override
        public void parse(String itemId, String url, Callback callback) {
            Observable.defer(() -> fromCache(itemId))
                    .subscribeOn(mIoScheduler)
                    .flatMap(content -> content != null ?
                            Observable.just(content) : fromNetwork(itemId, url))
                    .map(content -> AndroidUtils.TextUtils.equals(EMPTY_CONTENT, content) ? null : content)
                    .observeOn(mMainThreadScheduler)
                    .subscribe(callback::onResponse);
        }

        @WorkerThread
        @Override
        public void parse(String itemId, String url) {
            Observable.defer(() -> fromCache(itemId))
                    .subscribeOn(Schedulers.immediate())
                    .switchIfEmpty(fromNetwork(itemId, url))
                    .map(content -> AndroidUtils.TextUtils.equals(EMPTY_CONTENT, content) ? null : content)
                    .observeOn(Schedulers.immediate())
                    .subscribe();
        }

        @NonNull
        private Observable<String> fromNetwork(String itemId, String url) {
            return mMercuryService.parse(url)
                    .onErrorReturn(throwable -> null)
                    .map(readable -> readable == null ? null : readable.content)
                    .doOnNext(content -> mCache.putReadability(itemId, content));
        }

        private Observable<String> fromCache(String itemId) {
            return Observable.just(mCache.getReadability(itemId));
        }
    }
}

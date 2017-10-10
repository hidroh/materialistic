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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.DataModule;
import io.github.hidroh.materialistic.annotation.Synthetic;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
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
        private final ContentResolver mContentResolver;
        private final Scheduler mIoScheduler;

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
        public Impl(Context context, RestServiceFactory factory,
                    @Named(DataModule.IO_THREAD) Scheduler ioScheduler) {
            mMercuryService = factory.rxEnabled(true)
                    .create(MercuryService.MERCURY_API_URL,
                            MercuryService.class);
            mContentResolver = context.getContentResolver();
            mIoScheduler = ioScheduler;
        }

        @Override
        public void parse(String itemId, String url, Callback callback) {
            Observable.defer(() -> fromCache(itemId))
                    .subscribeOn(mIoScheduler)
                    .flatMap(content -> content != null ?
                            Observable.just(content) : fromNetwork(itemId, url))
                    .map(content -> TextUtils.equals(EMPTY_CONTENT, content) ? null : content)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(callback::onResponse);
        }

        @WorkerThread
        @Override
        public void parse(String itemId, String url) {
            Observable.defer(() -> fromCache(itemId))
                    .subscribeOn(Schedulers.immediate())
                    .switchIfEmpty(fromNetwork(itemId, url))
                    .map(content -> TextUtils.equals(EMPTY_CONTENT, content) ? null : content)
                    .observeOn(Schedulers.immediate())
                    .subscribe();
        }

        @NonNull
        private Observable<String> fromNetwork(String itemId, String url) {
            return mMercuryService.parse(url)
                    .onErrorReturn(throwable -> null)
                    .map(readable -> readable == null ? null : readable.content)
                    .doOnNext(content -> cache(itemId, content));
        }

        private Observable<String> fromCache(String itemId) {
            Cursor cursor = mContentResolver.query(MaterialisticProvider.URI_READABILITY,
                    new String[]{MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_CONTENT},
                    MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_ITEM_ID + " = ?",
                    new String[]{itemId}, null);
            String content = null;
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    content = cursor.getString(cursor.getColumnIndexOrThrow(
                            MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_CONTENT));
                }
                cursor.close();
            }
            return Observable.just(content);
        }

        @WorkerThread
        private void cache(String itemId, String content) {
            final ContentValues contentValues = new ContentValues();
            contentValues.put(MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_ITEM_ID, itemId);
            contentValues.put(MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_CONTENT, content);
            mContentResolver.insert(MaterialisticProvider.URI_READABILITY, contentValues);
        }
    }
}

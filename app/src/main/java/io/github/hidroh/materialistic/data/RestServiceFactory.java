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

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

import javax.inject.Inject;

import okhttp3.Call;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

public interface RestServiceFactory {
    String CACHE_CONTROL_FORCE_CACHE = "Cache-Control: only-if-cached, max-stale=" + Integer.MAX_VALUE;
    String CACHE_CONTROL_FORCE_NETWORK = "Cache-Control: no-cache";
    String CACHE_CONTROL_MAX_AGE_30M = "Cache-Control: max-age=" + (30 * 60);
    String CACHE_CONTROL_MAX_AGE_24H = "Cache-Control: max-age=" + (24 * 60 * 60);

    RestServiceFactory rxEnabled(boolean enabled);

    <T> T create(String baseUrl, Class<T> clazz);

    <T> T create(String baseUrl, Class<T> clazz, Executor callbackExecutor);

    class Impl implements RestServiceFactory {
        private final Call.Factory mCallFactory;
        private boolean mRxEnabled;

        @Inject
        public Impl(Call.Factory callFactory) {
            this.mCallFactory = callFactory;
        }

        @Override
        public RestServiceFactory rxEnabled(boolean enabled) {
            mRxEnabled = enabled;
            return this;
        }

        @Override
        public <T> T create(String baseUrl, Class<T> clazz) {
            return create(baseUrl, clazz, null);
        }

        @Override
        public <T> T create(String baseUrl, Class<T> clazz, Executor callbackExecutor) {
            Retrofit.Builder builder = new Retrofit.Builder();
            if (mRxEnabled) {
                builder.addCallAdapterFactory(RxJavaCallAdapterFactory
                        .createWithScheduler(Schedulers.io()));
            }
            builder.callFactory(mCallFactory)
                    .callbackExecutor(callbackExecutor != null ?
                            callbackExecutor : new MainThreadExecutor());
            return builder.baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(clazz);
        }
    }

    class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override public void execute(@NonNull Runnable r) {
            handler.post(r);
        }
    }
}

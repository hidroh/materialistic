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

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import io.github.hidroh.materialistic.BuildConfig;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

public interface RestServiceFactory {
    <T> T create(String baseUrl, Class<T> clazz);

    class Impl implements RestServiceFactory {
        private static final String TAG_OK_HTTP = "OkHttp";
        private static final long CACHE_SIZE = 1024 * 1024;
        private final RestAdapter.Builder mBuilder;

        public Impl(Context context) {
            final OkHttpClient okHttpClient = new OkHttpClient();
            HttpLoggingInterceptor interceptor =
                    new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                        @Override
                        public void log(String message) {
                            Log.d(TAG_OK_HTTP, message);
                        }
                    });
            interceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY :
                    HttpLoggingInterceptor.Level.NONE);
            okHttpClient.networkInterceptors().add(interceptor);
            okHttpClient.setCache(new Cache(context.getApplicationContext().getCacheDir(),
                    CACHE_SIZE));

            RestAdapter.Builder builder = new RestAdapter.Builder()
                    .setClient(new OkClient(okHttpClient));
            builder.setLogLevel(BuildConfig.DEBUG ? RestAdapter.LogLevel.BASIC :
                    RestAdapter.LogLevel.NONE);
            mBuilder = builder;
        }

        @Override
        public <T> T create(String baseUrl, Class<T> clazz) {
            return mBuilder
                    .setEndpoint(baseUrl)
                    .build()
                    .create(clazz);
        }
    }
}

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

import javax.inject.Inject;

import okhttp3.Call;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Retrofit;

public interface RestServiceFactory {
    String CACHE_CONTROL_FORCE_CACHE = "Cache-Control: only-if-cached, max-stale=" + Integer.MAX_VALUE;
    String CACHE_CONTROL_FORCE_NETWORK = "Cache-Control: no-cache";
    String CACHE_CONTROL_MAX_AGE_1H = "Cache-Control: max-age=3600";
    String CACHE_CONTROL_MAX_AGE_24H = "Cache-Control: max-age=86400";

    <T> T create(String baseUrl, Class<T> clazz);

    class Impl implements RestServiceFactory {
        private final Call.Factory mCallFactory;

        @Inject
        public Impl(Call.Factory callFactory) {
            this.mCallFactory = callFactory;
        }

        @Override
        public <T> T create(String baseUrl, Class<T> clazz) {
            return new Retrofit.Builder()
                    .callFactory(mCallFactory)
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(clazz);
        }
    }
}

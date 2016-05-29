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
import android.os.AsyncTask;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.io.IOException;

import javax.inject.Inject;

import io.github.hidroh.materialistic.BuildConfig;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface ReadabilityClient {
    String HOST = "readability.com";

    interface Callback {
        void onResponse(String content);
    }

    void parse(String itemId, String url, Callback callback);

    @WorkerThread
    String parse(String itemId, String url);

    class Impl implements ReadabilityClient {
        private static final CharSequence EMPTY_CONTENT = "<div></div>";
        private final ReadabilityService mReadabilityService;
        private final ContentResolver mContentResolver;

        interface ReadabilityService {
            String READABILITY_API_URL = "https://" + HOST + "/api/content/v1/";

            @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_24H)
            @GET("parser?token=" + BuildConfig.READABILITY_TOKEN)
            Call<Readable> parse(@Query("url") String url);
        }

        static class Readable {
            private String content;
        }

        @Inject
        Impl(Context context, RestServiceFactory factory) {
            mReadabilityService = factory.create(ReadabilityService.READABILITY_API_URL,
                    ReadabilityService.class);
            mContentResolver = context.getContentResolver();
        }

        @Override
        public void parse(final String itemId, final String url, final Callback callback) {
            new AsyncTask<Void, Void, String>() {

                @Override
                protected String doInBackground(Void... params) {
                    return parse(itemId, url);
                }

                @Override
                protected void onPostExecute(String content) {
                    callback.onResponse(content);
                }
            }.execute();
        }

        @WorkerThread
        @Override
        public String parse(String itemId, String url) {
            Cursor cursor = mContentResolver.query(MaterialisticProvider.URI_READABILITY,
                    new String[]{MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_CONTENT},
                    MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_ITEM_ID + " = ?",
                    new String[]{itemId}, null);
            String content;
            if (cursor == null || !cursor.moveToFirst()) {
                content = readabilityParse(itemId, url);
            } else {
                content = cursor.getString(cursor.getColumnIndexOrThrow(
                        MaterialisticProvider.ReadabilityEntry.COLUMN_NAME_CONTENT));
                content = TextUtils.equals(EMPTY_CONTENT, content) ? null : content;
            }
            if (cursor != null) {
                cursor.close();
            }
            return content;
        }

        @WorkerThread
        private String readabilityParse(String itemId, String url) {
            try {
                Readable readable;
                if ((readable = mReadabilityService.parse(url).execute().body()) != null) {
                    cache(itemId, readable.content);
                    if (!TextUtils.equals(EMPTY_CONTENT, readable.content)) {
                        return readable.content;
                    }
                }
            } catch (IOException e) {
                // no op
            }
            return null;
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

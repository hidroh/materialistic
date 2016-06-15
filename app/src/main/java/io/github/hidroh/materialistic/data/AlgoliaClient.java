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

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.Preferences;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class AlgoliaClient implements ItemManager {

    public static boolean sSortByTime = true;
    public static final String HOST = "hn.algolia.com";
    private static final String BASE_API_URL = "https://" + HOST + "/api/v1/";
    protected RestService mRestService;
    @Inject @Named(ActivityModule.HN) ItemManager mHackerNewsClient;

    @Inject
    public AlgoliaClient(Context context, RestServiceFactory factory) {
        mRestService = factory.create(BASE_API_URL, RestService.class);
        sSortByTime = Preferences.isSortByRecent(context);
    }

    @Override
    public void getStories(String filter, @CacheMode int cacheMode,
                           final ResponseListener<Item[]> listener) {
        if (listener == null) {
            return;
        }
        search(filter, new Callback<AlgoliaHits>() {
            @Override
            public void onResponse(Call<AlgoliaHits> call, Response<AlgoliaHits> response) {
                AlgoliaHits algoliaHits = response.body();
                if (algoliaHits == null) {
                    listener.onResponse(new Item[0]);
                    return;
                }
                Hit[] hits = algoliaHits.hits;
                Item[] stories = new Item[hits == null ? 0 : hits.length];
                for (int i = 0; i < stories.length; i++) {
                    HackerNewsItem item = new HackerNewsItem(
                            Long.parseLong(hits[i].objectID));
                    item.rank = i + 1;
                    stories[i] = item;
                }
                listener.onResponse(stories);
            }

            @Override
            public void onFailure(Call<AlgoliaHits> call, Throwable t) {
                listener.onError(t != null ? t.getMessage() : "");
            }
        });
    }

    @Override
    public void getItem(String itemId, @CacheMode int cacheMode, ResponseListener<Item> listener) {
        mHackerNewsClient.getItem(itemId, cacheMode, listener);
    }

    @Override
    public Item[] getStories(String filter, @CacheMode int cacheMode) {
        return new Item[0]; // not applicable
    }

    @Override
    public Item getItem(String itemId, @CacheMode int cacheMode) {
        return null; // not applicable
    }

    protected void search(String filter, Callback<AlgoliaHits> callback) {
        // TODO add ETag header
        Call<AlgoliaHits> call;
        if (sSortByTime) {
            call = mRestService.searchByDate(filter);
        } else {
            call = mRestService.search(filter);
        }
        call.enqueue(callback);
    }

    interface RestService {
        @GET("search_by_date?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        Call<AlgoliaHits> searchByDate(@Query("query") String query);

        @GET("search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        Call<AlgoliaHits> search(@Query("query") String query);

        @GET("search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        Call<AlgoliaHits> searchByMinTimestamp(@Query("numericFilters") String timestampSeconds);
    }

    protected static class AlgoliaHits {
        Hit[] hits;
    }

    private static class Hit {
        String objectID;
    }
}

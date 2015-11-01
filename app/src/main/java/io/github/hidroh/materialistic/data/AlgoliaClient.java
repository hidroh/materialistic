package io.github.hidroh.materialistic.data;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.Preferences;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Query;

public class AlgoliaClient implements ItemManager {

    public static boolean sSortByTime = true;
    private static final String BASE_API_URL = "https://hn.algolia.com/api/v1";
    protected RestService mRestService;
    @Inject @Named(ActivityModule.HN) ItemManager mHackerNewsClient;

    @Inject
    public AlgoliaClient(Context context, RestServiceFactory factory) {
        mRestService = factory.create(BASE_API_URL, RestService.class);
        sSortByTime = Preferences.isSortByRecent(context);
    }

    @Override
    public void getStories(String filter, final ResponseListener<Item[]> listener) {
        if (listener == null) {
            return;
        }

        Callback<AlgoliaHits> callback = new Callback<AlgoliaHits>() {
            @Override
            public void success(AlgoliaHits algoliaHits, Response response) {
                Hit[] hits = algoliaHits.hits;
                Item[] stories = new Item[hits == null ? 0 : hits.length];
                for (int i = 0; i < stories.length; i++) {
                    stories[i] = new HackerNewsClient.HackerNewsItem(Long.parseLong(hits[i].objectID));
                }
                listener.onResponse(stories);
            }

            @Override
            public void failure(RetrofitError error) {
                listener.onError(error != null ? error.getMessage() : "");
            }
        };
        search(filter, callback);
    }

    @Override
    public void getItem(String itemId, ResponseListener<Item> listener) {
        mHackerNewsClient.getItem(itemId, listener);
    }

    protected void search(String filter, Callback<AlgoliaHits> callback) {
        if (sSortByTime) {
            mRestService.searchByDate(filter, callback);
        } else {
            mRestService.search(filter, callback);
        }
    }

    interface RestService {
        @Headers("Cache-Control: max-age=600")
        @GET("/search_by_date?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        void searchByDate(@Query("query") String query, Callback<AlgoliaHits> callback);

        @Headers("Cache-Control: max-age=600")
        @GET("/search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        void search(@Query("query") String query, Callback<AlgoliaHits> callback);

        @Headers("Cache-Control: max-age=600")
        @GET("/search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        void searchByMinTimestamp(@Query("numericFilters") String timestampSeconds, Callback<AlgoliaHits> callback);
    }

    protected static class AlgoliaHits {
        Hit[] hits;
    }

    private static class Hit {
        String objectID;
    }
}

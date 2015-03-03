package io.github.hidroh.materialistic.data;

import android.content.Context;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Query;

public class AlgoliaClient implements ItemManager {

    private static final String BASE_API_URL = "https://hn.algolia.com/api/v1";
    private static AlgoliaClient mInstance;
    private RestService mRestService;
    private String mQuery = "";
    private HackerNewsClient mHackerNewsClient;

    public static AlgoliaClient getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AlgoliaClient();
            mInstance.mRestService = RestServiceFactory.create(context, BASE_API_URL, RestService.class);
            mInstance.mHackerNewsClient = HackerNewsClient.getInstance(context);
        }

        return mInstance;
    }

    @Override
    public void getStories(FetchMode fetchMode, ResponseListener<Item[]> listener) {
        getStories(mQuery, listener);
    }

    @Override
    public void getItem(String itemId, ResponseListener<Item> listener) {
        mHackerNewsClient.getItem(itemId, listener);
    }

    public void setQuery(String query) {
        if (query == null) {
            query = "";
        }

        mQuery = query;
    }

    private void getStories(String query, final ResponseListener<Item[]> listener) {
        mRestService.search(query, new Callback<AlgoliaHits>() {
            @Override
            public void success(AlgoliaHits algoliaHits, Response response) {
                if (listener == null) {
                    return;
                }

                Hit[] hits = algoliaHits.hits;
                Item[] stories = new Item[hits == null ? 0 : hits.length];
                for (int i = 0; i < stories.length; i++) {
                    stories[i] = new HackerNewsClient.HackerNewsItem(Long.parseLong(hits[i].objectID));
                }
                listener.onResponse(stories);
            }

            @Override
            public void failure(RetrofitError error) {
                if (listener == null) {
                    return;
                }

                listener.onError(error == null ? error.getMessage() : "");
            }
        });
    }

    private static interface RestService {
        @Headers("Cache-Control: max-age=600")
        @GET("/search_by_date?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        void search(@Query("query") String query, Callback<AlgoliaHits> callback);
    }

    private class AlgoliaHits {
        private Hit[] hits;
    }

    private class Hit {
        private String objectID;
    }
}

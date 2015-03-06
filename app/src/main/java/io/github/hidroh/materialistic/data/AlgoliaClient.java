package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.preference.PreferenceManager;

import io.github.hidroh.materialistic.R;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Query;

public class AlgoliaClient implements ItemManager {

    private static final String BASE_API_URL = "https://hn.algolia.com/api/v1";
    private static final Object sLock = new Object();
    private static AlgoliaClient sInstance;
    private RestService mRestService;
    private HackerNewsClient mHackerNewsClient;
    private boolean mSortByTime = true;

    public static AlgoliaClient getInstance(Context context) {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new AlgoliaClient();
                sInstance.mRestService = RestServiceFactory.create(context, BASE_API_URL, RestService.class);
                sInstance.mHackerNewsClient = HackerNewsClient.getInstance(context);
                sInstance.mSortByTime = PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(context.getString(R.string.pref_item_search_recent), true);
            }

            return sInstance;
        }
    }

    public void setSortByTime(boolean sortByTime) {
        mSortByTime = sortByTime;
    }

    @Override
    public void getStories(String filter, final ResponseListener<Item[]> listener) {
        Callback<AlgoliaHits> callback = new Callback<AlgoliaHits>() {
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
        };
        if (mSortByTime) {
            mRestService.searchByDate(filter, callback);
        } else {
            mRestService.search(filter, callback);
        }
    }

    @Override
    public void getItem(String itemId, ResponseListener<Item> listener) {
        mHackerNewsClient.getItem(itemId, listener);
    }

    private static interface RestService {
        @Headers("Cache-Control: max-age=600")
        @GET("/search_by_date?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        void searchByDate(@Query("query") String query, Callback<AlgoliaHits> callback);

        @Headers("Cache-Control: max-age=600")
        @GET("/search?hitsPerPage=100&tags=story&attributesToRetrieve=objectID&attributesToHighlight=none")
        void search(@Query("query") String query, Callback<AlgoliaHits> callback);
    }

    private class AlgoliaHits {
        private Hit[] hits;
    }

    private class Hit {
        private String objectID;
    }
}

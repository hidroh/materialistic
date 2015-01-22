package com.example.trung.material;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;

/**
 * Client to retrieve Hacker News content asynchronously
 */
public class HackerNewsClient {
    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0";
    private static HackerNewsClient mInstance;
    private RestService mRestService;

    private static interface RestService {
        @GET("/topstories.json")
        void topStories(Callback<int[]> callback);
    }

    private static interface ResponseListener<T> {
        void onResponse(T response);
        void onError(String errorMessage);
    }

    /**
     * Gets singleton client instance
     * @return a hacker news client
     */
    public static HackerNewsClient getInstance() {
        if (mInstance == null) {
            mInstance = new HackerNewsClient();
            mInstance.mRestService = new RestAdapter.Builder()
                    .setEndpoint(BASE_URL)
                    .build()
                    .create(RestService.class);
        }

        return mInstance;
    }

    /**
     * Gets array of top 100 stories
     * @param listener callback to be notified on response
     */
    public void getTopStories(final ResponseListener<int[]> listener) {
        mRestService.topStories(makeCallback(listener));
    }

    private <T> Callback<T> makeCallback(final ResponseListener<T> listener) {
        return new Callback<T>() {
            @Override
            public void success(T t, Response response) {
                if (listener == null) {
                    return;
                }

                listener.onResponse(t);
            }

            @Override
            public void failure(RetrofitError error) {
                if (listener == null) {
                    return;
                }

                listener.onError(error == null ? error.getMessage() : "");
            }
        };
    }
}

package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;

import io.github.hidroh.materialistic.R;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Path;

/**
 * Client to retrieve Hacker News content asynchronously
 */
public class HackerNewsClient implements ItemManager {
    private static final String BASE_API_URL = "https://hacker-news.firebaseio.com/v0";
    private static final long CACHE_SIZE = 1024 * 1024;
    private static final String TAG_OK_HTTP = "OkHttp";
    private static HackerNewsClient mInstance;
    private RestService mRestService;

    /**
     * Gets singleton client instance
     * @return a hacker news client
     */
    public static HackerNewsClient getInstance(Context context) {
        if (mInstance == null) {
            final OkHttpClient okHttpClient = new OkHttpClient();
            final boolean loggingEnabled = context.getResources().getBoolean(R.bool.debug);
            if (loggingEnabled) {
                okHttpClient.networkInterceptors().add(new LoggingInterceptor());
            }
            try {
                okHttpClient.setCache(new Cache(context.getApplicationContext().getCacheDir(),
                        CACHE_SIZE));
            } catch (IOException e) {
                // do nothing
            }

            mInstance = new HackerNewsClient();
            RestAdapter.Builder builder = new RestAdapter.Builder()
                    .setEndpoint(BASE_API_URL)
                    .setClient(new OkClient(okHttpClient));
            if (loggingEnabled) {
                builder.setLogLevel(RestAdapter.LogLevel.BASIC);
            }
            mInstance.mRestService = builder
                    .build()
                    .create(RestService.class);
        }

        return mInstance;
    }

    @Override
    public void getTopStories(final ItemManager.ResponseListener<ItemManager.Item[]> listener) {
        mRestService.topStories(new Callback<int[]>() {
            @Override
            public void success(int[] ints, Response response) {
                if (listener == null) {
                    return;
                }

                ItemManager.Item[] topStories = new ItemManager.Item[ints.length];
                for (int i = 0; i < ints.length; i++) {
                    topStories[i] = new ItemManager.Item(ints[i]);
                }
                listener.onResponse(topStories);
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

    @Override
    public void getItem(String itemId, ItemManager.ResponseListener<ItemManager.Item> listener) {
        mRestService.item(itemId, makeCallback(listener));
    }

    private <T> Callback<T> makeCallback(final ItemManager.ResponseListener<T> listener) {
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

    private static interface RestService {
        @Headers("Cache-Control: max-age=600")
        @GET("/topstories.json")
        void topStories(Callback<int[]> callback);
        @Headers("Cache-Control: max-age=300")
        @GET("/item/{itemId}.json")
        void item(@Path("itemId") String itemId, Callback<ItemManager.Item> callback);
    }

    private static class LoggingInterceptor implements Interceptor {
        @Override
        public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            long t1 = System.nanoTime();
            Log.d(TAG_OK_HTTP, String.format("---> %s (%s)%n%s",
                    request.url(), chain.connection(), request.headers()));

            com.squareup.okhttp.Response response = chain.proceed(request);

            long t2 = System.nanoTime();
            Log.d(TAG_OK_HTTP, String.format("<--- %s (%.1fms)%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));

            return response;
        }
    }
}

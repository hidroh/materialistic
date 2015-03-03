package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;

import io.github.hidroh.materialistic.R;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

public class RestServiceFactory {
    private static final String TAG_OK_HTTP = "OkHttp";
    private static final long CACHE_SIZE = 1024 * 1024;

    public static <T> T create(Context context, String baseUrl, Class<T> clazz) {
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

        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setEndpoint(baseUrl)
                .setClient(new OkClient(okHttpClient));
        if (loggingEnabled) {
            builder.setLogLevel(RestAdapter.LogLevel.BASIC);
        }
        return builder
                .build()
                .create(clazz);

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

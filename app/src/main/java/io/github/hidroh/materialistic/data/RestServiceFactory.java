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

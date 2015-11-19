package io.github.hidroh.materialistic.data;

import android.text.TextUtils;

import javax.inject.Inject;

import io.github.hidroh.materialistic.BuildConfig;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Query;

public interface ReadabilityClient {
    interface Callback {
        void onResponse(String content);
    }

    void parse(String url, Callback callback);

    class Impl implements ReadabilityClient {
        private static final CharSequence EMPTY_CONTENT = "<div></div>";
        private ReadabilityService mReadabilityService;

        @Inject
        public Impl(RestServiceFactory factory) {
            mReadabilityService = factory.create(ReadabilityService.READABILITY_API_URL,
                    ReadabilityService.class);
        }

        @Override
        public void parse(String url, final Callback callback) {
            mReadabilityService.parse(url, new retrofit.Callback<Readable>() {
                @Override
                public void success(Readable readable, Response response) {
                    if (TextUtils.equals(EMPTY_CONTENT, readable.content)) {
                        callback.onResponse(null);
                    } else {
                        callback.onResponse(readable.content);
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    callback.onResponse(null);
                }
            });
        }

        interface ReadabilityService {
            String READABILITY_API_URL = "https://readability.com/api/content/v1";

            @GET("/parser?token=" + BuildConfig.READABILITY_TOKEN)
            void parse(@Query("url") String url, retrofit.Callback<Readable> callback);
        }

        static class Readable {
            private String content;
        }
    }
}

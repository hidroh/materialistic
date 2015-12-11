package io.github.hidroh.materialistic.accounts;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.net.HttpURLConnection;

import io.github.hidroh.materialistic.BuildConfig;

public class UserServicesClient implements UserServices {
    private static final String TAG_OK_HTTP = "OkHttp";
    private static final String BASE_WEB_URL = "https://news.ycombinator.com";
    private static final String LOGIN_PATH = "login";
    private static final String LOGIN_PARAM_ACCT = "acct";
    private static final String LOGIN_PARAM_PW = "pw";
    private static final String LOGIN_PARAM_GOTO = "goto";
    private static final String DEFAULT_REDIRECT = "news";
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient mClient;

    public UserServicesClient(OkHttpClient okHttpClient) {
        mClient = okHttpClient;
        HttpLoggingInterceptor interceptor =
                new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        Log.d(TAG_OK_HTTP, message);
                    }
                });
        interceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.HEADERS :
                HttpLoggingInterceptor.Level.NONE);
        mClient.networkInterceptors().add(interceptor);
        mClient.setFollowRedirects(false);
    }

    @Override
    public void login(final String username, final String password, final Callback callback) {
        mClient.newCall(new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(LOGIN_PATH)
                        .build())
                .post(new FormEncodingBuilder()
                        .add(LOGIN_PARAM_ACCT, username)
                        .add(LOGIN_PARAM_PW, password)
                        .add(LOGIN_PARAM_GOTO, DEFAULT_REDIRECT)
                        .build())
                .build())
                .enqueue(new com.squareup.okhttp.Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onDone(false);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        final boolean successful = response.code() == HttpURLConnection.HTTP_MOVED_TEMP;
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onDone(successful);
                            }
                        });
                    }
                });

    }
}

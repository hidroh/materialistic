package io.github.hidroh.materialistic.accounts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.Pair;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.net.HttpURLConnection;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.R;

public class UserServicesClient implements UserServices {
    private static final String TAG_OK_HTTP = "OkHttp";
    private static final String BASE_WEB_URL = "https://news.ycombinator.com";
    private static final String LOGIN_PATH = "login";
    private static final String VOTE_PATH = "vote";
    private static final String COMMENT_PATH = "comment";
    private static final String LOGIN_PARAM_ACCT = "acct";
    private static final String LOGIN_PARAM_PW = "pw";
    private static final String LOGIN_PARAM_CREATING = "creating";
    private static final String LOGIN_PARAM_GOTO = "goto";
    private static final String VOTE_PARAM_FOR = "for";
    private static final String VOTE_PARAM_WHENCE = "whence";
    private static final String VOTE_PARAM_DIR = "dir";
    private static final String COMMENT_PARAM_PARENT = "parent";
    private static final String COMMENT_PARAM_TEXT = "text";
    private static final String VOTE_DIR_UP = "up";
    private static final String DEFAULT_REDIRECT = "news";
    private static final String CREATING_TRUE = "t";
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
    public void login(final String username, final String password, boolean createAccount,
                      final Callback callback) {
        FormEncodingBuilder formBuilder = new FormEncodingBuilder()
                .add(LOGIN_PARAM_ACCT, username)
                .add(LOGIN_PARAM_PW, password)
                .add(LOGIN_PARAM_GOTO, DEFAULT_REDIRECT);
        if (createAccount) {
            formBuilder.add(LOGIN_PARAM_CREATING, CREATING_TRUE);
        }
        mClient.newCall(new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(LOGIN_PATH)
                        .build())
                .post(formBuilder.build())
                .build())
                .enqueue(wrap(callback));
    }

    @Override
    public void voteUp(Context context, String itemId, final Callback callback) {
        Pair<String, String> credentials = AppUtils.getCredentials(context);
        if (credentials == null) {
            callback.onDone(false);
            return;
        }
        Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show();
        mClient.newCall(new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(VOTE_PATH)
                        .build())
                .post(new FormEncodingBuilder()
                        .add(LOGIN_PARAM_ACCT, credentials.first)
                        .add(LOGIN_PARAM_PW, credentials.second)
                        .add(VOTE_PARAM_FOR, itemId)
                        .add(VOTE_PARAM_DIR, VOTE_DIR_UP)
                        .add(VOTE_PARAM_WHENCE, DEFAULT_REDIRECT)
                        .build())
                .build())
                .enqueue(wrap(callback));
    }

    @Override
    public void reply(Context context, String parentId, String text, Callback callback) {
        Pair<String, String> credentials = AppUtils.getCredentials(context);
        if (credentials == null) {
            callback.onDone(false);
            return;
        }
        mClient.newCall(new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(COMMENT_PATH)
                        .build())
                .post(new FormEncodingBuilder()
                        .add(LOGIN_PARAM_ACCT, credentials.first)
                        .add(LOGIN_PARAM_PW, credentials.second)
                        .add(COMMENT_PARAM_PARENT, parentId)
                        .add(COMMENT_PARAM_TEXT, text)
                        .build())
                .build())
                .enqueue(wrap(callback));
    }

    private com.squareup.okhttp.Callback wrap(final Callback callback) {
        return new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError();
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
        };
    }
}

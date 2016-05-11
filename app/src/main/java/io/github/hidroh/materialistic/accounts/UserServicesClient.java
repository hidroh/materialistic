/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic.accounts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.WorkerThread;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UserServicesClient implements UserServices {
    private static final String BASE_WEB_URL = "https://news.ycombinator.com";
    private static final String LOGIN_PATH = "login";
    private static final String VOTE_PATH = "vote";
    private static final String COMMENT_PATH = "comment";
    private static final String SUBMIT_PATH = "submit";
    private static final String SUBMIT_POST_PATH = "r";
    private static final String LOGIN_PARAM_ACCT = "acct";
    private static final String LOGIN_PARAM_PW = "pw";
    private static final String LOGIN_PARAM_CREATING = "creating";
    private static final String LOGIN_PARAM_GOTO = "goto";
    private static final String VOTE_PARAM_FOR = "for";
    private static final String VOTE_PARAM_WHENCE = "whence";
    private static final String VOTE_PARAM_DIR = "dir";
    private static final String COMMENT_PARAM_PARENT = "parent";
    private static final String COMMENT_PARAM_TEXT = "text";
    private static final String SUBMIT_PARAM_TITLE = "title";
    private static final String SUBMIT_PARAM_URL = "url";
    private static final String SUBMIT_PARAM_TEXT = "text";
    private static final String SUBMIT_PARAM_FNID = "fnid";
    private static final String SUBMIT_PARAM_FNOP = "fnop";
    private static final String VOTE_DIR_UP = "up";
    private static final String DEFAULT_REDIRECT = "news";
    private static final String CREATING_TRUE = "t";
    private static final String DEFAULT_FNOP = "submit-page";
    private static final String DEFAULT_SUBMIT_REDIRECT = "newest";
    private static final String REGEX_INPUT = "<\\s*input[^>]*>";
    private static final String REGEX_VALUE = "value[^\"]*\"([^\"]*)\"";
    private static final String HEADER_LOCATION = "location";
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private final Call.Factory mCallFactory;

    @Inject
    public UserServicesClient(Call.Factory callFactory) {
        mCallFactory = callFactory;
    }

    @Override
    public void login(final String username, final String password, boolean createAccount,
                      final Callback callback) {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add(LOGIN_PARAM_ACCT, username)
                .add(LOGIN_PARAM_PW, password)
                .add(LOGIN_PARAM_GOTO, DEFAULT_REDIRECT);
        if (createAccount) {
            formBuilder.add(LOGIN_PARAM_CREATING, CREATING_TRUE);
        }
        mCallFactory.newCall(new Request.Builder()
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
        mCallFactory.newCall(new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(VOTE_PATH)
                        .build())
                .post(new FormBody.Builder()
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
        mCallFactory.newCall(new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(COMMENT_PATH)
                        .build())
                .post(new FormBody.Builder()
                        .add(LOGIN_PARAM_ACCT, credentials.first)
                        .add(LOGIN_PARAM_PW, credentials.second)
                        .add(COMMENT_PARAM_PARENT, parentId)
                        .add(COMMENT_PARAM_TEXT, text)
                        .build())
                .build())
                .enqueue(wrap(callback));
    }

    @Override
    public void submit(Context context, final String title, final String content, final boolean isUrl,
                       final Callback callback) {
        Pair<String, String> credentials = AppUtils.getCredentials(context);
        if (credentials == null) {
            callback.onDone(false);
            return;
        }
        /**
         * The flow:
         * POST /submit with acc, pw
         *  if 302 to /login, considered failed
         * POST /r with fnid, fnop, title, url or text
         *  if 302 to /newest, considered successful
         *  if 302 to /x, considered error, maybe duplicate or invalid input
         *  if 200 or anything else, considered error
         */
        // fetch submit page with given credentials
        mCallFactory.newCall(new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(SUBMIT_PATH)
                        .build())
                .post(new FormBody.Builder()
                        .add(LOGIN_PARAM_ACCT, credentials.first)
                        .add(LOGIN_PARAM_PW, credentials.second)
                        .build())
                .build())
                .enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        postError(callback);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        final boolean redirect = response.code() == HttpURLConnection.HTTP_MOVED_TEMP;
                        if (redirect) {
                            // redirect = failed login
                            postResult(callback, false);
                        } else {
                            // grab fnid from HTML and submit
                            ResponseBody body = response.body();
                            doSubmit(title,
                                    content,
                                    getInputValue(body.string(), SUBMIT_PARAM_FNID),
                                    isUrl,
                                    callback);
                            body.close();
                        }
                    }
                });
    }

    @WorkerThread
    private void doSubmit(String title, String content, String fnid, boolean isUrl, final Callback callback) {
        if (TextUtils.isEmpty(fnid)) {
            postError(callback);
            return;
        }
        mCallFactory.newCall(new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(SUBMIT_POST_PATH)
                        .build())
                .post(new FormBody.Builder()
                        .add(SUBMIT_PARAM_FNID, fnid)
                        .add(SUBMIT_PARAM_FNOP, DEFAULT_FNOP)
                        .add(SUBMIT_PARAM_TITLE, title)
                        .add(isUrl ? SUBMIT_PARAM_URL : SUBMIT_PARAM_TEXT, content)
                        .build())
                .build())
                .enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        postError(callback);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        boolean redirect = response.code() == HttpURLConnection.HTTP_MOVED_TEMP;
                        if (!redirect) {
                            postError(callback);
                            return;
                        }
                        String location = response.header(HEADER_LOCATION);
                        switch (location) {
                            case DEFAULT_SUBMIT_REDIRECT:
                                postResult(callback, true);
                                break;
                            default:
                                postError(callback);
                                break;
                        }
                    }
                });
    }

    private okhttp3.Callback wrap(final Callback callback) {
        return new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                UserServicesClient.this.postError(callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // redirect = successful submit
                postResult(callback, response.code() == HttpURLConnection.HTTP_MOVED_TEMP);
            }
        };
    }

    private void postResult(final Callback callback, final boolean successful) {
        mUiHandler.post(() -> callback.onDone(successful));
    }

    private void postError(final Callback callback) {
        mUiHandler.post(callback::onError);
    }

    private String getInputValue(String html, String name) {
        // extract <input ... >
        Matcher matcherInput = Pattern.compile(REGEX_INPUT).matcher(html);
        while (matcherInput.find()) {
            String input = matcherInput.group();
            if (input.contains(name)) {
                // extract value="..."
                Matcher matcher = Pattern.compile(REGEX_VALUE).matcher(input);
                return matcher.find() ? matcher.group(1) : null; // return first match if any
            }
        }
        return null;
    }
}

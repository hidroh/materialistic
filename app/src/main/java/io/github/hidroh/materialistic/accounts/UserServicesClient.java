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
import android.net.Uri;
import androidx.core.util.Pair;
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
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

public class UserServicesClient implements UserServices {
    private static final String BASE_WEB_URL = "https://news.ycombinator.com";
    private static final String LOGIN_PATH = "login";
    private static final String VOTE_PATH = "vote";
    private static final String COMMENT_PATH = "comment";
    private static final String SUBMIT_PATH = "submit";
    private static final String ITEM_PATH = "item";
    private static final String SUBMIT_POST_PATH = "r";
    private static final String LOGIN_PARAM_ACCT = "acct";
    private static final String LOGIN_PARAM_PW = "pw";
    private static final String LOGIN_PARAM_CREATING = "creating";
    private static final String LOGIN_PARAM_GOTO = "goto";
    private static final String ITEM_PARAM_ID = "id";
    private static final String VOTE_PARAM_ID = "id";
    private static final String VOTE_PARAM_HOW = "how";
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
    private static final String REGEX_CREATE_ERROR_BODY = "<body>([^<]*)";
    private static final String HEADER_LOCATION = "location";
    private static final String HEADER_COOKIE = "cookie";
    private static final String HEADER_SET_COOKIE = "set-cookie";
    private final Call.Factory mCallFactory;
    private final Scheduler mIoScheduler;

    @Inject
    public UserServicesClient(Call.Factory callFactory, Scheduler ioScheduler) {
        mCallFactory = callFactory;
        mIoScheduler = ioScheduler;
    }

    @Override
    public void login(String username, String password, boolean createAccount, Callback callback) {
        execute(postLogin(username, password, createAccount))
                .flatMap(response -> {
                    if (response.code() == HttpURLConnection.HTTP_OK) {
                        return Observable.error(new UserServices.Exception(parseLoginError(response)));
                    }
                    return Observable.just(response.code() == HttpURLConnection.HTTP_MOVED_TEMP);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onDone, callback::onError);
    }

    @Override
    public boolean voteUp(Context context, String itemId, Callback callback) {
        Pair<String, String> credentials = AppUtils.getCredentials(context);
        if (credentials == null) {
            return false;
        }
        Toast.makeText(context, R.string.sending, Toast.LENGTH_SHORT).show();
        execute(postVote(credentials.first, credentials.second, itemId))
                .map(response -> response.code() == HttpURLConnection.HTTP_MOVED_TEMP)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onDone, callback::onError);
        return true;
    }

    @Override
    public void reply(Context context, String parentId, String text, Callback callback) {
        Pair<String, String> credentials = AppUtils.getCredentials(context);
        if (credentials == null) {
            callback.onDone(false);
            return;
        }
        execute(postReply(parentId, text, credentials.first, credentials.second))
                .map(response -> response.code() == HttpURLConnection.HTTP_MOVED_TEMP)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onDone, callback::onError);
    }

    @Override
    public void submit(Context context, String title, String content, boolean isUrl, Callback callback) {
        Pair<String, String> credentials = AppUtils.getCredentials(context);
        if (credentials == null) {
            callback.onDone(false);
            return;
        }
        /*
          The flow:
          POST /submit with acc, pw
           if 302 to /login, considered failed
          POST /r with fnid, fnop, title, url or text
           if 302 to /newest, considered successful
           if 302 to /x, considered error, maybe duplicate or invalid input
           if 200 or anything else, considered error
         */
        // fetch submit page with given credentials
        execute(postSubmitForm(credentials.first, credentials.second))
                .flatMap(response -> response.code() != HttpURLConnection.HTTP_MOVED_TEMP ?
                        Observable.just(response) :
                        Observable.error(new IOException()))
                .flatMap(response -> {
                    try {
                        return Observable.just(new String[]{
                                response.header(HEADER_SET_COOKIE),
                                response.body().string()
                        });
                    } catch (IOException e) {
                        return Observable.error(e);
                    } finally {
                        response.close();
                    }
                })
                .map(array -> {
                    array[1] = getInputValue(array[1], SUBMIT_PARAM_FNID);
                    return array;
                })
                .flatMap(array -> !TextUtils.isEmpty(array[1]) ?
                        Observable.just(array) :
                        Observable.error(new IOException()))
                .flatMap(array -> execute(postSubmit(title, content, isUrl, array[0], array[1])))
                .flatMap(response -> response.code() == HttpURLConnection.HTTP_MOVED_TEMP ?
                        Observable.just(Uri.parse(response.header(HEADER_LOCATION))) :
                        Observable.error(new IOException()))
                .flatMap(uri -> TextUtils.equals(uri.getPath(), DEFAULT_SUBMIT_REDIRECT) ?
                        Observable.just(true) :
                        Observable.error(buildException(uri)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::onDone, callback::onError);
    }

    private Request postLogin(String username, String password, boolean createAccount) {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add(LOGIN_PARAM_ACCT, username)
                .add(LOGIN_PARAM_PW, password)
                .add(LOGIN_PARAM_GOTO, DEFAULT_REDIRECT);
        if (createAccount) {
            formBuilder.add(LOGIN_PARAM_CREATING, CREATING_TRUE);
        }
        return new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(LOGIN_PATH)
                        .build())
                .post(formBuilder.build())
                .build();
    }

    private Request postVote(String username, String password, String itemId) {
        return new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(VOTE_PATH)
                        .build())
                .post(new FormBody.Builder()
                        .add(LOGIN_PARAM_ACCT, username)
                        .add(LOGIN_PARAM_PW, password)
                        .add(VOTE_PARAM_ID, itemId)
                        .add(VOTE_PARAM_HOW, VOTE_DIR_UP)
                        .build())
                .build();
    }

    private Request postReply(String parentId, String text, String username, String password) {
        return new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(COMMENT_PATH)
                        .build())
                .post(new FormBody.Builder()
                        .add(LOGIN_PARAM_ACCT, username)
                        .add(LOGIN_PARAM_PW, password)
                        .add(COMMENT_PARAM_PARENT, parentId)
                        .add(COMMENT_PARAM_TEXT, text)
                        .build())
                .build();
    }

    private Request postSubmitForm(String username, String password) {
        return new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(SUBMIT_PATH)
                        .build())
                .post(new FormBody.Builder()
                        .add(LOGIN_PARAM_ACCT, username)
                        .add(LOGIN_PARAM_PW, password)
                        .build())
                .build();
    }

    private Request postSubmit(String title, String content, boolean isUrl, String cookie, String fnid) {
        Request.Builder builder = new Request.Builder()
                .url(HttpUrl.parse(BASE_WEB_URL)
                        .newBuilder()
                        .addPathSegment(SUBMIT_POST_PATH)
                        .build())
                .post(new FormBody.Builder()
                        .add(SUBMIT_PARAM_FNID, fnid)
                        .add(SUBMIT_PARAM_FNOP, DEFAULT_FNOP)
                        .add(SUBMIT_PARAM_TITLE, title)
                        .add(isUrl ? SUBMIT_PARAM_URL : SUBMIT_PARAM_TEXT, content)
                        .build());
        if (!TextUtils.isEmpty(cookie)) {
            builder.addHeader(HEADER_COOKIE, cookie);
        }
        return builder.build();
    }

    private Observable<Response> execute(Request request) {
        return Observable.defer(() -> {
            try {
                return Observable.just(mCallFactory.newCall(request).execute());
            } catch (IOException e) {
                return Observable.error(e);
            }
        }).subscribeOn(mIoScheduler);
    }

    private Throwable buildException(Uri uri) {
        switch (uri.getPath()) {
            case ITEM_PATH:
                UserServices.Exception exception = new UserServices.Exception(R.string.item_exist);
                String itemId = uri.getQueryParameter(ITEM_PARAM_ID);
                if (!TextUtils.isEmpty(itemId)) {
                    exception.data = AppUtils.createItemUri(itemId);
                }
                return exception;
            default:
                return new IOException();
        }
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

    private String parseLoginError(Response response) {
        try {
            Matcher matcher = Pattern.compile(REGEX_CREATE_ERROR_BODY).matcher(response.body().string());
            return matcher.find() ? matcher.group(1).replaceAll("\\n|\\r|\\t|\\s+", " ").trim() : null;
        } catch (IOException e) {
            return null;
        }
    }
}

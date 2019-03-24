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

package io.github.hidroh.materialistic.data;

import androidx.annotation.NonNull;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.DataModule;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import rx.Observable;
import rx.Scheduler;

/**
 * Client to retrieve Hacker News content asynchronously
 */
public class HackerNewsClient implements ItemManager, UserManager {
    public static final String HOST = "hacker-news.firebaseio.com";
    public static final String BASE_WEB_URL = "https://news.ycombinator.com";
    public static final String WEB_ITEM_PATH = BASE_WEB_URL + "/item?id=%s";
    static final String BASE_API_URL = "https://" + HOST + "/v0/";
    @Inject @Named(DataModule.IO_THREAD) Scheduler mIoScheduler;
    @Inject @Named(DataModule.MAIN_THREAD) Scheduler mMainThreadScheduler;
    private final RestService mRestService;
    private final SessionManager mSessionManager;
    private final FavoriteManager mFavoriteManager;

    @Inject
    public HackerNewsClient(RestServiceFactory factory,
                            SessionManager sessionManager,
                            FavoriteManager favoriteManager) {
        mRestService = factory.rxEnabled(true).create(BASE_API_URL, RestService.class);
        mSessionManager = sessionManager;
        mFavoriteManager = favoriteManager;
    }

    @Override
    public void getStories(@FetchMode String filter, @CacheMode int cacheMode,
                           final ResponseListener<Item[]> listener) {
        if (listener == null) {
            return;
        }
        Observable.defer(() -> getStoriesObservable(filter, cacheMode))
                .subscribeOn(mIoScheduler)
                .observeOn(mMainThreadScheduler)
                .subscribe(listener::onResponse,
                        t -> listener.onError(t != null ? t.getMessage() : ""));
    }

    @Override
    public void getItem(final String itemId, @CacheMode int cacheMode, ResponseListener<Item> listener) {
        if (listener == null) {
            return;
        }
        Observable<HackerNewsItem> itemObservable;
        switch (cacheMode) {
            case MODE_DEFAULT:
            default:
                itemObservable = mRestService.itemRx(itemId);
                break;
            case MODE_NETWORK:
                itemObservable = mRestService.networkItemRx(itemId);
                break;
            case MODE_CACHE:
                itemObservable = mRestService.cachedItemRx(itemId)
                        .onErrorResumeNext(mRestService.itemRx(itemId));
                break;
        }
        Observable.defer(() -> Observable.zip(
                mSessionManager.isViewed(itemId),
                mFavoriteManager.check(itemId),
                itemObservable,
                (isViewed, favorite, hackerNewsItem) -> {
                    if (hackerNewsItem != null) {
                        hackerNewsItem.preload();
                        hackerNewsItem.setIsViewed(isViewed);
                        hackerNewsItem.setFavorite(favorite);
                    }
                    return hackerNewsItem;
                }))
                .subscribeOn(mIoScheduler)
                .observeOn(mMainThreadScheduler)
                .subscribe(listener::onResponse,
                        t -> listener.onError(t != null ? t.getMessage() : ""));

    }

    @Override
    public Item[] getStories(String filter, @CacheMode int cacheMode) {
        try {
            return toItems(getStoriesCall(filter, cacheMode).execute().body());
        } catch (IOException e) {
            return new Item[0];
        }
    }

    @Override
    public Item getItem(String itemId, @CacheMode int cacheMode) {
        Call<HackerNewsItem> call;
        switch (cacheMode) {
            case MODE_DEFAULT:
            case MODE_CACHE:
            default:
                call = mRestService.item(itemId);
                break;
            case MODE_NETWORK:
                call = mRestService.networkItem(itemId);
                break;
        }
        try {
            return call.execute().body();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void getUser(String username, final ResponseListener<User> listener) {
        if (listener == null) {
            return;
        }
        mRestService.userRx(username)
                .map(userItem -> {
                    if (userItem != null) {
                        userItem.setSubmittedItems(toItems(userItem.getSubmitted()));
                    }
                    return userItem;
                })
                .subscribeOn(mIoScheduler)
                .observeOn(mMainThreadScheduler)
                .subscribe(listener::onResponse,
                        t -> listener.onError(t != null ? t.getMessage() : ""));
    }

    @NonNull
    private Observable<Item[]> getStoriesObservable(@FetchMode String filter, @CacheMode int cacheMode) {
        Observable<int[]> observable;
        switch (filter) {
            case NEW_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkNewStoriesRx() : mRestService.newStoriesRx();
                break;
            case SHOW_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkShowStoriesRx() : mRestService.showStoriesRx();
                break;
            case ASK_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkAskStoriesRx() : mRestService.askStoriesRx();
                break;
            case JOBS_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkJobStoriesRx() : mRestService.jobStoriesRx();
                break;
            case BEST_FETCH_MODE:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkBestStoriesRx() : mRestService.bestStoriesRx();
                break;
            default:
                observable = cacheMode == MODE_NETWORK ?
                        mRestService.networkTopStoriesRx() : mRestService.topStoriesRx();
                break;
        }
        return observable.map(this::toItems);
    }

    @NonNull
    private Call<int[]> getStoriesCall(@FetchMode String filter, @CacheMode int cacheMode) {
        Call<int[]> call;
        if (filter == null) {
            // for legacy 'new stories' widgets
            return cacheMode == MODE_NETWORK ?
                    mRestService.networkNewStories() : mRestService.newStories();
        }
        switch (filter) {
            case NEW_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkNewStories() : mRestService.newStories();
                break;
            case SHOW_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkShowStories() : mRestService.showStories();
                break;
            case ASK_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkAskStories() : mRestService.askStories();
                break;
            case JOBS_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkJobStories() : mRestService.jobStories();
                break;
            case BEST_FETCH_MODE:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkBestStories() : mRestService.bestStories();
                break;
            default:
                call = cacheMode == MODE_NETWORK ?
                        mRestService.networkTopStories() : mRestService.topStories();
                break;
        }
        return call;
    }

    private HackerNewsItem[] toItems(int[] ids) {
        if (ids == null) {
            return null;
        }
        HackerNewsItem[] items = new HackerNewsItem[ids.length];
        for (int i = 0; i < items.length; i++) {
            HackerNewsItem item = new HackerNewsItem(ids[i]);
            item.rank = i + 1;
            items[i] = item;
        }
        return items;
    }

    interface RestService {
        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("topstories.json")
        Observable<int[]> topStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("newstories.json")
        Observable<int[]> newStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("showstories.json")
        Observable<int[]> showStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("askstories.json")
        Observable<int[]> askStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("jobstories.json")
        Observable<int[]> jobStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("beststories.json")
        Observable<int[]> bestStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("topstories.json")
        Observable<int[]> networkTopStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("newstories.json")
        Observable<int[]> networkNewStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("showstories.json")
        Observable<int[]> networkShowStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("askstories.json")
        Observable<int[]> networkAskStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("jobstories.json")
        Observable<int[]> networkJobStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("beststories.json")
        Observable<int[]> networkBestStoriesRx();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("item/{itemId}.json")
        Observable<HackerNewsItem> itemRx(@Path("itemId") String itemId);

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("item/{itemId}.json")
        Observable<HackerNewsItem> networkItemRx(@Path("itemId") String itemId);

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_CACHE)
        @GET("item/{itemId}.json")
        Observable<HackerNewsItem> cachedItemRx(@Path("itemId") String itemId);

        @GET("user/{userId}.json")
        Observable<UserItem> userRx(@Path("userId") String userId);

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("topstories.json")
        Call<int[]> topStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("newstories.json")
        Call<int[]> newStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("showstories.json")
        Call<int[]> showStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("askstories.json")
        Call<int[]> askStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("jobstories.json")
        Call<int[]> jobStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("beststories.json")
        Call<int[]> bestStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("topstories.json")
        Call<int[]> networkTopStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("newstories.json")
        Call<int[]> networkNewStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("showstories.json")
        Call<int[]> networkShowStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("askstories.json")
        Call<int[]> networkAskStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("jobstories.json")
        Call<int[]> networkJobStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("beststories.json")
        Call<int[]> networkBestStories();

        @Headers(RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M)
        @GET("item/{itemId}.json")
        Call<HackerNewsItem> item(@Path("itemId") String itemId);

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_NETWORK)
        @GET("item/{itemId}.json")
        Call<HackerNewsItem> networkItem(@Path("itemId") String itemId);

        @Headers(RestServiceFactory.CACHE_CONTROL_FORCE_CACHE)
        @GET("item/{itemId}.json")
        Call<HackerNewsItem> cachedItem(@Path("itemId") String itemId);

        @GET("user/{userId}.json")
        Call<UserItem> user(@Path("userId") String userId);
    }
}

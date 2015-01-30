package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
public class HackerNewsClient {
    private static final String BASE_API_URL = "https://hacker-news.firebaseio.com/v0";
    private static final String BASE_WEB_URL = "https://news.ycombinator.com";
    private static final String WEB_ITEM_PATH = BASE_WEB_URL + "/item?id=%s";
    private static final long CACHE_SIZE = 1024 * 1024;
    public static final String TAG_OK_HTTP = "OkHttp";
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

    /**
     * Gets array of top 100 stories
     * @param listener callback to be notified on response
     */
    public void getTopStories(final ResponseListener<Item[]> listener) {
        mRestService.topStories(new Callback<int[]>() {
            @Override
            public void success(int[] ints, Response response) {
                if (listener == null) {
                    return;
                }

                Item[] topStories = new Item[ints.length];
                for (int i = 0; i < ints.length; i++) {
                    topStories[i] = new Item(ints[i]);
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

    /**
     * Gets individual item by ID
     * @param itemId    item ID
     * @param listener  callback to be notified on response
     */
    public void getItem(String itemId, ResponseListener<Item> listener) {
        mRestService.item(itemId, makeCallback(listener));
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

    public static interface ResponseListener<T> {
        void onResponse(T response);
        void onError(String errorMessage);
    }

    private static interface RestService {
        @Headers("Cache-Control: max-age=600")
        @GET("/topstories.json")
        void topStories(Callback<int[]> callback);
        @Headers("Cache-Control: max-age=300")
        @GET("/item/{itemId}.json")
        void item(@Path("itemId") String itemId, Callback<Item> callback);
    }

    public static class Item implements WebItem {
        public enum Type { job, story, comment, poll, pollopt }
        // The item's unique id. Required.
        private long id;
        // true if the item is deleted.
        private boolean deleted;
        // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
        private String type;
        // The username of the item's author.
        private String by;
        // Creation date of the item, in Unix Time.
        private long time;
        // The comment, Ask HN, or poll text. HTML.
        private String text;
        // true if the item is dead.
        private boolean dead;
        // The item's parent. For comments, either another comment or the relevant story. For pollopts, the relevant poll.
        private long parent;
        // The ids of the item's comments, in ranked display order.
        private long[] kids;
        // The URL of the story.
        private String url;
        // The story's score, or the votes for a pollopt.
        private int score;
        // The title of the story or poll.
        private String title;
        // A list of related pollopts, in display order.
        private long[] parts;
        private Item[] kidItems;
        private Boolean favorite;
        public int localRevision = -1;

        public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel source) {
                return new Item(source);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        private Item(long id) {
            this.id = id;
        }

        private Item(Parcel source) {
            id = source.readLong();
            title = source.readString();
            time = source.readLong();
            by = source.readString();
            kids = source.createLongArray();
            url = source.readString();
            text = source.readString();
            type = source.readString();
        }

        public void populate(Item info) {
            title = info.title;
            time = info.time;
            by = info.by;
            kids = info.kids;
            url = info.url;
            text = info.text;
            type = info.type;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(id);
            dest.writeString(title);
            dest.writeLong(time);
            dest.writeString(by);
            dest.writeLongArray(kids);
            dest.writeString(url);
            dest.writeString(text);
            dest.writeString(type);
        }

        @Override
        public String getId() {
            return String.valueOf(id);
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String getDisplayedTitle() {
            Type itemType = !TextUtils.isEmpty(type) ? Type.valueOf(type) : Type.story;
            switch (itemType) {
                case comment:
                    return text;
                case job:
                case story:
                case poll: // TODO poll need to display options
                default:
                    return title;
            }
        }

        public CharSequence getDisplayedTime(Context context) {
            return String.format("%s by %s",
                    DateUtils.getRelativeDateTimeString(context, time * 1000,
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.YEAR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_MONTH),
                    by);
        }

        public int getKidCount() {
            return kids != null ? kids.length : 0;
        }

        @Override
        public String getUrl() {
            Type itemType = !TextUtils.isEmpty(type) ? Type.valueOf(type) : Type.story;
            switch (itemType) {
                case job:
                case poll:
                case comment:
                    return getItemUrl(getId());
                default:
                    return url;
            }
        }

        private String getItemUrl(String itemId) {
            return String.format(WEB_ITEM_PATH, itemId);
        }

        public String getSource() {
            return TextUtils.isEmpty(url) ? null : Uri.parse(url).getHost();
        }

        public Item[] getKidItems() {
            if (kids == null || kids.length == 0) {
                return null;
            }

            if (kidItems == null) {
                kidItems = new Item[kids.length];
                for (int i = 0; i < kids.length; i++) {
                    kidItems[i] = new Item(kids[i]);
                }
            }

            return kidItems;
        }

        public String getText() {
            return text;
        }

        @Override
        public boolean isShareable() {
            Type itemType = !TextUtils.isEmpty(type) ? Type.valueOf(type) : Type.story;
            switch (itemType) {
                case story:
                case poll:
                case job:
                    return true;
                case comment:
                    return false;
                default:
                    return false;
            }
        }

        public Boolean isFavorite() {
            return favorite;
        }

        public void setFavorite(boolean favorite) {
            this.favorite = favorite;
        }
    }

    public interface WebItem extends Parcelable {
        String getDisplayedTitle();
        String getUrl();
        boolean isShareable();
        String getId();
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

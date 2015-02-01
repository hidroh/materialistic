package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
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
public class HackerNewsClient implements ItemManager {
    public static final String BASE_WEB_URL = "https://news.ycombinator.com";
    public static final String WEB_ITEM_PATH = BASE_WEB_URL + "/item?id=%s";
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
    public void getTopStories(final ItemManager.ResponseListener<Item[]> listener) {
        mRestService.topStories(new Callback<int[]>() {
            @Override
            public void success(int[] ints, Response response) {
                if (listener == null) {
                    return;
                }

                Item[] topStories = new Item[ints == null ? 0 : ints.length];
                for (int i = 0; i < topStories.length; i++) {
                    topStories[i] = new HackerNewsItem(ints[i]);
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
    public void getItem(String itemId, final ItemManager.ResponseListener<Item> listener) {
        mRestService.item(itemId, new Callback<HackerNewsItem>() {
            @Override
            public void success(HackerNewsItem item, Response response) {
                if (listener == null) {
                    return;
                }

                listener.onResponse(item);
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

    private static interface RestService {
        @Headers("Cache-Control: max-age=600")
        @GET("/topstories.json")
        void topStories(Callback<int[]> callback);
        @Headers("Cache-Control: max-age=300")
        @GET("/item/{itemId}.json")
        void item(@Path("itemId") String itemId, Callback<HackerNewsItem> callback);
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

    private static class HackerNewsItem implements Item {
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
        private HackerNewsItem[] kidItems;
        private boolean favorite;
        private int localRevision = -1;

        public static final Creator<Item> CREATOR = new Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel source) {
                return new HackerNewsItem(source);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        HackerNewsItem(long id) {
            this.id = id;
        }

        private HackerNewsItem(Parcel source) {
            id = source.readLong();
            title = source.readString();
            time = source.readLong();
            by = source.readString();
            kids = source.createLongArray();
            url = source.readString();
            text = source.readString();
            type = source.readString();
        }

        @Override
        public void populate(Item info) {
            title = info.getTitle();
            time = info.getTime();
            by = info.getBy();
            kids = info.getKids();
            url = info.getRawUrl();
            text = info.getText();
            type = info.getRawType();
        }

        @Override
        public String getRawType() {
            return type;
        }

        @Override
        public String getRawUrl() {
            return url;
        }

        @Override
        public long[] getKids() {
            return kids;
        }

        @Override
        public String getBy() {
            return by;
        }

        @Override
        public long getTime() {
            return time;
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

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getDisplayedTitle() {
            switch (getType()) {
                case comment:
                    return text;
                case job:
                case story:
                case poll: // TODO poll need to display options
                default:
                    return title;
            }
        }

        @Override
        public Type getType() {
            return !TextUtils.isEmpty(type) ? Type.valueOf(type) : Type.story;
        }

        @Override
        public CharSequence getDisplayedTime(Context context) {
            return String.format("%s by %s",
                    DateUtils.getRelativeDateTimeString(context, time * 1000,
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.YEAR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_MONTH),
                    by);
        }

        @Override
        public int getKidCount() {
            return kids != null ? kids.length : 0;
        }

        @Override
        public String getUrl() {
            switch (getType()) {
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

        @Override
        public String getSource() {
            return TextUtils.isEmpty(url) ? null : Uri.parse(url).getHost();
        }

        @Override
        public HackerNewsItem[] getKidItems() {
            if (kids == null || kids.length == 0) {
                return null;
            }

            if (kidItems == null) {
                kidItems = new HackerNewsItem[kids.length];
                for (int i = 0; i < kids.length; i++) {
                    kidItems[i] = new HackerNewsItem(kids[i]);
                }
            }

            return kidItems;
        }

        @Override
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

        @Override
        public boolean isFavorite() {
            return favorite;
        }

        @Override
        public void setFavorite(boolean favorite) {
            this.favorite = favorite;
        }

        @Override
        public int getLocalRevision() {
            return localRevision;
        }

        @Override
        public void setLocalRevision(int localRevision) {
            this.localRevision = localRevision;
        }
    }
}

package io.github.hidroh.materialistic;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Client to retrieve Hacker News content asynchronously
 */
public class HackerNewsClient {
    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0";
    private static HackerNewsClient mInstance;
    private RestService mRestService;

    /**
     * Gets singleton client instance
     * @return a hacker news client
     */
    public static HackerNewsClient getInstance() {
        if (mInstance == null) {
            mInstance = new HackerNewsClient();
            mInstance.mRestService = new RestAdapter.Builder()
                    .setLogLevel(RestAdapter.LogLevel.BASIC)
                    .setEndpoint(BASE_URL)
                    .build()
                    .create(RestService.class);
        }

        return mInstance;
    }

    /**
     * Gets array of top 100 stories
     * @param listener callback to be notified on response
     */
    public void getTopStories(final ResponseListener<TopStory[]> listener) {
        mRestService.topStories(new Callback<int[]>() {
            @Override
            public void success(int[] ints, Response response) {
                if (listener == null) {
                    return;
                }

                TopStory[] topStories = new TopStory[ints.length];
                for (int i = 0; i < ints.length; i++) {
                    topStories[i] = new TopStory(ints[i]);
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
     * TODO consider subclassing Item
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
        @GET("/topstories.json")
        void topStories(Callback<int[]> callback);
        @GET("/item/{itemId}.json")
        void item(@Path("itemId") String itemId, Callback<Item> callback);
    }

    public static class Item implements ItemInterface {
        // The item's unique id. Required.
        protected long id;
        // true if the item is deleted.
        private boolean deleted;
        // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
        private String type;
        // The username of the item's author.
        protected String by;
        // Creation date of the item, in Unix Time.
        protected long time;
        // The comment, Ask HN, or poll text. HTML.
        private String text;
        // true if the item is dead.
        private boolean dead;
        // The item's parent. For comments, either another comment or the relevant story. For pollopts, the relevant poll.
        private long parent;
        // The ids of the item's comments, in ranked display order.
        protected long[] kids;
        // The URL of the story.
        protected String url;
        // The story's score, or the votes for a pollopt.
        private int score;
        // The title of the story or poll.
        protected String title;
        // A list of related pollopts, in display order.
        private long[] parts;

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
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

        public String getUrl() {
            return url;
        }
    }

    public static class TopStory extends Item implements Parcelable {
        public static final Creator<TopStory> CREATOR = new Creator<TopStory>() {
            @Override
            public TopStory createFromParcel(Parcel source) {
                return new TopStory(source);
            }

            @Override
            public TopStory[] newArray(int size) {
                return new TopStory[size];
            }
        };

        private TopStory(long id) {
            this.id = id;
        }

        private TopStory(Parcel source) {
            id = source.readLong();
            title = source.readString();
            time = source.readLong();
            by = source.readString();
            kids = source.createLongArray();
            url = source.readString();
        }

        public void populate(Item info) {
            title = info.title;
            time = info.time;
            by = info.by;
            kids = info.kids;
            url = info.url;
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
        }
    }

    public interface ItemInterface {
        long getId();
        String getTitle();
        CharSequence getDisplayedTime(Context context);
        int getKidCount();
        String getUrl();
    }
}

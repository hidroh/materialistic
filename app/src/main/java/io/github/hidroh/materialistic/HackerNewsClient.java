package io.github.hidroh.materialistic;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;
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

    public static class Item implements Parcelable {
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

        public CharSequence getText() {
            return TextUtils.isEmpty(text) ? null : Html.fromHtml(text);
        }
    }
}

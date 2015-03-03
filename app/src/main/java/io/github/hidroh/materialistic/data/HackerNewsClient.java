package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.text.TextUtils;
import android.text.format.DateUtils;

import retrofit.Callback;
import retrofit.RetrofitError;
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
    private static HackerNewsClient mInstance;
    private RestService mRestService;

    /**
     * Gets singleton client instance
     * @return a hacker news client
     */
    public static HackerNewsClient getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new HackerNewsClient();
            mInstance.mRestService = RestServiceFactory.create(context, BASE_API_URL, RestService.class);
        }

        return mInstance;
    }

    @Override
    public void getStories(FetchMode fetchMode, final ResponseListener<Item[]> listener) {
        final Callback<int[]> callback = new Callback<int[]>() {
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
        };
        switch (fetchMode) {
            case newest:
                mRestService.newStories(callback);
                break;
            case show:
                mRestService.showStories(callback);
                break;
            case ask:
                mRestService.askStories(callback);
                break;
            case jobs:
                mRestService.jobStories(callback);
                break;
            default:
                mRestService.topStories(callback);
                break;
        }
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

        @Headers("Cache-Control: max-age=600")
        @GET("/newstories.json")
        void newStories(Callback<int[]> callback);

        @Headers("Cache-Control: max-age=600")
        @GET("/showstories.json")
        void showStories(Callback<int[]> callback);

        @Headers("Cache-Control: max-age=600")
        @GET("/askstories.json")
        void askStories(Callback<int[]> callback);

        @Headers("Cache-Control: max-age=600")
        @GET("/jobstories.json")
        void jobStories(Callback<int[]> callback);

        @Headers("Cache-Control: max-age=300")
        @GET("/item/{itemId}.json")
        void item(@Path("itemId") String itemId, Callback<HackerNewsItem> callback);
    }

    static class HackerNewsItem implements Item {
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
            favorite = source.readInt() == 0 ? false : true;
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
            dest.writeInt(favorite ? 1 : 0);
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
            try {
                return !TextUtils.isEmpty(type) ? Type.valueOf(type) : Type.story;
            } catch (IllegalArgumentException e) {
                return Type.story;
            }
        }

        @Override
        public CharSequence getDisplayedTime(Context context) {
            try {
                return String.format("%s by %s",
                        DateUtils.getRelativeDateTimeString(context, time * 1000,
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.YEAR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_MONTH),
                        by);
            } catch (NullPointerException e) { // TODO should properly prevent this
                return String.format("by %s", by);
            }
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
                    return TextUtils.isEmpty(url) ? getItemUrl(getId()) : url;
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

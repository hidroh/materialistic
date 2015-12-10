package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StrikethroughSpan;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AppUtils;
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
    private RestService mRestService;

    @Inject
    public HackerNewsClient(RestServiceFactory factory) {
        mRestService = factory.create(BASE_API_URL, RestService.class);
    }

    @Override
    public void getStories(@FetchMode String filter, final ResponseListener<Item[]> listener) {
        if (listener == null) {
            return;
        }

        final Callback<int[]> callback = new Callback<int[]>() {
            @Override
            public void success(int[] ints, Response response) {
                Item[] topStories = new Item[ints == null ? 0 : ints.length];
                for (int i = 0; i < topStories.length; i++) {
                    HackerNewsItem item = new HackerNewsItem(ints[i]);
                    item.rank = i + 1;
                    topStories[i] = item;
                }
                listener.onResponse(topStories);
            }

            @Override
            public void failure(RetrofitError error) {
                listener.onError(error != null ? error.getMessage() : "");
            }
        };
        switch (filter) {
            case NEW_FETCH_MODE:
                mRestService.newStories(callback);
                break;
            case SHOW_FETCH_MODE:
                mRestService.showStories(callback);
                break;
            case ASK_FETCH_MODE:
                mRestService.askStories(callback);
                break;
            case JOBS_FETCH_MODE:
                mRestService.jobStories(callback);
                break;
            default:
                mRestService.topStories(callback);
                break;
        }
    }

    @Override
    public void getItem(String itemId, final ItemManager.ResponseListener<Item> listener) {
        if (listener == null) {
            return;
        }

        mRestService.item(itemId, new Callback<HackerNewsItem>() {
            @Override
            public void success(HackerNewsItem item, Response response) {
                listener.onResponse(item);
            }

            @Override
            public void failure(RetrofitError error) {
                listener.onError(error != null ? error.getMessage() : "");
            }
        });
    }

    interface RestService {
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
        // In the case of stories or polls, the total comment count.
        private int descendants = -1;

        // view state
        private HackerNewsItem[] kidItems;
        private boolean favorite;
        private Boolean viewed;
        private int localRevision = -1;
        private int level = 0;
        private boolean collapsed;
        private boolean contentExpanded;
        int rank;
        private int lastKidCount = -1;
        private boolean hasNewDescendants = false;

        public static final Creator<HackerNewsItem> CREATOR = new Creator<HackerNewsItem>() {
            @Override
            public HackerNewsItem createFromParcel(Parcel source) {
                return new HackerNewsItem(source);
            }

            @Override
            public HackerNewsItem[] newArray(int size) {
                return new HackerNewsItem[size];
            }
        };

        HackerNewsItem(long id) {
            this.id = id;
        }

        private HackerNewsItem(long id, int level) {
            this(id);
            this.level = level;
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
            favorite = source.readInt() != 0;
            descendants = source.readInt();
            score = source.readInt();
            kidItems = source.createTypedArray(HackerNewsItem.CREATOR);
            favorite = source.readInt() == 1;
            viewed = source.readInt() == 1;
            localRevision = source.readInt();
            level = source.readInt();
            dead = source.readInt() == 1;
            deleted = source.readInt() == 1;
            collapsed = source.readInt() == 1;
            contentExpanded = source.readInt() == 1;
            rank = source.readInt();
            lastKidCount = source.readInt();
            hasNewDescendants = source.readInt() == 1;
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
            descendants = info.getDescendants();
            hasNewDescendants = lastKidCount >= 0 && descendants > lastKidCount;
            lastKidCount = descendants;
            parent = Long.parseLong(info.getParent());
            deleted = info.isDeleted();
            dead = info.isDead();
            score = info.getScore();
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
            dest.writeInt(descendants);
            dest.writeInt(score);
            dest.writeTypedArray(kidItems, 0);
            dest.writeInt(favorite ? 1 : 0);
            dest.writeInt(viewed != null && viewed ? 1 : 0);
            dest.writeInt(localRevision);
            dest.writeInt(level);
            dest.writeInt(dead ? 1 : 0);
            dest.writeInt(deleted ? 1 : 0);
            dest.writeInt(collapsed ? 1 : 0);
            dest.writeInt(contentExpanded ? 1 : 0);
            dest.writeInt(rank);
            dest.writeInt(lastKidCount);
            dest.writeInt(hasNewDescendants ? 1 : 0);
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
                case COMMENT_TYPE:
                    return text;
                case JOB_TYPE:
                case STORY_TYPE:
                case POLL_TYPE: // TODO poll need to display options
                default:
                    return title;
            }
        }

        @NonNull
        @Override
        public String getType() {
            return !TextUtils.isEmpty(type) ? type : STORY_TYPE;
        }

        @Override
        public Spannable getDisplayedTime(Context context, boolean abbreviate) {
            CharSequence relativeTime = "";
            if (abbreviate) {
                relativeTime = AppUtils.getAbbreviatedTimeSpan(time * 1000);
            } else {
                try {
                    relativeTime = DateUtils.getRelativeTimeSpanString(time * 1000,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL);
                } catch (NullPointerException e) {
                    // TODO should properly prevent this
                }
            }
            if (deleted) {
                Spannable spannable = new SpannableString(relativeTime);
                spannable.setSpan(new StrikethroughSpan(), 0, relativeTime.length(),
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                return spannable;
            }

            return new SpannableString(String.format("%s - %s", relativeTime, by));
        }

        @Override
        public int getKidCount() {
            if (descendants > 0) {
                return descendants;
            }

            return kids != null ? kids.length : 0;
        }

        @Override
        public int getLastKidCount() {
            return lastKidCount;
        }

        @Override
        public void setLastKidCount(int lastKidCount) {
            this.lastKidCount = lastKidCount;
        }

        @Override
        public boolean hasNewKids() {
            return hasNewDescendants;
        }

        @Override
        public String getUrl() {
            switch (getType()) {
                case JOB_TYPE:
                case POLL_TYPE:
                case COMMENT_TYPE:
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
                return new HackerNewsItem[0];
            }

            if (kidItems == null) {
                kidItems = new HackerNewsItem[kids.length];
                for (int i = 0; i < kids.length; i++) {
                    HackerNewsItem item = new HackerNewsItem(kids[i], level + 1);
                    item.rank = i + 1;
                    kidItems[i] = item;
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
            switch (getType()) {
                case STORY_TYPE:
                case POLL_TYPE:
                case JOB_TYPE:
                    return true;
                case COMMENT_TYPE:
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

        @Override
        public int getDescendants() {
            return descendants;
        }

        @Override
        public Boolean isViewed() {
            return viewed;
        }

        @Override
        public void setIsViewed(boolean isViewed) {
            viewed = isViewed;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public String getParent() {
            return String.valueOf(parent);
        }

        @Override
        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public boolean isDead() {
            return dead;
        }

        @Override
        public int getScore() {
            return score;
        }

        @Override
        public boolean isCollapsed() {
            return collapsed;
        }

        @Override
        public void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
        }

        @Override
        public int getRank() {
            return rank;
        }

        @Override
        public boolean isContentExpanded() {
            return contentExpanded;
        }

        @Override
        public void setContentExpanded(boolean expanded) {
            contentExpanded = expanded;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && o instanceof HackerNewsItem && id == ((HackerNewsItem) o).id;
        }
    }
}

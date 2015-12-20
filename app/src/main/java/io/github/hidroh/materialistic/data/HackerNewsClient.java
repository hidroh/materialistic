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

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StrikethroughSpan;

import javax.inject.Inject;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.R;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Path;

/**
 * Client to retrieve Hacker News content asynchronously
 */
public class HackerNewsClient implements ItemManager, UserManager {
    public static final String BASE_WEB_URL = "https://news.ycombinator.com";
    public static final String WEB_ITEM_PATH = BASE_WEB_URL + "/item?id=%s";
    private static final String BASE_API_URL = "https://hacker-news.firebaseio.com/v0/";
    private final RestService mRestService;
    private final SessionManager mSessionManager;
    private final FavoriteManager mFavoriteManager;
    private final ContentResolver mContentResolver;

    @Inject
    public HackerNewsClient(Context context, RestServiceFactory factory,
                            SessionManager sessionManager,
                            FavoriteManager favoriteManager) {
        mRestService = factory.create(BASE_API_URL, RestService.class);
        mSessionManager = sessionManager;
        mFavoriteManager = favoriteManager;
        mContentResolver = context.getApplicationContext().getContentResolver();
    }

    @Override
    public void getStories(@FetchMode String filter, final ResponseListener<Item[]> listener) {
        if (listener == null) {
            return;
        }
        Call<int[]> call;
        switch (filter) {
            case NEW_FETCH_MODE:
                call = mRestService.newStories();
                break;
            case SHOW_FETCH_MODE:
                call = mRestService.showStories();
                break;
            case ASK_FETCH_MODE:
                call = mRestService.askStories();
                break;
            case JOBS_FETCH_MODE:
                call = mRestService.jobStories();
                break;
            default:
                call = mRestService.topStories();
                break;
        }
        call.enqueue(new Callback<int[]>() {
            @Override
            public void onResponse(Response<int[]> response, Retrofit retrofit) {
                listener.onResponse(toItems(response.body()));
            }

            @Override
            public void onFailure(Throwable t) {
                listener.onError(t != null ? t.getMessage() : "");

            }
        });
    }

    @Override
    public void getItem(String itemId, final ResponseListener<Item> listener) {
        if (listener == null) {
            return;
        }
        ItemCallbackWrapper wrapper = new ItemCallbackWrapper(listener);
        mSessionManager.isViewed(mContentResolver, itemId, wrapper);
        mFavoriteManager.check(mContentResolver, itemId, wrapper);
        mRestService.item(itemId).enqueue(wrapper);
    }

    @Override
    public void getUser(String username, final ResponseListener<User> listener) {
        if (listener == null) {
            return;
        }
        mRestService.user(username)
                .enqueue(new Callback<UserItem>() {
                    @Override
                    public void onResponse(Response<UserItem> response, Retrofit retrofit) {
                        UserItem user = response.body();
                        if (user == null) {
                            listener.onResponse(null);
                            return;
                        }
                        user.submittedItems = toItems(user.submitted);
                        listener.onResponse(user);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        listener.onError(t != null ? t.getMessage() : "");
                    }
                });
    }

    @NonNull
    private HackerNewsItem[] toItems(int[] ids) {
        HackerNewsItem[] items = new HackerNewsItem[ids == null ? 0 : ids.length];
        for (int i = 0; i < items.length; i++) {
            HackerNewsItem item = new HackerNewsItem(ids[i]);
            item.rank = i + 1;
            items[i] = item;
        }
        return items;
    }

    interface RestService {
        @Headers("Cache-Control: max-age=600")
        @GET("topstories.json")
        Call<int[]> topStories();

        @Headers("Cache-Control: max-age=600")
        @GET("newstories.json")
        Call<int[]> newStories();

        @Headers("Cache-Control: max-age=600")
        @GET("showstories.json")
        Call<int[]> showStories();

        @Headers("Cache-Control: max-age=600")
        @GET("askstories.json")
        Call<int[]> askStories();

        @Headers("Cache-Control: max-age=600")
        @GET("jobstories.json")
        Call<int[]> jobStories();

        @Headers("Cache-Control: max-age=300")
        @GET("item/{itemId}.json")
        Call<HackerNewsItem> item(@Path("itemId") String itemId);

        @Headers("Cache-Control: max-age=300")
        @GET("user/{userId}.json")
        Call<UserItem> user(@Path("userId") String userId);
    }

    static class HackerNewsItem implements Item {
        private static final String FORMAT_LINK_USER = "<a href=\"%1$s://user/%2$s\">%2$s</a>";

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
        private boolean viewed;
        private int localRevision = -1;
        private int level = 0;
        private boolean collapsed;
        private boolean contentExpanded;
        int rank;
        private int lastKidCount = -1;
        private boolean hasNewDescendants = false;
        private HackerNewsItem parentItem;

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
            parent = source.readLong();
            parentItem = source.readParcelable(HackerNewsItem.class.getClassLoader());
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
            viewed = info.isViewed();
            favorite = info.isFavorite();
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
            dest.writeInt(viewed ? 1 : 0);
            dest.writeInt(localRevision);
            dest.writeInt(level);
            dest.writeInt(dead ? 1 : 0);
            dest.writeInt(deleted ? 1 : 0);
            dest.writeInt(collapsed ? 1 : 0);
            dest.writeInt(contentExpanded ? 1 : 0);
            dest.writeInt(rank);
            dest.writeInt(lastKidCount);
            dest.writeInt(hasNewDescendants ? 1 : 0);
            dest.writeLong(parent);
            dest.writeParcelable(parentItem, flags);
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
        public Spannable getDisplayedTime(Context context, boolean abbreviate, boolean authorLink) {
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
            SpannableStringBuilder spannableBuilder = new SpannableStringBuilder();
            if (dead) {
                spannableBuilder.append(context.getString(R.string.dead_prefix)).append(" ");
            }
            spannableBuilder.append(relativeTime);
            if (!TextUtils.isEmpty(by)) {
                spannableBuilder.append(" - ")
                        .append(authorLink ? Html.fromHtml(String.format(FORMAT_LINK_USER,
                                BuildConfig.APPLICATION_ID, by)) : by);
            }
            return spannableBuilder;
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
        public boolean isStoryType() {
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
        public boolean isViewed() {
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
        public Item getParentItem() {
            if (parent == 0) {
                return null;
            }
            if (parentItem == null) {
                parentItem = new HackerNewsItem(parent);
            }
            return parentItem;
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
        public void incrementScore() {
            score++;
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

    static class UserItem implements User {
        public static final Creator<UserItem> CREATOR = new Creator<UserItem>() {
            @Override
            public UserItem createFromParcel(Parcel source) {
                return new UserItem(source);
            }

            @Override
            public UserItem[] newArray(int size) {
                return new UserItem[size];
            }
        };
        private String id;
        private long delay;
        private long created;
        private long karma;
        private String about;
        private int[] submitted;

        // view state
        private HackerNewsItem[] submittedItems = new HackerNewsItem[0];

        private UserItem(Parcel source) {
            id = source.readString();
            delay = source.readLong();
            created = source.readLong();
            karma = source.readLong();
            about = source.readString();
            submitted = source.createIntArray();
            submittedItems = source.createTypedArray(HackerNewsItem.CREATOR);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getAbout() {
            return about;
        }

        @Override
        public long getKarma() {
            return karma;
        }

        @Override
        public String getCreated(Context context) {
            return DateUtils.formatDateTime(context, created * 1000, DateUtils.FORMAT_SHOW_DATE);
        }

        @NonNull
        @Override
        public Item[] getItems() {
            return submittedItems;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id);
            dest.writeLong(delay);
            dest.writeLong(created);
            dest.writeLong(karma);
            dest.writeString(about);
            dest.writeIntArray(submitted);
            dest.writeTypedArray(submittedItems, flags);
        }
    }

    private static class ItemCallbackWrapper implements SessionManager.OperationCallbacks,
            FavoriteManager.OperationCallbacks, Callback<HackerNewsItem> {
        private final ResponseListener<Item> responseListener;
        private Boolean isViewed;
        private Boolean isFavorite;
        private Item item;
        private String errorMessage;
        private boolean hasError;
        private boolean hasResponse;

        private ItemCallbackWrapper(@NonNull ResponseListener<Item> responseListener) {
            this.responseListener = responseListener;
        }

        @Override
        public void onCheckViewedComplete(boolean isViewed) {
            this.isViewed = isViewed;
            done();
        }

        @Override
        public void onCheckComplete(boolean isFavorite) {
            this.isFavorite = isFavorite;
            done();
        }

        @Override
        public void onResponse(Response<HackerNewsItem> response, Retrofit retrofit) {
            this.item = response.body();
            this.hasResponse = true;
            done();
        }

        @Override
        public void onFailure(Throwable t) {
            this.errorMessage = t != null ? t.getMessage() : "";
            this.hasError = true;
            done();
        }

        private void done() {
            if (isViewed == null) {
                return;
            }
            if (isFavorite == null) {
                return;
            }
            if (!(hasResponse || hasError)) {
                return;
            }
            if (hasResponse) {
                item.setFavorite(isFavorite);
                item.setIsViewed(isViewed);
                responseListener.onResponse(item);
            } else {
                responseListener.onError(errorMessage);
            }
        }
    }
}

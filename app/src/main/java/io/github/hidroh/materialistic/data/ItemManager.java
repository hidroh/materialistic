package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateUtils;

public interface ItemManager {
    String BASE_WEB_URL = "https://news.ycombinator.com";
    String WEB_ITEM_PATH = BASE_WEB_URL + "/item?id=%s";

    /**
     * Gets array of top 100 stories
     * @param listener callback to be notified on response
     */
    void getTopStories(final ResponseListener<Item[]> listener);
    /**
     * Gets individual item by ID
     * @param itemId    item ID
     * @param listener  callback to be notified on response
     */
    void getItem(String itemId, ItemManager.ResponseListener<ItemManager.Item> listener);

    interface WebItem extends Parcelable {
        String getDisplayedTitle();
        String getUrl();
        boolean isShareable();
        String getId();
    }

    interface ResponseListener<T> {
        void onResponse(T response);
        void onError(String errorMessage);
    }

    class Item implements WebItem {
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

        Item(long id) {
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

        public Type getType() {
            return !TextUtils.isEmpty(type) ? Type.valueOf(type) : Type.story;
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
}

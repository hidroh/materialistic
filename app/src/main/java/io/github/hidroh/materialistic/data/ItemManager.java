package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.os.Parcelable;

/**
 * Data repository for {@link io.github.hidroh.materialistic.data.ItemManager.Item}
 */
public interface ItemManager {

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
    void getItem(String itemId, ItemManager.ResponseListener<Item> listener);

    /**
     * Callback interface for item requests
     * @param <T> item type
     */
    interface ResponseListener<T> {
        /**
         * Fired when request is successful
         * @param response result
         */
        void onResponse(T response);

        /**
         * Fired when request is failed
         * @param errorMessage error message or null
         */
        void onError(String errorMessage);
    }

    /**
     * Represents an item that can be displayed as story/comment
     */
    interface Item extends WebItem {
        /**
         * Item types
         */
        public enum Type { job, story, comment, poll, pollopt;}

        /**
         * Sets information from given item
         * @param info source item
         */
        void populate(Item info);

        /**
         * Gets raw item type
         * @return string type or null
         * @see io.github.hidroh.materialistic.data.ItemManager.Item.Type
         */
        String getRawType();

        /**
         * Gets raw URL
         * @return string URL or null
         * @see #getUrl()
         */
        String getRawUrl();

        /**
         * Gets array of kid IDs
         * @return array of kid IDs or null
         * @see #getKidCount()
         * @see #getKidItems()
         */
        long[] getKids();

        /**
         * Gets author name
         * @return author name or null
         * @see #getDisplayedTime(android.content.Context)
         */
        String getBy();

        /**
         * Gets posted time
         * @return posted time as Unix timestamp in seconds
         * @see #getDisplayedTime(android.content.Context)
         */
        long getTime();

        /**
         * Gets title
         * @return title or null
         * @see #getDisplayedTitle()
         */
        String getTitle();

        /**
         * Gets item type, should be parsed from {@link #getRawType()}
         * @return item type
         * @see #getRawType()
         */
        Type getType();

        /**
         * Gets item text
         * @return item text or null
         * @see #getDisplayedTitle()
         */
        String getText();

        /**
         * Gets item source
         * @return item source or null
         */
        String getSource();

        /**
         * Gets formatted posted time for display
         * @param context an instance of {@link android.content.Context}
         * @return  displayed time
         * @see #getTime()
         * @see #getBy()
         */
        CharSequence getDisplayedTime(Context context);

        /**
         * Gets number of kids, contained in {@link #getKids()}
         * @return number of kids
         * @see #getKids()
         * @see #getKidItems()
         */
        int getKidCount();

        /**
         * Gets array of kids, with corresponding IDs in {@link #getKids()}
         * @return array of kids or null
         * @see #getKids()
         * @see #getKidCount()
         */
        Item[] getKidItems();

        /**
         * Checks if item is marked as favorite
         * @return true if favorite, false otherwise
         * @see #setFavorite(boolean)
         */
        Boolean isFavorite();

        /**
         * Updates item's favorite status to given status
         * @param favorite true if favorite, false otherwise
         * @see #isFavorite()
         */
        void setFavorite(boolean favorite);

        /**
         * Gets item's current revision. A revision can be used to determined if item state is stale
         * and needs updated
         * @return current revision
         * @see #setLocalRevision(int)
         * @see #populate(io.github.hidroh.materialistic.data.ItemManager.Item)
         * @see #setFavorite(boolean)
         */
        int getLocalRevision();

        /**
         * Updates item's current revision to new one
         * @param localRevision new item revision
         * @see #getLocalRevision()
         */
        void setLocalRevision(int localRevision);
    }

    /**
     * Represents an item that can be displayed by a {@link android.webkit.WebView}
     */
    interface WebItem extends Parcelable {
        /**
         * Gets formatted title to display
         * @return formatted title or null
         */
        String getDisplayedTitle();

        /**
         * Gets item URL to pass to {@link android.webkit.WebView#loadUrl(String)}
         * @return URL or null
         */
        String getUrl();

        /**
         * Checks if item can be shared
         * @return true if shareable, false otherwise
         */
        boolean isShareable();

        /**
         * Gets item ID
         * @return item ID
         */
        String getId();
    }
}

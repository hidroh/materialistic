package io.github.hidroh.materialistic.data;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.text.Spannable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Data repository for {@link io.github.hidroh.materialistic.data.ItemManager.Item}
 */
public interface ItemManager {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            TOP_FETCH_MODE,
            NEW_FETCH_MODE,
            ASK_FETCH_MODE,
            SHOW_FETCH_MODE,
            JOBS_FETCH_MODE
    })
    @interface FetchMode {}
    String TOP_FETCH_MODE = "top";
    String NEW_FETCH_MODE = "new";
    String ASK_FETCH_MODE = "ask";
    String SHOW_FETCH_MODE = "show";
    String JOBS_FETCH_MODE = "jobs";

    /**
     * Gets array of top stories
     * @param filter    filter of stories to fetch
     * @param listener  callback to be notified on response
     */
    void getStories(String filter, final ResponseListener<Item[]> listener);

    /**
     * Gets individual item by ID
     * @param itemId    item ID
     * @param listener  callback to be notified on response
     */
    void getItem(String itemId, ResponseListener<Item> listener);

    /**
     * Represents an item that can be displayed as story/comment
     */
    interface Item extends WebItem {

        /**
         * Sets information from given item
         * @param info source item
         */
        void populate(Item info);

        /**
         * Gets raw item type, used to be parsed by {@link #getType()}
         * @return string type or null
         * @see io.github.hidroh.materialistic.data.ItemManager.WebItem.Type
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
         * @see WebItem#getDisplayedTime(Context, boolean, boolean)
         */
        String getBy();

        /**
         * Gets posted time
         * @return posted time as Unix timestamp in seconds
         * @see WebItem#getDisplayedTime(Context, boolean, boolean)
         */
        long getTime();

        /**
         * Gets title
         * @return title or null
         * @see #getDisplayedTitle()
         */
        String getTitle();

        /**
         * Gets item text
         * @return item text or null
         * @see #getDisplayedTitle()
         */
        String getText();

        /**
         * Gets number of kids, contained in {@link #getKids()}
         * @return number of kids
         * @see #getKids()
         * @see #getKidItems()
         */
        int getKidCount();

        /**
         * Gets previous number of kids, before {@link #populate(Item)} is called
         * @return previous number of kids
         * @see #setLastKidCount(int)
         */
        int getLastKidCount();

        /**
         * Sets previous number of kids, before {@link #populate(Item)} is called
         * @param lastKidCount previous number of kids
         */
        void setLastKidCount(int lastKidCount);

        /**
         * Checks if item has new kids after {@link #populate(Item)}
         * @return true if has new kids, false otherwise
         */
        boolean hasNewKids();

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
        boolean isFavorite();

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

        /**
         * Gets item's descendants if any
         * @return  item's descendants or -1 if none
         */
        int getDescendants();

        /**
         * Indicates if this item has been viewed
         * @return true if viewed, falst if not, null if unknown
         */
        Boolean isViewed();

        /**
         * Sets item view status
         * @param isViewed  true if has been viewed, false otherwise
         */
        void setIsViewed(boolean isViewed);

        /**
         * Gets item level, i.e. how many ascendants it has
         * @return item level
         */
        int getLevel();

        /**
         * Gets parent ID if any
         * @return parent ID or 0 if none
         */
        String getParent();

        /**
         * Checks if item has been deleted
         * @return true if deleted, false otherwise
         */
        boolean isDeleted();

        /**
         * Checks if item is dead
         * @return true if dead, false otherwise
         */
        boolean isDead();

        /**
         * Gets item's score
         * @return item's score
         */
        int getScore();

        /**
         * Increments item's score
         */
        void incrementScore();

        /**
         * Checks if item is collapsed
         * @return true if collapsed, false otherwise
         */
        boolean isCollapsed();

        /**
         * Sets item collapsed state
         * @param collapsed true to collapse, false otherwise
         */
        void setCollapsed(boolean collapsed);

        /**
         * Gets item's rank among its siblings
         * @return item's rank
         */
        int getRank();

        /**
         * Checks if item content is expanded
         * @return true if expanded, false otherwise
         */
        boolean isContentExpanded();

        /**
         * Sets item content expanded state
         * @param expanded true to expand, false otherwise
         */
        void setContentExpanded(boolean expanded);
    }

    /**
     * Represents an item that can be displayed by a {@link android.webkit.WebView}
     */
    interface WebItem extends Parcelable {
        @Retention(RetentionPolicy.SOURCE)
        @StringDef({
                JOB_TYPE,
                STORY_TYPE,
                COMMENT_TYPE,
                POLL_TYPE
        })
        /**
         * Item types
         */
        @interface Type {}
        String JOB_TYPE = "job";
        String STORY_TYPE = "story";
        String COMMENT_TYPE = "comment";
        String POLL_TYPE = "poll";

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
         * Checks if item is not a comment
         * @return true if is not a comment, false otherwise
         */
        boolean isStoryType();

        /**
         * Gets item ID
         * @return item ID
         */
        String getId();

        /**
         * Gets item source
         * @return item source or null
         */
        String getSource();

        /**
         * Gets formatted posted time for display
         * @param context       an instance of {@link Context}
         * @param abbreviate    true to abbreviate time span, false otherwise
         * @param authorLink    true to display link to author, false otherwise
         * @return  displayed time
         */
        Spannable getDisplayedTime(Context context, boolean abbreviate, boolean authorLink);

        /**
         * Gets item type
         * @return item type
         */
        @NonNull
        @Type
        String getType();
    }
}

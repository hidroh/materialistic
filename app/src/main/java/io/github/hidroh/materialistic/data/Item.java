/*
 * Copyright (c) 2016 Ha Duy Trung
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

import android.content.Context;

/**
 * Represents an item that can be displayed as story/comment
 */
public interface Item extends WebItem {

    /**
     * Sets information from given item
     * @param info source item
     */
    void populate(Item info);

    /**
     * Gets raw item type, used to be parsed by {@link #getType()}
     * @return string type or null
     * @see Type
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
     * @see WebItem#getDisplayedAuthor(Context, boolean, int)
     */
    String getBy();

    /**
     * Gets posted time
     * @return posted time as Unix timestamp in seconds
     * @see WebItem#getDisplayedAuthor(Context, boolean, int)
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
     * Gets item's current revision. A revision can be used to determined if item state is stale
     * and needs updated
     * @return current revision
     * @see #setLocalRevision(int)
     * @see #populate(Item)
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
    boolean isViewed();

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
     * Gets parent item if any
     * @return parent item or null
     */
    Item getParentItem();

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
     * Checks if item has been voted via a user action
     * @return true if voted, false otherwise
     * @see #incrementScore()
     */
    boolean isVoted();

    /**
     * Checks if item has pending vote via a user action
     * @return true if pending voted, false otherwise
     * @see #incrementScore()
     */
    boolean isPendingVoted();

    /**
     * Clears pending voted status
     * @see #isPendingVoted()
     * @see #incrementScore()
     */
    void clearPendingVoted();

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

    long getNeighbour(int direction);

    CharSequence getDisplayedText();
}

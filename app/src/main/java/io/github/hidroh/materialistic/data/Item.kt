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

package io.github.hidroh.materialistic.data

/**
 * Represents an item that can be displayed as story/comment
 */
interface Item : WebItem {

    /**
     * raw item type, used to be parsed by [.getType]
     * @see WebItem.Type
     */
    val rawType: String

    /**
     * raw URL
     * @see url
     */
    val rawUrl: String

    /**
     * array of kid IDs
     * @see kidCount
     * @see kidItems
     */
    val kids: LongArray?

    /**
     * author name
     * @see WebItem.getDisplayedAuthor
     */
    val by: String?

    /**
     * posted time as Unix timestamp in seconds
     * @see WebItem.getDisplayedAuthor
     */
    val time: Long

    /**
     * Gets title
     * @see displayedTitle
     */
    val title: String?

    /**
     * Gets item text
     * @see displayedTitle
     */
    val text: String?

    /**
     * Gets number of kids, contained in [kids]
     * @see kids
     * @see kidItems
     */
    val kidCount: Int

    /**
     * previous number of kids, before [populate] is called
     * @see kidCount
     */
    var lastKidCount: Int

    /**
     * array of kids, with corresponding IDs in [.getKids]
     * @see kids
     * @see kidCount
     */
    val kidItems: Array<Item>?

    /**
     * item's current revision. A revision can be used to determined if item state is stale
     * and needs updated
     * @see populate
     * @see isFavorite
     */
    var localRevision: Int

    /**
     * item's descendants if any or -1 if none
     */
    val descendants: Int

    /**
     * Indicates if this item has been viewed. true if viewed, false if not
     */
    var isViewed: Boolean

    /**
     * item level, i.e. how many ascendants it has
     */
    val level: Int

    /**
     * parent ID if any or 0 if none
     */
    val parent: String

    /**
     * parent item if any
     */
    val parentItem: Item?

    /**
     * Checks if item has been deleted
     */
    val isDeleted: Boolean

    /**
     * Checks if item is dead
     */
    val isDead: Boolean

    /**
     * item's score
     */
    val score: Int

    /**
     * Checks if item has been voted via a user action
     * @see incrementScore
     */
    val isVoted: Boolean

    /**
     * Checks if item has pending vote via a user action
     * @see incrementScore
     */
    val isPendingVoted: Boolean

    /**
     * Checks if item is collapsed
     */
    var isCollapsed: Boolean

    /**
     * item's rank among its siblings
     */
    val rank: Int

    /**
     * Checks if item content is expanded
     */
    var isContentExpanded: Boolean

    val displayedText: CharSequence

    /**
     * Sets information from given item
     * @param info source item
     */
    fun populate(info: Item)

    /**
     * Checks if item has new kids after [.populate]
     * @return true if has new kids, false otherwise
     */
    fun hasNewKids(): Boolean

    /**
     * Increments item's score
     */
    fun incrementScore()

    /**
     * Clears pending voted status
     * @see isPendingVoted
     * @see incrementScore
     */
    fun clearPendingVoted()

    fun getNeighbour(direction: Int): Long
}

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

import android.content.Context
import android.os.Parcelable
import android.support.annotation.StringDef
import android.text.Spannable

/**
 * Represents an item that can be displayed by a [android.webkit.WebView]
 */
interface WebItem : Parcelable {

    /**
     * formatted title to display
     */
    val displayedTitle: String?

    /**
     * item URL to pass to [android.webkit.WebView.loadUrl]
     */
    val url: String?

    /**
     * item is not a comment
     */
    val isStoryType: Boolean

    /**
     * item's ID string
     */
    val id: String

    /**
     * Gets item's ID
     */
    val longId: Long

    /**
     * item's source
     */
    val source: String?

    /**
     * Item's type
     */
    @get:Type
    val type: String

    /**
     * item's favorite status
     */
    var isFavorite: Boolean

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(JOB_TYPE, STORY_TYPE, COMMENT_TYPE, POLL_TYPE)
    annotation class Type

    /**
     * Gets formatted author for display
     * @param context       an instance of [Context]
     * @param linkify       true to display author as a hyperlink, false otherwise
     * @param color         optional decorator color for author, or 0
     * @return  displayed author
     */
    fun getDisplayedAuthor(context: Context, linkify: Boolean, color: Int): Spannable

    /**
     * Gets formatted posted time for display
     * @param context    resources provider
     * @return  displayed time
     */
    fun getDisplayedTime(context: Context): Spannable

    companion object {
        const val JOB_TYPE = "job"
        const val STORY_TYPE = "story"
        const val COMMENT_TYPE = "comment"
        const val POLL_TYPE = "poll"
    }
}

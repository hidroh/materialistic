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
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import android.text.Spannable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents an item that can be displayed by a {@link android.webkit.WebView}
 */
public interface WebItem extends Parcelable {
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            JOB_TYPE,
            STORY_TYPE,
            COMMENT_TYPE,
            POLL_TYPE
    })
    /*
      Item types
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
     * Gets item ID string
     * @return item ID string
     */
    String getId();

    /**
     * Gets item ID
     * @return item ID
     */
    long getLongId();

    /**
     * Gets item source
     * @return item source or null
     */
    String getSource();

    /**
     * Gets formatted author for display
     * @param context       an instance of {@link Context}
     * @param linkify       true to display author as a hyperlink, false otherwise
     * @param color         optional decorator color for author, or 0
     * @return  displayed author
     */
    Spannable getDisplayedAuthor(Context context, boolean linkify, int color);

    /**
     * Gets formatted posted time for display
     * @param context    resources provider
     * @return  displayed time
     */
    Spannable getDisplayedTime(Context context);

    /**
     * Gets item type
     * @return item type
     */
    @NonNull
    @Type
    String getType();

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
}

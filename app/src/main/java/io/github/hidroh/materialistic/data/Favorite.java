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
import android.net.Uri;
import android.os.Parcel;
import androidx.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.annotation.Synthetic;

/**
 * Represents a favorite item
 */
public class Favorite implements WebItem {
    private String itemId;
    private String url;
    private String title;
    private long time;
    private boolean favorite;
    private Spannable displayedTime;
    private final Spannable displayedAuthor = new SpannableString("");

    public static final Creator<Favorite> CREATOR = new Creator<Favorite>() {
        @Override
        public Favorite createFromParcel(Parcel source) {
            return new Favorite(source);
        }

        @Override
        public Favorite[] newArray(int size) {
            return new Favorite[size];
        }
    };

    Favorite(String itemId, String url, String title, long time) {
        this.itemId = itemId;
        this.url = url;
        this.title = title;
        this.time = time;
        this.favorite = true;
    }

    @Synthetic
    Favorite(Parcel source) {
        itemId = source.readString();
        url = source.readString();
        title = source.readString();
        favorite = source.readInt() != 0;
        time = source.readLong();
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public boolean isStoryType() {
        return true;
    }

    @Override
    public String getId() {
        return itemId;
    }

    @Override
    public long getLongId() {
        return Long.valueOf(itemId);
    }

    @Override
    public String getDisplayedTitle() {
        return title;
    }

    @Override
    public Spannable getDisplayedAuthor(Context context, boolean linkify, int color) {
        return displayedAuthor;
    }

    @Override
    public Spannable getDisplayedTime(Context context) {
        if (displayedTime == null) {
            displayedTime = new SpannableString(context.getString(R.string.saved,
                    AppUtils.getAbbreviatedTimeSpan(time)));
        }
        return displayedTime;
    }

    @Override
    public String getSource() {
        return TextUtils.isEmpty(url) ? null : Uri.parse(url).getHost();
    }

    @NonNull
    @Override
    public String getType() {
        // TODO treating all saved items as stories for now
        return STORY_TYPE;
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
    public String toString() {
        return String.format("%s (%s) - %s", title, url, String.format(HackerNewsClient.WEB_ITEM_PATH, itemId));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(itemId);
        dest.writeString(url);
        dest.writeString(title);
        dest.writeInt(favorite ? 1 : 0);
        dest.writeLong(time);
    }

    long getTime() {
        return time;
    }
}

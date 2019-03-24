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
import android.os.Parcel;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import android.text.format.DateUtils;

import io.github.hidroh.materialistic.annotation.Synthetic;

class UserItem implements UserManager.User {
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
    @Keep private String id;
    @Keep private long delay;
    @Keep private long created;
    @Keep private long karma;
    @Keep private String about;
    @Keep private int[] submitted;

    // view state
    private HackerNewsItem[] submittedItems = new HackerNewsItem[0];

    @Synthetic
    UserItem(Parcel source) {
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

    void setSubmittedItems(HackerNewsItem[] submittedItems) {
        this.submittedItems = submittedItems != null ? submittedItems : new HackerNewsItem[0];
    }

    int[] getSubmitted() {
        return submitted;
    }
}

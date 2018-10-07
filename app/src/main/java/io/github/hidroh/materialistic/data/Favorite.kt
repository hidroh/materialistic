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
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableString
import io.github.hidroh.materialistic.AppUtils
import io.github.hidroh.materialistic.R
import io.github.hidroh.materialistic.data.WebItem.Companion.STORY_TYPE

open class Favorite(override val id: String, override val url: String?, override val displayedTitle: String, val time: Long) : WebItem {
    override var isFavorite: Boolean = true
    private val displayedAuthor = SpannableString("")
    private var displayedTime: Spannable? = null

    constructor(parcel: Parcel) : this(parcel.readString()!!, parcel.readString()!!, parcel.readString()!!, parcel.readLong()) {
        isFavorite = parcel.readInt() != 0
    }

    override val isStoryType: Boolean
        get() = true
    override val longId: Long
        get() = id.toLong()
    override val source: String?
        get() = if (url.isNullOrEmpty()) null else Uri.parse(url).host
    override val type: String
    // TODO treating all saved items as stories for now
        get() = STORY_TYPE


    override fun getDisplayedAuthor(context: Context, linkify: Boolean, color: Int): Spannable {
        return displayedAuthor
    }

    override fun getDisplayedTime(context: Context): Spannable {
        if (displayedTime == null) {
            displayedTime = SpannableString(context.getString(R.string.saved, AppUtils.getAbbreviatedTimeSpan(time)))
        }
        return displayedTime!!
    }


    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(url)
        dest.writeString(displayedTitle)
        dest.writeLong(time)
        dest.writeInt(if (isFavorite) 1 else 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Favorite> = object : Parcelable.Creator<Favorite> {
            override fun createFromParcel(source: Parcel): Favorite {
                return Favorite(source)
            }

            override fun newArray(size: Int): Array<Favorite?> {
                return arrayOfNulls(size)
            }
        }
    }

}
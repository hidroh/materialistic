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
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Parcel;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.view.View;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.Navigable;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.annotation.Synthetic;

class HackerNewsItem implements Item {
    private static final String AUTHOR_SEPARATOR = " - ";

    // The item's unique id. Required.
    @Keep private long id;
    // true if the item is deleted.
    @Keep private boolean deleted;
    // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
    @Keep private String type;
    // The username of the item's author.
    @Keep private String by;
    // Creation date of the item, in Unix Time.
    @Keep private long time;
    // The comment, Ask HN, or poll text. HTML.
    @Keep private String text;
    // true if the item is dead.
    @Keep private boolean dead;
    // The item's parent. For comments, either another comment or the relevant story. For pollopts, the relevant poll.
    @Keep private long parent;
    // The ids of the item's comments, in ranked display order.
    @Keep private long[] kids;
    // The URL of the story.
    @Keep private String url;
    // The story's score, or the votes for a pollopt.
    @Keep private int score;
    // The title of the story or poll.
    @Keep private String title;
    // A list of related pollopts, in display order.
    @SuppressWarnings("unused")
    @Keep private long[] parts;
    // In the case of stories or polls, the total comment count.
    @Keep private int descendants = -1;

    // view state
    private boolean favorite;
    private boolean viewed;
    private int localRevision = -1;
    @VisibleForTesting int level = 0;
    private boolean collapsed;
    private boolean contentExpanded;
    int rank;
    private int lastKidCount = -1;
    private boolean hasNewDescendants = false;
    private boolean voted;
    private boolean pendingVoted;
    private long next, previous;

    // non parcelable fields
    private HackerNewsItem[] kidItems;
    private HackerNewsItem parentItem;
    private Spannable displayedTime;
    private Spannable displayedAuthor;
    private CharSequence displayedText;
    private int defaultColor;

    public static final Creator<HackerNewsItem> CREATOR = new Creator<HackerNewsItem>() {
        @Override
        public HackerNewsItem createFromParcel(Parcel source) {
            return new HackerNewsItem(source);
        }

        @Override
        public HackerNewsItem[] newArray(int size) {
            return new HackerNewsItem[size];
        }
    };

    HackerNewsItem(long id) {
        this.id = id;
    }

    private HackerNewsItem(long id, int level) {
        this(id);
        this.level = level;
    }

    @Synthetic
    HackerNewsItem(Parcel source) {
        id = source.readLong();
        title = source.readString();
        time = source.readLong();
        by = source.readString();
        kids = source.createLongArray();
        url = source.readString();
        text = source.readString();
        type = source.readString();
        favorite = source.readInt() != 0;
        descendants = source.readInt();
        score = source.readInt();
        favorite = source.readInt() == 1;
        viewed = source.readInt() == 1;
        localRevision = source.readInt();
        level = source.readInt();
        dead = source.readInt() == 1;
        deleted = source.readInt() == 1;
        collapsed = source.readInt() == 1;
        contentExpanded = source.readInt() == 1;
        rank = source.readInt();
        lastKidCount = source.readInt();
        hasNewDescendants = source.readInt() == 1;
        parent = source.readLong();
        voted = source.readInt() == 1;
        pendingVoted = source.readInt() == 1;
        next = source.readLong();
        previous = source.readLong();
    }

    @Override
    public void populate(Item info) {
        title = info.getTitle();
        time = info.getTime();
        by = info.getBy();
        kids = info.getKids();
        url = info.getRawUrl();
        text = info.getText();
        displayedText = info.getDisplayedText(); // pre-load, but not part of Parcelable
        type = info.getRawType();
        descendants = info.getDescendants();
        hasNewDescendants = lastKidCount >= 0 && descendants > lastKidCount;
        lastKidCount = descendants;
        parent = Long.parseLong(info.getParent());
        deleted = info.isDeleted();
        dead = info.isDead();
        score = info.getScore();
        viewed = info.isViewed();
        favorite = info.isFavorite();
        localRevision = 1;
    }

    @Override
    public String getRawType() {
        return type;
    }

    @Override
    public String getRawUrl() {
        return url;
    }

    @Override
    public long[] getKids() {
        return kids;
    }

    @Override
    public String getBy() {
        return by;
    }

    @Override
    public long getTime() {
        return time;
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
        dest.writeInt(favorite ? 1 : 0);
        dest.writeInt(descendants);
        dest.writeInt(score);
        dest.writeInt(favorite ? 1 : 0);
        dest.writeInt(viewed ? 1 : 0);
        dest.writeInt(localRevision);
        dest.writeInt(level);
        dest.writeInt(dead ? 1 : 0);
        dest.writeInt(deleted ? 1 : 0);
        dest.writeInt(collapsed ? 1 : 0);
        dest.writeInt(contentExpanded ? 1 : 0);
        dest.writeInt(rank);
        dest.writeInt(lastKidCount);
        dest.writeInt(hasNewDescendants ? 1 : 0);
        dest.writeLong(parent);
        dest.writeInt(voted ? 1 : 0);
        dest.writeInt(pendingVoted ? 1 : 0);
        dest.writeLong(next);
        dest.writeLong(previous);
    }

    @Override
    public String getId() {
        return String.valueOf(id);
    }

    @Override
    public long getLongId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDisplayedTitle() {
        switch (getType()) {
            case COMMENT_TYPE:
                return text;
            case JOB_TYPE:
            case STORY_TYPE:
            case POLL_TYPE: // TODO poll need to display options
            default:
                return title;
        }
    }

    @NonNull
    @Override
    public String getType() {
        return !TextUtils.isEmpty(type) ? type : STORY_TYPE;
    }

    @Override
    public Spannable getDisplayedAuthor(Context context, boolean linkify, int color) {
        if (displayedAuthor == null) {
            if (TextUtils.isEmpty(by)) {
                displayedAuthor = new SpannableString("");
            } else {
                defaultColor = ContextCompat.getColor(context, AppUtils.getThemedResId(context,
                        linkify ? android.R.attr.textColorLink : android.R.attr.textColorSecondary));
                displayedAuthor = createAuthorSpannable(linkify);
            }
        }
        if (displayedAuthor.length() == 0) {
            return displayedAuthor;
        }
        displayedAuthor.setSpan(new ForegroundColorSpan(color != 0 ? color : defaultColor),
                AUTHOR_SEPARATOR.length(), displayedAuthor.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return displayedAuthor;
    }

    @Override
    public Spannable getDisplayedTime(Context context) {
        if (displayedTime == null) {
            SpannableStringBuilder builder = new SpannableStringBuilder(dead ?
                    context.getString(R.string.dead_prefix) + " " : "");
            SpannableString timeSpannable = new SpannableString(
                    AppUtils.getAbbreviatedTimeSpan(time * 1000));
            if (deleted) {
                timeSpannable.setSpan(new StrikethroughSpan(), 0, timeSpannable.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            builder.append(timeSpannable);
            displayedTime = builder;
        }
        return displayedTime;
    }

    @Override
    public int getKidCount() {
        if (descendants > 0) {
            return descendants;
        }

        return kids != null ? kids.length : 0;
    }

    @Override
    public int getLastKidCount() {
        return lastKidCount;
    }

    @Override
    public void setLastKidCount(int lastKidCount) {
        this.lastKidCount = lastKidCount;
    }

    @Override
    public boolean hasNewKids() {
        return hasNewDescendants;
    }

    @Override
    public String getUrl() {
        switch (getType()) {
            case JOB_TYPE:
            case POLL_TYPE:
            case COMMENT_TYPE:
                return getItemUrl(getId());
            default:
                return TextUtils.isEmpty(url) ? getItemUrl(getId()) : url;
        }
    }

    @NonNull
    private SpannableString createAuthorSpannable(boolean authorLink) {
        SpannableString bySpannable = new SpannableString(AUTHOR_SEPARATOR + by);
        if (!authorLink) {
            return bySpannable;
        }
        bySpannable.setSpan(new StyleSpan(Typeface.BOLD),
                AUTHOR_SEPARATOR.length(), bySpannable.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW)
                        .setData(AppUtils.createUserUri(getBy())));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        bySpannable.setSpan(clickableSpan,
                AUTHOR_SEPARATOR.length(), bySpannable.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return bySpannable;
    }

    private String getItemUrl(String itemId) {
        return String.format(HackerNewsClient.WEB_ITEM_PATH, itemId);
    }

    @Override
    public String getSource() {
        return TextUtils.isEmpty(getUrl()) ? null : Uri.parse(getUrl()).getHost();
    }

    @Override
    public HackerNewsItem[] getKidItems() {
        if (kids == null || kids.length == 0) {
            return new HackerNewsItem[0];
        }

        if (kidItems == null) {
            kidItems = new HackerNewsItem[kids.length];
            for (int i = 0; i < kids.length; i++) {
                HackerNewsItem item = new HackerNewsItem(kids[i], level + 1);
                item.rank = i + 1;
                if (i > 0) {
                    item.previous = kids[i - 1];
                }
                if (i < kids.length - 1) {
                    item.next = kids[i + 1];
                }
                kidItems[i] = item;
            }
        }

        return kidItems;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public CharSequence getDisplayedText() {
        if (displayedText == null) {
            displayedText = AppUtils.fromHtml(text);
        }
        return displayedText;
    }

    @Override
    public boolean isStoryType() {
        switch (getType()) {
            case STORY_TYPE:
            case POLL_TYPE:
            case JOB_TYPE:
                return true;
            case COMMENT_TYPE:
            default:
                return false;
        }
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
    public int getLocalRevision() {
        return localRevision;
    }

    @Override
    public void setLocalRevision(int localRevision) {
        this.localRevision = localRevision;
    }

    @Override
    public int getDescendants() {
        return descendants;
    }

    @Override
    public boolean isViewed() {
        return viewed;
    }

    @Override
    public void setIsViewed(boolean isViewed) {
        viewed = isViewed;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public String getParent() {
        return String.valueOf(parent);
    }

    @Override
    public Item getParentItem() {
        if (parent == 0) {
            return null;
        }
        if (parentItem == null) {
            parentItem = new HackerNewsItem(parent);
        }
        return parentItem;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean isDead() {
        return dead;
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public void incrementScore() {
        score++;
        voted = true;
        pendingVoted = true;
    }

    @Override
    public boolean isVoted() {
        return voted;
    }

    @Override
    public boolean isPendingVoted() {
        return pendingVoted;
    }

    @Override
    public void clearPendingVoted() {
        pendingVoted = false;
    }

    @Override
    public boolean isCollapsed() {
        return collapsed;
    }

    @Override
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    @Override
    public int getRank() {
        return rank;
    }

    @Override
    public boolean isContentExpanded() {
        return contentExpanded;
    }

    @Override
    public void setContentExpanded(boolean expanded) {
        contentExpanded = expanded;
    }

    @Override
    public long getNeighbour(int direction) {
        switch (direction) {
            case Navigable.DIRECTION_UP:
                return previous;
            case Navigable.DIRECTION_DOWN:
                return next;
            case Navigable.DIRECTION_LEFT:
                return level > 1 ? parent : 0L;
            case Navigable.DIRECTION_RIGHT:
                return kids != null && kids.length > 0 ? kids[0] : 0L;
            default:
                return 0L;
        }
    }

    @Override
    public int hashCode() {
        return (int) id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HackerNewsItem && id == ((HackerNewsItem) o).id;
    }

    void preload() {
        getDisplayedText(); // pre-load HTML
        getKidItems(); // pre-construct kids
    }
}

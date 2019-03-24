/*
 * Copyright (c) 2015 Ha Duy Trung
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

package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Checkable;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.util.Locale;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.annotation.Synthetic;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.WebItem;

public class StoryView extends RelativeLayout implements Checkable {
    private static final int VOTE_DELAY_MILLIS = 500;
    private static final String PROMOTED = "+%1$d";
    private final int mBackgroundColor;
    private final int mHighlightColor;
    private final int mTertiaryTextColorResId;
    private final int mSecondaryTextColorResId;
    private final int mPromotedColorResId;
    private final int mHotColorResId;
    private final int mAccentColorResId;
    private final TextView mRankTextView;
    @Synthetic final TextView mScoreTextView;
    private final View mBookmarked;
    private final TextView mPostedTextView;
    private final TextView mTitleTextView;
    private final TextView mSourceTextView;
    private final TextView mCommentButton;
    private final boolean mIsLocal;
    @Synthetic final ViewSwitcher mVoteSwitcher;
    private final View mMoreButton;
    private final Drawable mCommentDrawable;
    private final View mBackground;
    private boolean mChecked;

    public StoryView(Context context) {
        this(context, null);
    }

    public StoryView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StoryView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.StoryView);
        mIsLocal = ta.getBoolean(R.styleable.StoryView_local, false);
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.textColorTertiary,
                android.R.attr.textColorSecondary,
                R.attr.colorCardBackground,
                R.attr.colorCardHighlight
        });
        mTertiaryTextColorResId = ContextCompat.getColor(context, a.getResourceId(0, 0));
        mSecondaryTextColorResId = ContextCompat.getColor(context, a.getResourceId(1, 0));
        mBackgroundColor = ContextCompat.getColor(context, a.getResourceId(2, 0));
        mHighlightColor = ContextCompat.getColor(context, a.getResourceId(3, 0));
        mPromotedColorResId = ContextCompat.getColor(context, R.color.greenA700);
        mHotColorResId = ContextCompat.getColor(context, R.color.orange500);
        mAccentColorResId = ContextCompat.getColor(getContext(),
                AppUtils.getThemedResId(getContext(), R.attr.colorAccent));
        mCommentDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(context,
                R.drawable.ic_comment_white_24dp).mutate());
        DrawableCompat.setTint(mCommentDrawable, mAccentColorResId);
        inflate(context, mIsLocal ? R.layout.local_story_view : R.layout.story_view, this);
        mBackground = findViewById(R.id.background);
        mBackground.setBackgroundColor(mBackgroundColor);
        mVoteSwitcher = (ViewSwitcher) findViewById(R.id.vote_switcher);
        mRankTextView = (TextView) findViewById(R.id.rank);
        mScoreTextView = (TextView) findViewById(R.id.score);
        mBookmarked = findViewById(R.id.bookmarked);
        mPostedTextView = (TextView) findViewById(R.id.posted);
        mTitleTextView = (TextView) findViewById(R.id.title);
        mSourceTextView = (TextView) findViewById(R.id.source);
        mCommentButton = (TextView) findViewById(R.id.comment);
        mCommentButton.setCompoundDrawablesWithIntrinsicBounds(mCommentDrawable, null, null, null);
        mMoreButton = findViewById(R.id.button_more);
        // replace with bounded ripple as unbounded ripple requires container bg
        // http://b.android.com/155880
        mMoreButton.setBackgroundResource(AppUtils.getThemedResId(context,
                R.attr.selectableItemBackground));
        ta.recycle();
        a.recycle();
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked == checked) {
            return;
        }
        mChecked = checked;
        mBackground.setBackgroundColor(mChecked ? mHighlightColor : mBackgroundColor);
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    public void setStory(@NonNull WebItem story, int hotThreshold) {
        if (!mIsLocal && story instanceof Item) {
            Item item = (Item) story;
            boolean hot = item.getScore() >= hotThreshold * AppUtils.HOT_FACTOR;
            mScoreTextView.setTextColor(hot ? mHotColorResId : mSecondaryTextColorResId);
            mRankTextView.setText(String.valueOf(item.getRank()));
            mScoreTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, hot ?
                    R.drawable.ic_whatshot_orange500_18dp : 0);
            mScoreTextView.setText(getContext().getResources()
                    .getQuantityString(R.plurals.score, item.getScore(), item.getScore()));
            if (item.getKidCount() > 0) {
                hot = item.getKidCount() >= hotThreshold;
                mCommentButton.setTextColor(hot ? mHotColorResId : mAccentColorResId);
                if (hot) {
                    mCommentButton.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_whatshot_orange500_24dp, 0, 0, 0);
                } else {
                    mCommentButton.setCompoundDrawablesWithIntrinsicBounds(
                            mCommentDrawable, null, null, null);
                }
                mCommentButton.setText(String.valueOf(item.getKidCount()));
            } else {
                mCommentButton.setTextColor(mAccentColorResId);
                mCommentButton.setText(null);
                mCommentButton.setCompoundDrawablesWithIntrinsicBounds(
                        mCommentDrawable, null, null, null);
            }
        }
        mCommentButton.setVisibility(View.VISIBLE);
        mTitleTextView.setText(getContext().getString(R.string.loading_text));
        mTitleTextView.setText(story.getDisplayedTitle());
        mPostedTextView.setText(story.getDisplayedTime(getContext()));
        mPostedTextView.append(story.getDisplayedAuthor(getContext(), false, 0));
        switch (story.getType()) {
            case Item.JOB_TYPE:
                mSourceTextView.setText(null);
                mSourceTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_work_white_18dp, 0, 0, 0);
                break;
            case Item.POLL_TYPE:
                mSourceTextView.setText(null);
                mSourceTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_poll_white_18dp, 0, 0, 0);
                break;
            default:
                mSourceTextView.setText(story.getSource());
                mSourceTextView.setCompoundDrawables(null, null, null, null);
                break;
        }
    }

    public void reset() {
        if (!mIsLocal) {
            mRankTextView.setText(R.string.loading_text);
            mScoreTextView.setText(R.string.loading_text);
            mScoreTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            mBookmarked.setVisibility(INVISIBLE);
        }
        mTitleTextView.setText(getContext().getString(R.string.loading_text));
        mPostedTextView.setText(R.string.loading_text);
        mSourceTextView.setText(R.string.loading_text);
        mSourceTextView.setCompoundDrawables(null, null, null, null);
        mCommentButton.setVisibility(View.INVISIBLE);
    }

    public void setViewed(boolean isViewed) {
        if (mIsLocal) {
            return; // local always means viewed, do not decorate
        }
        mTitleTextView.setTextColor(isViewed ? mSecondaryTextColorResId : mTertiaryTextColorResId);
    }

    public void setPromoted(int change) {
        SpannableString spannable = new SpannableString(String.format(Locale.US, PROMOTED, change));
        spannable.setSpan(new SuperscriptSpan(), 0, spannable.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(0.6f), 0, spannable.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(mPromotedColorResId), 0, spannable.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mRankTextView.append(spannable);
    }

    public void setFavorite(boolean isFavorite) {
        if (mIsLocal) {
            return; // local item must be favorite, do not decorate
        }
        mBookmarked.setVisibility(isFavorite ? View.VISIBLE : View.INVISIBLE);
    }

    public void setOnCommentClickListener(View.OnClickListener listener) {
        mCommentButton.setOnClickListener(listener);
    }

    public void setUpdated(@NonNull Item story, boolean updated, int change) {
        if (mIsLocal) {
            return; // local items do not change
        }
        if (updated) {
            mRankTextView.append(decorateUpdated());
        } else if (change > 0) {
            setPromoted(change);
        }
        if (story.getKidCount() > 0 && story.hasNewKids()) {
            mCommentButton.append(decorateUpdated());
        }
    }

    void animateVote(final int newScore) {
        if (mIsLocal) {
            return;
        }
        mVoteSwitcher.getInAnimation().setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // no op
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().postDelayed(mVoteSwitcher::showNext, VOTE_DELAY_MILLIS);
                mScoreTextView.setText(getContext().getResources()
                        .getQuantityString(R.plurals.score, newScore, newScore));
                mVoteSwitcher.getInAnimation().setAnimationListener(null);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // no op
            }
        });
        mVoteSwitcher.showNext();
    }

    public View getMoreOptions() {
        return mMoreButton;
    }

    private Spannable decorateUpdated() {
        SpannableStringBuilder sb = new SpannableStringBuilder("*");
        sb.setSpan(new AsteriskSpan(getContext()), sb.length() - 1, sb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
}

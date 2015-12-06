package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import java.util.List;

import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.data.ItemManager;

public class StoryView extends RelativeLayout implements Checkable {
    private final int mBackgroundColor;
    private final int mHighlightColor;
    private final int mTertiaryTextColorResId;
    private final int mSecondaryTextColorResId;
    private final int mPromotedColorResId;
    private final TextSwitcher mTitleSwitcher;
    private final TextView mRankTextView;
    private final TextView mScoreTextView;
    private final View mBookmarked;
    private final TextView mPostedTextView;
    private final TextSwitcher mTitleTextView;
    private final TextView mSourceTextView;
    private final View mCommentButton;
    private final boolean mIsLocal;
    private boolean mChecked;

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
        inflate(context, mIsLocal ? R.layout.local_story_view : R.layout.story_view, this);
        setBackgroundColor(mBackgroundColor);
        mTitleSwitcher = (TextSwitcher) findViewById(R.id.title);
        mRankTextView = (TextView) findViewById(R.id.rank);
        mScoreTextView = (TextView) findViewById(R.id.score);
        mBookmarked = findViewById(R.id.bookmarked);
        mPostedTextView = (TextView) findViewById(R.id.posted);
        mTitleTextView = (TextSwitcher) findViewById(R.id.title);
        mSourceTextView = (TextView) findViewById(R.id.source);
        mCommentButton = findViewById(R.id.comment);
        ta.recycle();
        a.recycle();
    }

    @Override
    public void setChecked(boolean checked) {
        if (mChecked == checked) {
            return;
        }
        mChecked = checked;
        setBackgroundColor(mChecked ? mHighlightColor : mBackgroundColor);
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    public void setStory(@NonNull ItemManager.WebItem story) {
        if (!mIsLocal && story instanceof ItemManager.Item) {
            ItemManager.Item item = (ItemManager.Item) story;
            mRankTextView.setText(String.valueOf(item.getRank()));
            mScoreTextView.setText(getContext().getString(R.string.score, item.getScore()));
            if (item.getKidCount() > 0) {
                ((Button) mCommentButton).setText(getContext()
                        .getString(R.string.comments_count, item.getKidCount()));
                mCommentButton.setVisibility(View.VISIBLE);
            } else {
                mCommentButton.setVisibility(View.GONE);
            }
        }
        mTitleTextView.setCurrentText(getContext().getString(R.string.loading_text));
        mTitleTextView.setText(story.getDisplayedTitle());
        mPostedTextView.setText(story.getDisplayedTime(getContext(), true));
        switch (story.getType()) {
            case ItemManager.Item.JOB_TYPE:
                mSourceTextView.setText(null);
                mSourceTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_work_grey600_18dp, 0, 0, 0);
                break;
            case ItemManager.Item.POLL_TYPE:
                mSourceTextView.setText(null);
                mSourceTextView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_poll_grey600_18dp, 0, 0, 0);
                break;
            default:
                mSourceTextView.setText(story.getSource());
                mSourceTextView.setCompoundDrawables(null, null, null, null);
                break;
        }
    }

    public void reset() {
        if (!mIsLocal) {
            mScoreTextView.setText(R.string.loading_text);
        }
        mTitleTextView.setCurrentText(getContext().getString(R.string.loading_text));
        mPostedTextView.setText(R.string.loading_text);
        mSourceTextView.setText(R.string.loading_text);
        mSourceTextView.setCompoundDrawables(null, null, null, null);
        mCommentButton.setVisibility(View.GONE);
    }

    public void setViewed(boolean isViewed) {
        if (mIsLocal) {
            return; // local always means viewed, do not decorate
        }
        ((android.widget.TextView) mTitleSwitcher.getCurrentView())
                .setTextColor(isViewed ? mSecondaryTextColorResId : mTertiaryTextColorResId);
    }

    public void setPromoted(boolean isPromoted) {
        if (mIsLocal) {
            return; // local item cannot change rank
        }
        mRankTextView.setTextColor(isPromoted ? mPromotedColorResId : mTertiaryTextColorResId);
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

    public void setUpdated(@NonNull ItemManager.Item story,
                              @NonNull List<ItemManager.Item> updated,
                              @NonNull List<String> promotedIds) {
        if (mIsLocal) {
            return; // local items do not change
        }
        mRankTextView.setText(decorateUpdated(
                String.valueOf(story.getRank()), updated.contains(story)));
        setPromoted(promotedIds.contains(story.getId()));
        if (story.getKidCount() > 0) {
            ((Button) mCommentButton).setText(decorateUpdated(
                    getContext().getString(R.string.comments_count, story.getKidCount()),
                    story.hasNewKids()));
        }
    }

    private Spannable decorateUpdated(String text, boolean updated) {
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        if (updated) {
            sb.append("*");
            sb.setSpan(new AsteriskSpan(getContext()), sb.length() - 1, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }
}

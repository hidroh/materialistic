package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

import io.github.hidroh.materialistic.R;

public class StoryView extends RelativeLayout implements Checkable {
    private final int mBackgroundColor;
    private final int mHighlightColor;
    private final int mTertiaryTextColorResId;
    private final int mSecondaryTextColorResId;
    private final int mPromotedColorResId;
    private final TextSwitcher mTitleSwitcher;
    private final TextView mRankTextView;
    private final View mBookmarked;
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
        mRankTextView = (android.widget.TextView) findViewById(R.id.rank);
        mBookmarked = findViewById(R.id.bookmarked);
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
}

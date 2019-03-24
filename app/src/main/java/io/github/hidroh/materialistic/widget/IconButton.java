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

package io.github.hidroh.materialistic.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.widget.AppCompatImageButton;
import android.util.AttributeSet;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;

public class IconButton extends AppCompatImageButton {
    private static final int[][] STATES = new int[][]{
            new int[]{android.R.attr.state_enabled},
            new int[]{-android.R.attr.state_enabled}
    };
    private ColorStateList mColorStateList;
    private final boolean mTinted;

    public IconButton(Context context) {
        this(context, null);
    }

    public IconButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundResource(AppUtils.getThemedResId(context, R.attr.selectableItemBackgroundBorderless));
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconButton, 0, 0);
        int colorDisabled = ContextCompat.getColor(context,
                AppUtils.getThemedResId(context, android.R.attr.textColorSecondary));
        int colorDefault = ContextCompat.getColor(context,
                AppUtils.getThemedResId(context, android.R.attr.textColorPrimary));
        int colorEnabled = ta.getColor(R.styleable.IconButton_tint, colorDefault);
        mColorStateList = new ColorStateList(STATES, new int[]{colorEnabled, colorDisabled});
        mTinted = ta.hasValue(R.styleable.IconButton_tint);
        if (getSuggestedMinimumWidth() == 0) {
            setMinimumWidth(context.getResources().getDimensionPixelSize(R.dimen.icon_button_width));
        }
        setScaleType(ScaleType.CENTER);
        setImageDrawable(getDrawable());
        ta.recycle();
    }

    @Override
    public void setImageResource(int resId) {
        setImageDrawable(ContextCompat.getDrawable(getContext(), resId));
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(tint(drawable));
    }

    private Drawable tint(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Drawable tintDrawable = DrawableCompat.wrap(mTinted ? drawable.mutate() : drawable);
        DrawableCompat.setTintList(tintDrawable, mColorStateList);
        return tintDrawable;
    }
}

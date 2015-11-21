package io.github.hidroh.materialistic.widget.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import io.github.hidroh.materialistic.AppUtils;
import io.github.hidroh.materialistic.R;

public class ThemeView extends CardView {

    public ThemeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.theme_view, this, true);
        TypedArray ta = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.theme});
        ContextThemeWrapper wrapper = new ContextThemeWrapper(context, ta.getResourceId(0, R.style.AppTheme));
        ta.recycle();
        int cardBackgroundColor = AppUtils.getThemedResId(wrapper, R.attr.colorCardBackground);
        int textColor = AppUtils.getThemedResId(wrapper, android.R.attr.textColorTertiary);
        setCardBackgroundColor(ContextCompat.getColor(wrapper, cardBackgroundColor));
        ((TextView) findViewById(R.id.content)).setTextColor(ContextCompat.getColor(wrapper, textColor));
    }

}

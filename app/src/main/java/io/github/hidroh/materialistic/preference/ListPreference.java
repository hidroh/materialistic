package io.github.hidroh.materialistic.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import io.github.hidroh.materialistic.AppUtils;

public class ListPreference extends android.preference.ListPreference {
    public ListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        final TextView title = (TextView) view.findViewById(android.R.id.title);
        title.setTextColor(getContext().getResources().getColor(AppUtils.getThemedResId(
                getContext(), android.R.attr.textColorPrimaryInverse)));
        title.setEllipsize(null);
        title.setSingleLine(false);
    }
}

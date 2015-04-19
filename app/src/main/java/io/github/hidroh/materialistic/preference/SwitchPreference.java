package io.github.hidroh.materialistic.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import io.github.hidroh.materialistic.AppUtils;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class SwitchPreference extends android.preference.SwitchPreference {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchPreference(Context context) {
        super(context);
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

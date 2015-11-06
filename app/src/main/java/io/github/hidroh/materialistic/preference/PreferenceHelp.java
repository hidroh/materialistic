package io.github.hidroh.materialistic.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceGroup;
import android.util.AttributeSet;

import io.github.hidroh.materialistic.R;

public class PreferenceHelp extends PreferenceGroup {
    private final int mLayoutResId;
    private final String mTitle;

    public PreferenceHelp(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.preferenceHelpStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceHelp);
        try {
            mLayoutResId = a.getResourceId(R.styleable.PreferenceHelp_dialogLayout, 0);
            mTitle = a.getString(R.styleable.PreferenceHelp_dialogTitle);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onClick() {
        if (mLayoutResId == 0) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setTitle(mTitle)
                .setView(mLayoutResId)
                .setPositiveButton(R.string.got_it, null)
                .create()
                .show();
    }
}

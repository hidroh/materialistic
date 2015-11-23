package io.github.hidroh.materialistic.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.github.hidroh.materialistic.R;

public class CommentMaxLinesPreference extends SpinnerPreference {
    private final LayoutInflater mLayoutInflater;

    public CommentMaxLinesPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommentMaxLinesPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLayoutInflater = LayoutInflater.from(getContext());
    }

    @Override
    protected View createDropDownView(int position, ViewGroup parent) {
        return mLayoutInflater.inflate(R.layout.support_simple_spinner_dropdown_item, parent, false);
    }

    @Override
    protected void bindDropDownView(int position, View view) {
        ((TextView) view.findViewById(android.R.id.text1)).setText(mEntries[position]);
    }
}

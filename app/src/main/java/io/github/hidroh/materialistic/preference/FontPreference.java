package io.github.hidroh.materialistic.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.FontCache;
import io.github.hidroh.materialistic.R;

public class FontPreference extends SpinnerPreference {
    private final LayoutInflater mLayoutInflater;

    public FontPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FontPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLayoutInflater = LayoutInflater.from(getContext());
    }

    @Override
    protected View createDropDownView(int position, ViewGroup parent) {
        return mLayoutInflater.inflate(R.layout.support_simple_spinner_dropdown_item, parent, false);
    }

    @Override
    protected void bindDropDownView(int position, View view) {
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setTypeface(FontCache.getInstance().get(getContext(), mEntryValues[position]));
        textView.setText(mEntries[position]);
    }

    @Override
    protected boolean persistString(String value) {
        Application.TYPE_FACE = FontCache.getInstance().get(getContext(), value);
        return super.persistString(value);
    }

}

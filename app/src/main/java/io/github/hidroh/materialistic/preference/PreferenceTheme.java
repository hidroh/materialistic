package io.github.hidroh.materialistic.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.util.ArrayMap;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import io.github.hidroh.materialistic.R;

public class PreferenceTheme extends Preference {

    private static final ArrayMap<Integer, Integer> VALUE_MAP = new ArrayMap<Integer, Integer>() {{
        put(R.id.theme_light, R.string.pref_theme_value_light);
        put(R.id.theme_dark, R.string.pref_theme_value_dark);
        put(R.id.theme_sepia, R.string.pref_theme_value_sepia);
        put(R.id.theme_green, R.string.pref_theme_value_green);
    }};
    private static final ArrayMap<Integer, Integer> RES_MAP = new ArrayMap<Integer, Integer>() {{
        put(R.string.pref_theme_value_light, R.string.theme_light);
        put(R.string.pref_theme_value_dark, R.string.theme_dark);
        put(R.string.pref_theme_value_sepia, R.string.theme_sepia);
        put(R.string.pref_theme_value_green, R.string.theme_green);
    }};
    private static final ArrayMap<String, Integer> SUMMARY_MAP = new ArrayMap<>();

    public PreferenceTheme(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreferenceTheme(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        for (int i = 0; i < RES_MAP.size(); i++) {
            SUMMARY_MAP.put(context.getString(RES_MAP.keyAt(i)), RES_MAP.valueAt(i));
        }
        setWidgetLayoutResource(R.layout.preference_theme);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        String value = restorePersistedValue ? getPersistedString(null): (String) defaultValue;
        if (TextUtils.isEmpty(value)) {
            value = getContext().getString(R.string.pref_theme_value_light);
        }
        setSummary(getContext().getString(SUMMARY_MAP.get(value)));
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false);
        for (ArrayMap.Entry<Integer, Integer> entry : VALUE_MAP.entrySet()) {
            final int id = entry.getKey();
            final String value = getContext().getString(entry.getValue());
            View button = holder.findViewById(id);
            button.setClickable(true);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSummary(getContext().getString(SUMMARY_MAP.get(value)));
                    persistString(value);
                }
            });
        }
    }

}

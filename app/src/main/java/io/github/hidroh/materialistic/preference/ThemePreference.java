/*
 * Copyright (c) 2015 Ha Duy Trung
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

package io.github.hidroh.materialistic.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.ArrayMap;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import io.github.hidroh.materialistic.R;

public class ThemePreference extends Preference {

    public static final @StyleRes int THEME_DEFAULT = R.style.AppTheme;
    public static final @StyleRes int DIALOG_THEME_DEFAULT = R.style.AppAlertDialog;
    private static final String VALUE_LIGHT = "light";
    private static final String VALUE_DARK = "dark";
    private static final String VALUE_BLACK = "black";
    private static final String VALUE_SEPIA = "sepia";
    private static final String VALUE_GREEN = "green";
    private static final String VALUE_SOLARIZED = "solarized";
    private static final String VALUE_SOLARIZED_DARK = "solarized_dark";
    private static final ArrayMap<Integer, String> BUTTON_VALUE = new ArrayMap<>();
    private static final ArrayMap<String, ThemeSpec> VALUE_THEME = new ArrayMap<>();
    static {
        BUTTON_VALUE.put(R.id.theme_light, VALUE_LIGHT);
        BUTTON_VALUE.put(R.id.theme_dark, VALUE_DARK);
        BUTTON_VALUE.put(R.id.theme_black, VALUE_BLACK);
        BUTTON_VALUE.put(R.id.theme_sepia, VALUE_SEPIA);
        BUTTON_VALUE.put(R.id.theme_green, VALUE_GREEN);
        BUTTON_VALUE.put(R.id.theme_solarized, VALUE_SOLARIZED);
        BUTTON_VALUE.put(R.id.theme_solarized_dark, VALUE_SOLARIZED_DARK);

        VALUE_THEME.put(VALUE_LIGHT,
                new ThemeSpec(R.string.theme_light, R.style.AppTheme, R.style.AppAlertDialog));
        VALUE_THEME.put(VALUE_DARK,
                new ThemeSpec(R.string.theme_dark, R.style.AppTheme_Dark, R.style.AppAlertDialog_Dark));
        VALUE_THEME.put(VALUE_BLACK,
                new ThemeSpec(R.string.theme_black, R.style.AppTheme_Dark_Black, R.style.AppAlertDialog_Dark));
        VALUE_THEME.put(VALUE_SEPIA,
                new ThemeSpec(R.string.theme_sepia, R.style.AppTheme_Sepia, R.style.AppAlertDialog_Sepia));
        VALUE_THEME.put(VALUE_GREEN,
                new ThemeSpec(R.string.theme_green, R.style.AppTheme_Green, R.style.AppAlertDialog_Green));
        VALUE_THEME.put(VALUE_SOLARIZED,
                new ThemeSpec(R.string.theme_solarized, R.style.AppTheme_Solarized, R.style.AppAlertDialog_Solarized));
        VALUE_THEME.put(VALUE_SOLARIZED_DARK,
                new ThemeSpec(R.string.theme_solarized_dark, R.style.AppTheme_Dark_Solarized, R.style.AppAlertDialog_Dark_Solarized));
    }

    public static @StyleRes int getTheme(String value) {
        if (!VALUE_THEME.containsKey(value)) {
            return THEME_DEFAULT;
        }
        return VALUE_THEME.get(value).theme;
    }

    public static @StyleRes int getDialogTheme(String value) {
        if (!VALUE_THEME.containsKey(value)) {
            return DIALOG_THEME_DEFAULT;
        }
        return VALUE_THEME.get(value).dialogTheme;
    }

    public ThemePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_theme);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return VALUE_LIGHT;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        String value = restorePersistedValue ? getPersistedString(null): (String) defaultValue;
        if (TextUtils.isEmpty(value)) {
            value = VALUE_LIGHT;
        }
        setSummary(VALUE_THEME.get(value).summary);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false);
        for (int i = 0; i < BUTTON_VALUE.size(); i++) {
            final int buttonId = BUTTON_VALUE.keyAt(i);
            final String value = BUTTON_VALUE.valueAt(i);
            View button = holder.findViewById(buttonId);
            button.setClickable(true);
            button.setOnClickListener(v -> {
                setSummary(VALUE_THEME.get(value).summary);
                persistString(value);
            });
        }
    }

    private static class ThemeSpec {
        public final @StringRes int summary;
        public final @StyleRes int theme;
        public final @StyleRes int dialogTheme;

        private ThemeSpec(@StringRes int summary, @StyleRes int theme, @StyleRes int dialogTheme) {
            this.summary = summary;
            this.theme = theme;
            this.dialogTheme = dialogTheme;
        }
    }
}

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
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.collection.ArrayMap;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.annotation.Synthetic;

public class ThemePreference extends Preference {

    private static final String LIGHT = "light";
    private static final String DARK = "dark";
    private static final String BLACK = "black";
    private static final String SEPIA = "sepia";
    private static final String GREEN = "green";
    private static final String SOLARIZED = "solarized";
    private static final String SOLARIZED_DARK = "solarized_dark";
    private static final ArrayMap<Integer, String> BUTTONS = new ArrayMap<>();
    private static final ArrayMap<String, ThemeSpec> VALUES = new ArrayMap<>();
    static {
        BUTTONS.put(R.id.theme_light, LIGHT);
        BUTTONS.put(R.id.theme_dark, DARK);
        BUTTONS.put(R.id.theme_black, BLACK);
        BUTTONS.put(R.id.theme_sepia, SEPIA);
        BUTTONS.put(R.id.theme_green, GREEN);
        BUTTONS.put(R.id.theme_solarized, SOLARIZED);
        BUTTONS.put(R.id.theme_solarized_dark, SOLARIZED_DARK);

        VALUES.put(LIGHT, new DayNightSpec(R.string.theme_light));
        VALUES.put(DARK, new DarkSpec(R.string.theme_dark));
        VALUES.put(BLACK, new DarkSpec(R.string.theme_black, R.style.Black));
        VALUES.put(SEPIA, new DayNightSpec(R.string.theme_sepia, R.style.Sepia));
        VALUES.put(GREEN, new DayNightSpec(R.string.theme_green, R.style.Green));
        VALUES.put(SOLARIZED, new DayNightSpec(R.string.theme_solarized, R.style.Solarized));
        VALUES.put(SOLARIZED_DARK, new DarkSpec(R.string.theme_solarized_dark,
                R.style.Solarized_Dark));
    }

    private String mSelectedTheme;

    public static ThemeSpec getTheme(String value, boolean isTranslucent) {
        ThemeSpec themeSpec = VALUES.get(VALUES.containsKey(value) ? value : LIGHT);
        return isTranslucent ? themeSpec.getTranslucent() : themeSpec;
    }

    @SuppressWarnings("unused")
    public ThemePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThemePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.preference_theme);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return LIGHT;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        mSelectedTheme = restorePersistedValue ? getPersistedString(null): (String) defaultValue;
        if (TextUtils.isEmpty(mSelectedTheme)) {
            mSelectedTheme = LIGHT;
        }
        setSummary(VALUES.get(mSelectedTheme).summary);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false);
        for (int i = 0; i < BUTTONS.size(); i++) {
            final int buttonId = BUTTONS.keyAt(i);
            final String value = BUTTONS.valueAt(i);
            View button = holder.findViewById(buttonId);
            button.setClickable(true);
            button.setOnClickListener(v -> {
                mSelectedTheme = value;
                if (shouldDisableDependents()) {
                    Preferences.Theme.disableAutoDayNight(getContext());
                }
                setSummary(VALUES.get(value).summary);
                persistString(value);
            });
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        // assume only auto day-night is dependent
        return !(VALUES.get(mSelectedTheme) instanceof DayNightSpec);
    }

    public static class ThemeSpec {
        final @StringRes int summary;
        public final @StyleRes int theme;
        public final @StyleRes int themeOverrides;
        ThemeSpec translucent;

        @Synthetic
        ThemeSpec(@StringRes int summary, @StyleRes int theme, @StyleRes int themeOverrides) {
            this.summary = summary;
            this.theme = theme;
            this.themeOverrides = themeOverrides;
        }

        ThemeSpec getTranslucent() {
            return this;
        }
    }

    static class DarkSpec extends ThemeSpec {

        DarkSpec(@StringRes int summary) {
            this(summary, -1);
        }

        DarkSpec(@StringRes int summary, @StyleRes int themeOverrides) {
            super(summary, R.style.AppTheme_Dark, themeOverrides);
        }

        @Override
        ThemeSpec getTranslucent() {
            if (translucent == null) {
                translucent = new ThemeSpec(summary, R.style.AppTheme_Dark_Translucent, themeOverrides);
            }
            return translucent;
        }
    }

    public static class DayNightSpec extends ThemeSpec {

        DayNightSpec(@StringRes int summary) {
            this(summary, -1);
        }

        DayNightSpec(@StringRes int summary, @StyleRes int themeOverrides) {
            super(summary, R.style.AppTheme_DayNight, themeOverrides);
        }

        @Override
        ThemeSpec getTranslucent() {
            if (translucent == null) {
                translucent = new ThemeSpec(summary, R.style.AppTheme_Translucent, themeOverrides);
            }
            return translucent;
        }
    }
}

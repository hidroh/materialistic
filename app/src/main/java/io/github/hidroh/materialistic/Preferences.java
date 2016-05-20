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

package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Map;

import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.preference.ThemePreference;

public class Preferences {
    @VisibleForTesting static Boolean sReleaseNotesSeen = null;

    public enum StoryViewMode {
        Comment,
        Article,
        Readability
    }

    private static final BoolToStringPref[] PREF_MIGRATION = new BoolToStringPref[]{
            new BoolToStringPref(R.string.pref_item_click, false,
                    R.string.pref_story_display, R.string.pref_story_display_value_comments),
            new BoolToStringPref(R.string.pref_item_search_recent, true,
                    R.string.pref_search_sort, R.string.pref_search_sort_value_default)
    };

    static void sync(PreferenceManager preferenceManager) {
        Map<String, ?> map = preferenceManager.getSharedPreferences().getAll();
        for (String key : map.keySet()) {
            sync(preferenceManager, key);
        }
    }

    static void sync(PreferenceManager preferenceManager, String key) {
        Preference pref = preferenceManager.findPreference(key);
        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());
        }
    }

    /**
     * Migrate from boolean preferences to string preferences. Should be called only once
     * when application is relaunched.
     * If boolean preference has been set before, and value is not default, migrate to the new
     * corresponding string value
     * If boolean preference has been set before, but value is default, simply remove it
     * @param context   application context
     * TODO remove once all users migrated
     */
    static void migrate(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        for (BoolToStringPref pref : PREF_MIGRATION) {
            if (pref.isChanged(context, sp)) {
                editor.putString(context.getString(pref.newKey), context.getString(pref.newValue));
            }

            if (pref.hasOldValue(context, sp)) {
                editor.remove(context.getString(pref.oldKey));
            }
        }

        editor.apply();
    }

    static boolean isListItemCardView(Context context) {
        return get(context, R.string.pref_list_item_view, false);
    }

    static void setListItemCardView(Context context, boolean isCardView) {
        set(context, R.string.pref_list_item_view, isCardView);
    }

    public static boolean isSortByRecent(Context context) {
        return get(context, R.string.pref_search_sort, R.string.pref_search_sort_value_recent)
                .equals(context.getString(R.string.pref_search_sort_value_recent));
    }

    public static void setSortByRecent(Context context, boolean byRecent) {
        set(context, R.string.pref_search_sort, context.getString(byRecent ?
                R.string.pref_search_sort_value_recent : R.string.pref_search_sort_value_default));
    }

    public static StoryViewMode getDefaultStoryView(Context context) {
        String pref = get(context, R.string.pref_story_display,
                        R.string.pref_story_display_value_article);
        if (TextUtils.equals(context.getString(R.string.pref_story_display_value_comments), pref)) {
            return StoryViewMode.Comment;
        }
        if (TextUtils.equals(context.getString(R.string.pref_story_display_value_readability), pref)) {
            return StoryViewMode.Readability;
        }
        return StoryViewMode.Article;
    }

    static boolean externalBrowserEnabled(Context context) {
        return get(context, R.string.pref_external, false);
    }

    static boolean colorCodeEnabled(Context context) {
        return get(context, R.string.pref_color_code, true);
    }

    static void setColorCodeEnabled(Context context, boolean enabled) {
        set(context, R.string.pref_color_code, enabled);
    }

    static boolean highlightUpdatedEnabled(Context context) {
        return get(context, R.string.pref_highlight_updated, true);
    }

    static boolean customChromeTabEnabled(Context context) {
        return get(context, R.string.pref_custom_tab, true);
    }

    static boolean isSinglePage(Context context, String displayOption) {
        return !TextUtils.equals(displayOption,
                context.getString(R.string.pref_comment_display_value_multiple));
    }

    static boolean isAutoExpand(Context context, String displayOption) {
        return TextUtils.equals(displayOption,
                context.getString(R.string.pref_comment_display_value_single));
    }

    static String getCommentDisplayOption(Context context) {
        return get(context, R.string.pref_comment_display,
                        R.string.pref_comment_display_value_single);
    }

    static void setCommentDisplayOption(Context context, String choice) {
        set(context, R.string.pref_comment_display, choice);
    }

    static void setPopularRange(Context context, @AlgoliaPopularClient.Range @NonNull String range) {
        set(context, R.string.pref_popular_range, range);
    }

    @NonNull
    static String getPopularRange(Context context) {
        return get(context, R.string.pref_popular_range, AlgoliaPopularClient.LAST_24H);
    }

    static int getCommentMaxLines(Context context) {
        String maxLinesString = get(context, R.string.pref_max_lines, null);
        int maxLines = maxLinesString == null ? -1 : Integer.parseInt(maxLinesString);
        if (maxLines < 0) {
            maxLines = Integer.MAX_VALUE;
        }
        return maxLines;
    }

    static void setCommentMaxLines(Context context, String choice) {
        set(context, R.string.pref_max_lines, choice);
    }

    static boolean shouldLazyLoad(Context context) {
        return get(context, R.string.pref_lazy_load, true);
    }

    static String getUsername(Context context) {
        return get(context, R.string.pref_username, null);
    }

    public static void setUsername(Context context, String username) {
        set(context, R.string.pref_username, username);
    }

    @NonNull
    static String getLaunchScreen(Context context) {
        return get(context, R.string.pref_launch_screen, R.string.pref_launch_screen_value_top);
    }

    static boolean adBlockEnabled(Context context) {
        return get(context, R.string.pref_ad_block, true);
    }

    static boolean isReleaseNotesSeen(Context context) {
        if (sReleaseNotesSeen == null) {
            PackageInfo info = null;
            try {
                info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                // no op
            }
            // considered seen if first time install or last seen release is up to date
            sReleaseNotesSeen = info != null && info.firstInstallTime == info.lastUpdateTime ||
                    getInt(context, R.string.pref_latest_release, 0) >= BuildConfig.LATEST_RELEASE;
        }
        return sReleaseNotesSeen;
    }

    static void setReleaseNotesSeen(Context context) {
        sReleaseNotesSeen = true;
        setInt(context, R.string.pref_latest_release, BuildConfig.LATEST_RELEASE);
    }

    public static void reset(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .clear()
                .apply();
    }

    private static boolean get(Context context, @StringRes int key, boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(key), defaultValue);
    }

    private static int getInt(Context context, @StringRes int key, int defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(context.getString(key), defaultValue);
    }

    private static String get(Context context, @StringRes int key, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(key), defaultValue);
    }

    private static String get(Context context, @StringRes int key, @StringRes int defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(key), context.getString(defaultValue));
    }

    private static void set(Context context, @StringRes int key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(key), value)
                .apply();
    }

    private static void setInt(Context context, @StringRes int key, int value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(context.getString(key), value)
                .apply();
    }

    private static void set(Context context, @StringRes int key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getString(key), value)
                .apply();
    }

    private static class BoolToStringPref {
        private final int oldKey;
        private final boolean oldDefault;
        private final int newKey;
        private final int newValue;

        private BoolToStringPref(@StringRes int oldKey, boolean oldDefault,
                                 @StringRes int newKey, @StringRes int newValue) {
            this.oldKey = oldKey;
            this.oldDefault = oldDefault;
            this.newKey = newKey;
            this.newValue = newValue;
        }

        private boolean isChanged(Context context, SharedPreferences sp) {
            return hasOldValue(context, sp) &&
                    sp.getBoolean(context.getString(oldKey), oldDefault) != oldDefault;
        }

        private boolean hasOldValue(Context context, SharedPreferences sp) {
            return sp.contains(context.getString(oldKey));
        }
    }

    public static class Theme {

        public static void apply(Context context, boolean dialogTheme) {
            int theme = getTheme(context, dialogTheme);
            if (dialogTheme || theme != ThemePreference.THEME_DEFAULT) {
                context.setTheme(theme);
            }
        }

        static @Nullable String getTypeface(Context context) {
            return get(context, R.string.pref_font, null);
        }

        static @Nullable String getReadabilityTypeface(Context context) {
            String typefaceName = get(context, R.string.pref_readability_font, null);
            if (TextUtils.isEmpty(typefaceName)) {
                return getTypeface(context);
            }
            return typefaceName;
        }

        static void savePreferredReadabilityTypeface(Context context, String typefaceName) {
            set(context, R.string.pref_readability_font, typefaceName);
        }

        public static @StyleRes int resolveTextSize(String choice) {
            switch (Integer.parseInt(choice)) {
                case -1:
                    return R.style.AppTextSize_XSmall;
                case 0:
                default:
                    return R.style.AppTextSize;
                case 1:
                    return R.style.AppTextSize_Medium;
                case 2:
                    return R.style.AppTextSize_Large;
                case 3:
                    return R.style.AppTextSize_XLarge;
            }
        }

        static @StyleRes int resolvePreferredTextSize(Context context) {
            return resolveTextSize(getPreferredTextSize(context));
        }

        static @StyleRes int resolvePreferredReadabilityTextSize(Context context) {
            return resolveTextSize(getPreferredReadabilityTextSize(context));
        }

        static @NonNull String getPreferredReadabilityTextSize(Context context) {
            String choice = get(context, R.string.pref_readability_text_size, null);
            if (TextUtils.isEmpty(choice)) {
                return getPreferredTextSize(context);
            }
            return choice;
        }

        static void savePreferredReadabilityTextSize(Context context, String choice) {
            set(context, R.string.pref_readability_text_size, choice);
        }

        private static @NonNull String getPreferredTextSize(Context context) {
            return get(context, R.string.pref_text_size, String.valueOf(0));
        }

        private static @StyleRes int getTheme(Context context, boolean dialogTheme) {
            String choice = get(context, R.string.pref_theme, null);
            if (dialogTheme) {
                return ThemePreference.getDialogTheme(choice);
            } else {
                return ThemePreference.getTheme(choice);
            }
        }
    }

    public static class Offline {

        public static boolean isEnabled(Context context) {
            return get(context, R.string.pref_saved_item_sync, false);
        }

        public static boolean isCommentsEnabled(Context context) {
            return isEnabled(context) &&
                    get(context, R.string.pref_offline_comments, true);
        }

        public static boolean isArticleEnabled(Context context) {
            return isEnabled(context) &&
                    get(context, R.string.pref_offline_article, true);
        }

        public static boolean isReadabilityEnabled(Context context) {
            return isEnabled(context) &&
                    get(context, R.string.pref_offline_readability, true);
        }

        public static boolean currentConnectionEnabled(Context context) {
            return !isWifiOnly(context) || AppUtils.isOnWiFi(context);
        }

        public static boolean isNotificationEnabled(Context context) {
            return get(context, R.string.pref_offline_notification, false);
        }

        private static boolean isWifiOnly(Context context) {
            String wifiValue = context.getString(R.string.offline_data_wifi);
            return TextUtils.equals(wifiValue, get(context, R.string.pref_offline_data, wifiValue));
        }
    }
}

package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Map;

import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.preference.ThemePreference;

public class Preferences {
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

    public static void sync(PreferenceManager preferenceManager) {
        Map<String, ?> map = preferenceManager.getSharedPreferences().getAll();
        for (String key : map.keySet()) {
            sync(preferenceManager, key);
        }
    }

    public static void sync(PreferenceManager preferenceManager, String key) {
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
    public static void migrate(Context context) {
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

    public static boolean isSortByRecent(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_search_sort),
                        context.getString(R.string.pref_search_sort_value_recent))
                .equals(context.getString(R.string.pref_search_sort_value_recent));
    }

    public static void setSortByRecent(Context context, boolean byRecent) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getString(R.string.pref_search_sort),
                        context.getString(byRecent ?
                                R.string.pref_search_sort_value_recent :
                                R.string.pref_search_sort_value_default))
                .apply();
    }

    public static StoryViewMode getDefaultStoryView(Context context) {
        String pref = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_story_display),
                        context.getString(R.string.pref_story_display_value_article));
        if (TextUtils.equals(context.getString(R.string.pref_story_display_value_comments), pref)) {
            return StoryViewMode.Comment;
        }
        if (TextUtils.equals(context.getString(R.string.pref_story_display_value_readability), pref)) {
            return StoryViewMode.Readability;
        }
        return StoryViewMode.Article;
    }

    public static boolean externalBrowserEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_external), false);
    }

    public static boolean colorCodeEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_color_code), true);
    }

    public static boolean highlightUpdatedEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_highlight_updated), true);
    }

    public static boolean customChromeTabEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_custom_tab), true);
    }

    public static boolean isDefaultSinglePageComments(Context context) {
        return !PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_comment_display),
                        context.getString(R.string.pref_comment_display_value_multiple))
                .equals(context.getString(R.string.pref_comment_display_value_multiple));
    }

    public static boolean shouldAutoExpandComments(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_comment_display),
                        context.getString(R.string.pref_comment_display_value_multiple))
                .equals(context.getString(R.string.pref_comment_display_value_single));
    }

    public static void setPopularRange(Context context, @AlgoliaPopularClient.Range @NonNull String range) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getString(R.string.pref_popular_range), range)
                .apply();
    }

    @NonNull
    public static String getPopularRange(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_popular_range), AlgoliaPopularClient.LAST_24H);
    }

    public static int getCommentMaxLines(Context context) {
        String maxLinesString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_max_lines), null);
        int maxLines = maxLinesString == null ? -1 : Integer.parseInt(maxLinesString);
        if (maxLines < 0) {
            maxLines = Integer.MAX_VALUE;
        }
        return maxLines;
    }

    public static void reset(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .clear()
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

        public static void apply(Context context) {
            int theme = getTheme(context);
            if (theme != ThemePreference.THEME_DEFAULT) {
                context.setTheme(theme);
            }
            context.getTheme().applyStyle(resolvePreferredTextSize(context), true);
        }

        public static @Nullable String getTypeface(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(context.getString(R.string.pref_font), null);
        }

        public static @Nullable String getReadabilityTypeface(Context context) {
            String typefaceName = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(context.getString(R.string.pref_readability_font), null);
            if (TextUtils.isEmpty(typefaceName)) {
                return getTypeface(context);
            }
            return typefaceName;
        }

        public static void savePreferredReadabilityTypeface(Context context, String typefaceName) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(context.getString(R.string.pref_readability_font), typefaceName)
                    .apply();
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

        public static @StyleRes int resolvePreferredTextSize(Context context) {
            return resolveTextSize(getPreferredTextSize(context));
        }

        public static @StyleRes int resolvePreferredReadabilityTextSize(Context context) {
            return resolveTextSize(getPreferredReadabilityTextSize(context));
        }

        public static @NonNull String getPreferredReadabilityTextSize(Context context) {
            String choice = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(context.getString(R.string.pref_readability_text_size), null);
            if (TextUtils.isEmpty(choice)) {
                return getPreferredTextSize(context);
            }
            return choice;
        }

        public static void savePreferredReadabilityTextSize(Context context, String choice) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(context.getString(R.string.pref_readability_text_size), choice)
                    .apply();
        }

        private static @NonNull String getPreferredTextSize(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(context.getString(R.string.pref_text_size), String.valueOf(0));
        }

        private static @StyleRes int getTheme(Context context) {
            String choice = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(context.getString(R.string.pref_theme), null);
            return ThemePreference.getTheme(choice);
        }
    }
}

package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.StringRes;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.view.ContextThemeWrapper;

import java.util.Map;

public class Preferences {

    private static final BoolToStringPref[] PREF_MIGRATION = new BoolToStringPref[]{
            new BoolToStringPref(R.string.pref_dark_theme, false,
                    R.string.pref_theme, R.string.pref_theme_value_dark),
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

    public static boolean isDefaultOpenComments(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_story_display),
                        context.getString(R.string.pref_story_display_value_article))
                .equals(context.getString(R.string.pref_story_display_value_comments));
    }

    public static boolean externalBrowserEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_external), false);
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

    public static void applyTheme(ContextThemeWrapper contextThemeWrapper) {
        if (Preferences.darkThemeEnabled(contextThemeWrapper)) {
            contextThemeWrapper.setTheme(R.style.AppTheme_Dark);
        }

        contextThemeWrapper.getTheme().applyStyle(Preferences.resolveTextSizeResId(contextThemeWrapper), true);
    }

    private static boolean darkThemeEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_theme),
                        context.getString(R.string.pref_theme_value_light))
                .equals(context.getString(R.string.pref_theme_value_dark));
    }

    private static int resolveTextSizeResId(Context context) {
        String choice = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_text_size), String.valueOf(0));
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
}

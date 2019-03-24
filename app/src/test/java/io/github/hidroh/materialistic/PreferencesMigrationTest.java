package io.github.hidroh.materialistic;

import android.preference.PreferenceManager;
import androidx.annotation.StringRes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class PreferencesMigrationTest {
    private final int oldKey;
    private final boolean oldValue;
    private final int newKey;
    private final int newValue;

    public PreferencesMigrationTest(@StringRes int oldKey, boolean oldValue,
                                    @StringRes int newKey, @StringRes int newValue) {
        this.oldKey = oldKey;
        this.oldValue = oldValue;
        this.newKey = newKey;
        this.newValue = newValue;
    }

    @ParameterizedRobolectricTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{R.string.pref_item_click, true,
                        R.string.pref_story_display, R.string.pref_story_display_value_comments},
                new Object[]{R.string.pref_item_search_recent, false,
                        R.string.pref_search_sort, R.string.pref_search_sort_value_default}
        );
    }

    @Test
    public void testMigrate() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putBoolean(RuntimeEnvironment.application.getString(oldKey), oldValue)
                .apply();
        assertTrue(PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .contains(RuntimeEnvironment.application.getString(oldKey)));
        Preferences.migrate(RuntimeEnvironment.application);
        assertFalse(PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .contains(RuntimeEnvironment.application.getString(oldKey)));
        assertEquals(RuntimeEnvironment.application.getString(newValue),
                PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
        .getString(RuntimeEnvironment.application.getString(newKey), null));
    }

    @Test
    public void testNoMigrate() {
        assertFalse(PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .contains(RuntimeEnvironment.application.getString(oldKey)));
        Preferences.migrate(RuntimeEnvironment.application);
        assertNull(RuntimeEnvironment.application.getString(newValue),
                PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                        .getString(RuntimeEnvironment.application.getString(newKey), null));
    }

    @Test
    public void testNoMigrateDefault() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putBoolean(RuntimeEnvironment.application.getString(oldKey), !oldValue)
                .apply();
        assertTrue(PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .contains(RuntimeEnvironment.application.getString(oldKey)));
        Preferences.migrate(RuntimeEnvironment.application);
        assertFalse(PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .contains(RuntimeEnvironment.application.getString(oldKey)));
        assertNull(RuntimeEnvironment.application.getString(newValue),
                PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                        .getString(RuntimeEnvironment.application.getString(newKey), null));
    }
}

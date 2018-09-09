package io.github.hidroh.materialistic;

import android.app.Activity;
import android.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import io.github.hidroh.materialistic.test.TestRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunner.class)
public class PreferencesThemeTest {

    private Activity activity;

    @Before
    public void setUp() {
        activity = Robolectric.setupActivity(Activity.class);
    }

    @Test
    public void testDefaultTheme() {
        Preferences.Theme.apply(activity, false, false);
        assertThat(shadowOf(activity).callGetThemeResId()).isEqualTo(R.style.AppTheme_DayNight);
    }

    @Test
    public void testDarkTheme() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_theme), "dark")
                .apply();
        Preferences.Theme.apply(activity, false, false);
        assertThat(shadowOf(activity).callGetThemeResId()).isEqualTo(R.style.AppTheme_Dark);
    }
}

package io.github.hidroh.materialistic;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.data.AlgoliaClient;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class SettingsActivityTest {
    private ActivityController<SettingsActivity> controller;
    private SettingsActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(SettingsActivity.class);
        activity = controller.create().start().resume().visible().get();
    }

    @Test
    public void testPrefSearch() {
        assertTrue(AlgoliaClient.sSortByTime);
        String key = activity.getString(R.string.pref_item_search_recent);
        activity.getSharedPreferences("io.github.hidroh.materialistic_preferences", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, false)
                .commit();
        assertFalse(AlgoliaClient.sSortByTime);
    }

    @Test
    public void testPrefTheme() {
        String key = activity.getString(R.string.pref_dark_theme);
        activity.getSharedPreferences("io.github.hidroh.materialistic_preferences", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, true)
                .commit();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testPrefFont() {
        String key = activity.getString(R.string.pref_text_size);
        activity.getSharedPreferences("io.github.hidroh.materialistic_preferences", Context.MODE_PRIVATE)
                .edit()
                .putString(key, "1")
                .commit();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @After
    public void tearDown() {
        AlgoliaClient.sSortByTime = true;
        controller.pause().stop().destroy();
    }
}

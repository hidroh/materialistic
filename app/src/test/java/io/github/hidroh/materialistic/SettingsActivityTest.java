package io.github.hidroh.materialistic;

import android.app.AlertDialog;
import android.content.DialogInterface;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.test.ShadowSearchRecentSuggestions;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowSearchRecentSuggestions.class, ShadowSupportPreferenceManager.class})
@RunWith(RobolectricGradleTestRunner.class)
public class SettingsActivityTest {
    private SettingsActivity activity;
    private ActivityController<SettingsActivity> controller;
    private SettingsFragment fragment;

    @Before
    public void setUp() {
        TestApplication.applicationGraph.inject(this);
        controller = Robolectric.buildActivity(SettingsActivity.class);
        activity = controller.create().start().resume().visible().get();
        fragment = (SettingsFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(SettingsFragment.class.getName());
    }

    @Test
    public void testClearRecentSearches() {
        ShadowSearchRecentSuggestions.historyClearCount = 0;
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_clear_recent));
        shadowOf(activity).clickMenuItem(R.id.menu_clear_recent);
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertEquals(1, ShadowSearchRecentSuggestions.historyClearCount);
    }

    @Test
    public void testPrefSearch() {
        assertTrue(AlgoliaClient.sSortByTime);
        String key = activity.getString(R.string.pref_search_sort);
        // trigger listener
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(key, activity.getString(R.string.pref_search_sort_value_default))
                .commit();
        fragment.mListener.onSharedPreferenceChanged(
                ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity), key);
        assertFalse(AlgoliaClient.sSortByTime);
    }

    @Test
    public void testPrefTheme() {
        String key = activity.getString(R.string.pref_theme);
        // trigger listener
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(key, activity.getString(R.string.pref_theme_value_dark))
                .commit();
        fragment.mListener.onSharedPreferenceChanged(
                ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity), key);
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testPrefFont() {
        String key = activity.getString(R.string.pref_text_size);
        // trigger listener
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(key, "1")
                .commit();
        fragment.mListener.onSharedPreferenceChanged(
                ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity), key);
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @After
    public void tearDown() {
        AlgoliaClient.sSortByTime = true;
        controller.pause().stop().destroy();
    }
}

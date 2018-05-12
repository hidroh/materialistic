package io.github.hidroh.materialistic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.PreferenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.android.controller.ActivityController;

import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowSearchRecentSuggestions;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowSearchRecentSuggestions.class})
@RunWith(TestRunner.class)
public class SettingsActivityTest {
    private SettingsActivity activity;
    private ActivityController<SettingsActivity> controller;

    @Before
    public void setUp() {
        TestApplication.applicationGraph.inject(this);
        controller = Robolectric.buildActivity(SettingsActivity.class);
        activity = controller.create().postCreate(null).start().resume().visible().get();
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
    public void testReset() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_color_code), false)
                .apply();
        assertFalse(Preferences.colorCodeEnabled(activity));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_reset));
        shadowOf(activity).clickMenuItem(R.id.menu_reset);
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertTrue(Preferences.colorCodeEnabled(activity));
    }

    @Test
    public void testClearDrafts() {
        Preferences.saveDraft(activity, "1", "draft");
        shadowOf(activity).clickMenuItem(R.id.menu_clear_drafts);
        assertThat(Preferences.getDraft(activity, "1")).isNullOrEmpty();
    }

    @Test
    public void testAbout() {
        activity.findViewById(R.id.drawer_about).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, AboutActivity.class);
    }

    @Test
    public void testReleaseNotes() {
        activity.findViewById(R.id.drawer_release).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ReleaseNotesActivity.class);
    }

    @Test
    public void testDisplay() {
        activity.findViewById(R.id.drawer_display).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, PreferencesActivity.class);
    }

    @Test
    public void testOffline() {
        activity.findViewById(R.id.drawer_offline).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, PreferencesActivity.class);
    }

    @Test
    public void testList() {
        activity.findViewById(R.id.menu_list).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, PreferencesActivity.class);
    }

    @Test
    public void testComments() {
        activity.findViewById(R.id.menu_comments).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, PreferencesActivity.class);
    }

    @Test
    public void testReadability() {
        activity.findViewById(R.id.menu_readability).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, PreferencesActivity.class);
    }

    @After
    public void tearDown() {
        AlgoliaClient.sSortByTime = true;
        controller.pause().stop().destroy();
    }
}

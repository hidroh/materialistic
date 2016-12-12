package io.github.hidroh.materialistic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowResolveInfo;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.TestWebItem;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.shadow.ShadowSnackbar;
import io.github.hidroh.materialistic.test.suite.SlowTest;

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.appcompat.v7.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@Category(SlowTest.class)
@RunWith(TestRunner.class)
public class BaseListActivityTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create().postCreate(null).start().resume().visible().get();
    }

    @Test
    public void testCreate() {
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
    }

    @Config(shadows = ShadowSnackbar.class)
    @Test
    public void testShowReleaseNotes() {
        controller.pause().stop().destroy();
        Preferences.sReleaseNotesSeen = false;
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create().postCreate(null).start().resume().visible().get();
        View snackbarView = ShadowSnackbar.getLatestView();
        assertNotNull(snackbarView);
        snackbarView.findViewById(R.id.snackbar_action).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ReleaseNotesActivity.class);
        Preferences.sReleaseNotesSeen = null;
    }

    @Test
    public void testRotate() {
        Bundle savedState = new Bundle();
        activity.onSaveInstanceState(savedState);
        RuntimeEnvironment.setQualifiers("w820dp-land");
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create(savedState).postCreate(null).start().resume().visible().get();
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
    }

    @Test
    public void testSelectItemOpenWeb() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }
        });
        Intent actual = shadowOf(activity).getNextStartedActivity();
        assertEquals(ItemActivity.class.getName(), actual.getComponent().getClassName());
    }

    @Test
    public void testSelectItemOpenExternal() {
        RobolectricPackageManager packageManager = (RobolectricPackageManager)
                RuntimeEnvironment.application.getPackageManager();
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://example.com")),
                ShadowResolveInfo.newResolveInfo("label", "com.android.chrome", "DefaultActivity"));
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_external), true)
                .commit();
        controller.pause().resume();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }
        });
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_VIEW);
    }

    @Test
    public void testSelectItemStartActionView() {
        RobolectricPackageManager packageManager = (RobolectricPackageManager)
                RuntimeEnvironment.application.getPackageManager();
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://example.com")),
                ShadowResolveInfo.newResolveInfo("label", "com.android.chrome", "DefaultActivity"));
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://example.com")),
                ShadowResolveInfo.newResolveInfo("label", "com.android.browser", "DefaultActivity"));
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_external), true)
                .commit();
        controller.pause().resume();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }
        });
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_VIEW);
    }

    @Test
    public void testSelectItemOpenChooser() {
        RobolectricPackageManager packageManager = (RobolectricPackageManager)
                RuntimeEnvironment.application.getPackageManager();
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://news.ycombinator.com/item?id=1")),
                ShadowResolveInfo.newResolveInfo("label", "com.android.chrome", "DefaultActivity"));
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://news.ycombinator.com/item?id=1")),
                ShadowResolveInfo.newResolveInfo("label", "com.android.browser", "DefaultActivity"));
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_external), true)
                .commit();
        controller.pause().resume();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://news.ycombinator.com/item?id=1";
            }
        });
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_CHOOSER);
    }

    @Test
    public void testSelectItemOpenItem() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                        .edit()
                        .putString(activity.getString(R.string.pref_story_display),
                                activity.getString(R.string.pref_story_display_value_comments))
                        .commit();
        controller.pause().resume();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getId() {
                return "1";
            }
        });
        assertEquals(ItemActivity.class.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    @Test
    public void testGetSelectedItem() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        assertNotNull(activity.getSelectedItem());
    }

    @Test
    public void testLastUpdated() {
        String expected = activity.getString(R.string.last_updated,
                DateUtils.getRelativeTimeSpanString(System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL));
        activity.onRefreshed();
        assertThat(activity.getSupportActionBar()).hasSubtitle(expected);
        activity.getSupportActionBar().setSubtitle(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertThat(activity.getSupportActionBar()).hasSubtitle(expected);
        activity.getSupportActionBar().setSubtitle(null);
        controller.pause().resume();
        assertThat(activity.getSupportActionBar()).hasSubtitle(expected);
        Bundle savedState = new Bundle();
        activity.onSaveInstanceState(savedState);
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create(savedState).postCreate(null).start().resume().visible().get();
        assertThat(activity.getSupportActionBar()).hasSubtitle(expected);
    }

    @Config(shadows = ShadowRecyclerView.class)
    @Test
    public void testScrollToTop() {
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        recyclerView.smoothScrollToPosition(1);
        assertThat(customShadowOf(recyclerView).getScrollPosition()).isEqualTo(1);
        activity.findViewById(R.id.toolbar).performClick();
        assertThat(customShadowOf(recyclerView).getScrollPosition()).isEqualTo(0);
    }

    @Test
    public void testBackPressed() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_launch_screen),
                        activity.getString(R.string.pref_launch_screen_value_last))
                .apply();
        activity.onBackPressed();
        assertThat(activity).isNotFinishing();
    }

    @Test
    public void testBackPressedFinish() {
        activity.onBackPressed();
        assertThat(activity).isFinishing();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

package io.github.hidroh.materialistic;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowResolveInfo;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestWebItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.appcompat.v7.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

// TODO switch to API 21 once ShareActionProvider is fixed
@Config(sdk = 19, shadows = {ShadowSupportPreferenceManager.class, ShadowRecyclerView.class})
@RunWith(RobolectricGradleTestRunner.class)
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
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment));
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story));
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
    }

    @Test
    public void testRotate() {
        RuntimeEnvironment.setQualifiers("w820dp-land");
        activity.onConfigurationChanged(RuntimeEnvironment.application.getResources().getConfiguration());
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story));
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
        assertThat(actual).hasExtra(ItemActivity.EXTRA_OPEN_ARTICLE);
        assertTrue(actual.getBooleanExtra(ItemActivity.EXTRA_OPEN_ARTICLE, false));
    }

    @Test
    public void testSelectItemOpenExternal() {
        RobolectricPackageManager packageManager = (RobolectricPackageManager)
                RuntimeEnvironment.application.getPackageManager();
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://example.com")),
                ShadowResolveInfo.newResolveInfo("label", "com.android.chrome", "DefaultActivity"));
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
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
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_CHOOSER);
    }

    @Test
    public void testSelectItemOpenItem() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
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
            public boolean isShareable() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        assertNull(activity.getSelectedItem());
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
    }

    @Test
    public void testScrollToTop() {
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        recyclerView.smoothScrollToPosition(1);
        assertEquals(1, ((ShadowRecyclerView) ShadowExtractor.extract(recyclerView))
                .getSmoothScrollToPosition());
        activity.findViewById(R.id.toolbar).performClick();
        assertEquals(0, ((ShadowRecyclerView) ShadowExtractor.extract(recyclerView))
                .getSmoothScrollToPosition());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

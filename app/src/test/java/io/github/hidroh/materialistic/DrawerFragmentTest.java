package io.github.hidroh.materialistic;

import android.app.Activity;
import android.support.v4.widget.DrawerLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.util.ActivityController;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.materialistic.test.ParameterizedRobolectricGradleTestRunner;
import io.github.hidroh.materialistic.test.TestListActivity;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.support.v4.Shadows.shadowOf;

@RunWith(ParameterizedRobolectricGradleTestRunner.class)
public class DrawerFragmentTest {
    private final int drawerResId;
    private final Class<? extends Activity> startedActivity;
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;

    public DrawerFragmentTest(int drawerResId, Class<? extends Activity> startedActivity) {
        this.drawerResId = drawerResId;
        this.startedActivity = startedActivity;
    }

    @ParameterizedRobolectricGradleTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{R.id.drawer_account, LoginActivity.class},
                new Object[]{R.id.drawer_list, ListActivity.class},
                new Object[]{R.id.drawer_new, NewActivity.class},
                new Object[]{R.id.drawer_show, ShowActivity.class},
                new Object[]{R.id.drawer_ask, AskActivity.class},
                new Object[]{R.id.drawer_job, JobsActivity.class},
                new Object[]{R.id.drawer_settings, SettingsActivity.class},
                new Object[]{R.id.drawer_about, AboutActivity.class},
                new Object[]{R.id.drawer_favorite, FavoriteActivity.class},
                new Object[]{R.id.drawer_popular, PopularActivity.class},
                new Object[]{R.id.drawer_submit, SubmitActivity.class}
        );
    }

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestListActivity.class)
                .create()
                .postCreate(null)
                .start()
                .resume()
                .visible();
        activity = controller.get();
        shadowOf(activity).clickMenuItem(android.R.id.home);
    }

    @Test
    public void test() {
        shadowOf((DrawerLayout) activity.findViewById(R.id.drawer_layout))
                .getDrawerListener()
                .onDrawerClosed(activity.findViewById(R.id.drawer));
        assertNull(shadowOf(activity).getNextStartedActivity());
        activity.findViewById(drawerResId).performClick();
        shadowOf((DrawerLayout) activity.findViewById(R.id.drawer_layout))
                .getDrawerListener()
                .onDrawerClosed(activity.findViewById(R.id.drawer));
        assertEquals(startedActivity.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

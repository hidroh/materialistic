package io.github.hidroh.materialistic;

import android.annotation.SuppressLint;
import android.app.Activity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ActivityController;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.materialistic.test.ParameterizedTestRunner;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.shadow.ShadowSupportDrawerLayout;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowSupportDrawerLayout.class})
@RunWith(ParameterizedTestRunner.class)
public class DrawerActivityTest {
    private final int drawerResId;
    private final Class<? extends Activity> startedActivity;
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;

    public DrawerActivityTest(int drawerResId, Class<? extends Activity> startedActivity) {
        this.drawerResId = drawerResId;
        this.startedActivity = startedActivity;
    }

    @ParameterizedTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{R.id.drawer_account, LoginActivity.class},
                new Object[]{R.id.drawer_list, ListActivity.class},
                new Object[]{R.id.drawer_best, BestActivity.class},
                new Object[]{R.id.drawer_new, NewActivity.class},
                new Object[]{R.id.drawer_show, ShowActivity.class},
                new Object[]{R.id.drawer_ask, AskActivity.class},
                new Object[]{R.id.drawer_job, JobsActivity.class},
                new Object[]{R.id.drawer_settings, SettingsActivity.class},
                new Object[]{R.id.drawer_favorite, FavoriteActivity.class},
                new Object[]{R.id.drawer_popular, PopularActivity.class},
                new Object[]{R.id.drawer_submit, SubmitActivity.class},
                new Object[]{R.id.drawer_feedback, FeedbackActivity.class}
        );
    }

    @SuppressLint("InlinedApi")
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
        ((ShadowSupportDrawerLayout) ShadowExtractor.extract(activity.findViewById(R.id.drawer_layout)))
                .getDrawerListeners().get(0)
                .onDrawerClosed(activity.findViewById(R.id.drawer));
        assertNull(shadowOf(activity).getNextStartedActivity());
        activity.findViewById(drawerResId).performClick();
        ((ShadowSupportDrawerLayout) ShadowExtractor.extract(activity.findViewById(R.id.drawer_layout)))
                .getDrawerListeners().get(0)
                .onDrawerClosed(activity.findViewById(R.id.drawer));
        assertEquals(startedActivity.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

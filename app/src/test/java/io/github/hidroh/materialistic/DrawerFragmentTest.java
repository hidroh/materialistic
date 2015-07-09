package io.github.hidroh.materialistic;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import java.util.Arrays;
import java.util.List;

import io.github.hidroh.materialistic.test.TestListActivity;

import static junit.framework.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class DrawerFragmentTest {
    private final int drawerResId;
    private final Class<? extends Activity> startedActivity;
    private DrawerFragment fragment;

    public DrawerFragmentTest(int drawerResId, Class<? extends Activity> startedActivity) {
        this.drawerResId = drawerResId;
        this.startedActivity = startedActivity;
    }

    @ParameterizedRobolectricTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{R.id.drawer_list, ListActivity.class},
                new Object[]{R.id.drawer_new, NewActivity.class},
                new Object[]{R.id.drawer_show, ShowActivity.class},
                new Object[]{R.id.drawer_ask, AskActivity.class},
                new Object[]{R.id.drawer_job, JobsActivity.class},
                new Object[]{R.id.drawer_settings, ActionBarSettingsActivity.class},
                new Object[]{R.id.drawer_about, AboutActivity.class},
                new Object[]{R.id.drawer_favorite, FavoriteActivity.class}
        );
    }

    @Before
    public void setUp() {
        fragment = new DrawerFragment();
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestListActivity.class,
                android.R.id.content);
    }

    @Test
    public void test() {
        fragment.getView().findViewById(drawerResId).performClick();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(startedActivity.getName(),
                shadowOf(fragment.getActivity()).getNextStartedActivity().getComponent().getClassName());
    }
}

package io.github.hidroh.materialistic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

import static org.assertj.android.support.v4.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class BaseListActivityTest {
    private ActivityController<ShowActivity> controller;
    private ShowActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(ShowActivity.class);
        activity = controller.create().start().resume().get();
    }

    @Test
    public void testCreate() {
        assertThat(activity.getSupportFragmentManager()).hasFragmentWithId(android.R.id.list);
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

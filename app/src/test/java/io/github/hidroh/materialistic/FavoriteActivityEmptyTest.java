package io.github.hidroh.materialistic;

import android.support.v4.content.ShadowContentResolverCompatJellybean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowContentResolverCompatJellybean.class})
@RunWith(RobolectricGradleTestRunner.class)
public class FavoriteActivityEmptyTest {
    private ActivityController<FavoriteActivity> controller;
    private FavoriteActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(FavoriteActivity.class);
        activity = controller.create().start().resume().visible().get(); // skip menu due to search view
    }

    @Test
    public void testEmpty() {
        assertThat(activity.findViewById(R.id.empty)).isVisible();
        activity.findViewById(R.id.header_card_view).performLongClick();
        assertThat(activity.findViewById(R.id.header_card_view)
                .findViewById(R.id.bookmarked)).isVisible();
        activity.findViewById(R.id.header_card_view).performLongClick();
        assertThat(activity.findViewById(R.id.header_card_view)
                .findViewById(R.id.bookmarked)).isNotVisible();
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_clear));
        assertThat(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_search)).isNotVisible();
    }

    @Test
    public void testDataChanged() {
        assertThat(activity.findViewById(R.id.empty_search)).isNotVisible();
        activity.onDataChanged(true, "query");
        assertThat(activity.findViewById(R.id.empty_search)).isVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

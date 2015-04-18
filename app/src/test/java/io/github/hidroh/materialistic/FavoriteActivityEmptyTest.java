package io.github.hidroh.materialistic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

import static org.assertj.android.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class FavoriteActivityEmptyTest {
    private ActivityController<FavoriteActivity> controller;
    private FavoriteActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(FavoriteActivity.class);
        activity = controller.create().start().resume().get(); // skip menu due to search view
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

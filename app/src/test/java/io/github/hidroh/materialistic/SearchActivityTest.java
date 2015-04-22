package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.ShadowSearchRecentSuggestions;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.assertj.core.api.Assertions.assertThat;

@Config(shadows = ShadowSearchRecentSuggestions.class)
@RunWith(RobolectricTestRunner.class)
public class SearchActivityTest {
    private ActivityController<SearchActivity> controller;
    private SearchActivity activity;

    @Before
    public void setUp() {
        ShadowSearchRecentSuggestions.recentQueries.clear();
        controller = Robolectric.buildActivity(SearchActivity.class);
        activity = controller.get();
    }

    @Test
    public void testCreateWithQuery() {
        Intent intent = new Intent();
        intent.putExtra(SearchManager.QUERY, "filter");
        controller.withIntent(intent).create().start().resume(); // skip menu inflation
        assertThat(ShadowSearchRecentSuggestions.recentQueries).contains("filter");
        assertEquals(activity.getString(R.string.title_activity_search),
                activity.getDefaultTitle());
        assertEquals("filter", activity.getSupportActionBar().getSubtitle());
        assertFalse(activity.isItemOptionsMenuVisible());
        controller.pause().stop().destroy();
    }


    @Test
    public void testCreateWithoutQuery() {
        controller.create().start().resume(); // skip menu inflation
        assertThat(ShadowSearchRecentSuggestions.recentQueries).isEmpty();
        assertEquals(activity.getString(R.string.title_activity_search),
                activity.getDefaultTitle());
        assertNull(activity.getSupportActionBar().getSubtitle());
        assertFalse(activity.isItemOptionsMenuVisible());
        controller.pause().stop().destroy();
    }
}

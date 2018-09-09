package io.github.hidroh.materialistic;

import android.os.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.data.ItemManager;

import static junit.framework.Assert.assertEquals;
import static org.assertj.android.appcompat.v7.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class PopularActivityTest {
    private final int menuResId;
    private final String expectedRange;
    private final int expectedSubtitleResId;
    private ActivityController<PopularActivity> controller;
    private PopularActivity activity;
    @Inject @Named(ActivityModule.POPULAR) ItemManager itemManager;

    public PopularActivityTest(int menuResId, String expectedRange, int expectedSubtitleResId) {
        this.menuResId = menuResId;
        this.expectedRange = expectedRange;
        this.expectedSubtitleResId = expectedSubtitleResId;
    }

    @ParameterizedRobolectricTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{R.id.menu_range_day, AlgoliaPopularClient.LAST_24H, R.string.popular_range_last_24h},
                new Object[]{R.id.menu_range_week, AlgoliaPopularClient.PAST_WEEK, R.string.popular_range_past_week},
                new Object[]{R.id.menu_range_month, AlgoliaPopularClient.PAST_MONTH, R.string.popular_range_past_month},
                new Object[]{R.id.menu_range_year, AlgoliaPopularClient.PAST_YEAR, R.string.popular_range_past_year}
        );
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
        controller = Robolectric.buildActivity(PopularActivity.class);
        activity = controller.create().start().resume().visible().get();
    }

    @Test
    public void testFilter() {
        shadowOf(activity).clickMenuItem(menuResId);
        verify(itemManager, atLeastOnce()).getStories(eq(expectedRange), eq(ItemManager.MODE_DEFAULT));
        assertThat(activity.getSupportActionBar()).hasSubtitle(expectedSubtitleResId);
        assertEquals(expectedRange, Preferences.getPopularRange(activity));
        Bundle savedState = new Bundle();
        activity.onSaveInstanceState(savedState);
        controller = Robolectric.buildActivity(PopularActivity.class);
        activity = controller.create(savedState).start().resume().visible().get();
        assertThat(activity.getSupportActionBar()).hasSubtitle(expectedSubtitleResId);
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

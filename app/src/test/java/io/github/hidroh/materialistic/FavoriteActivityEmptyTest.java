package io.github.hidroh.materialistic;

import android.app.SearchManager;
import android.content.Intent;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.LocalItemManager;
import io.github.hidroh.materialistic.test.TestRunner;

import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunner.class)
public class FavoriteActivityEmptyTest {
    private ActivityController<FavoriteActivity> controller;
    private FavoriteActivity activity;
    @Inject FavoriteManager favoriteManager;
    @Captor ArgumentCaptor<LocalItemManager.Observer> observerCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        when(favoriteManager.getSize()).thenReturn(0);
        controller = Robolectric.buildActivity(FavoriteActivity.class);
        activity = controller.create().start().resume().visible().get();
        verify(favoriteManager, atLeastOnce()).attach(observerCaptor.capture(), any());
        observerCaptor.getValue().onChanged();
    }

    @Test
    public void testEmpty() {
        assertThat((View) activity.findViewById(R.id.empty)).isVisible();
        activity.findViewById(R.id.header_card_view).performLongClick();
        assertThat((View) activity.findViewById(R.id.header_card_view)
                .findViewById(R.id.bookmarked)).isVisible();
        activity.findViewById(R.id.header_card_view).performLongClick();
        assertThat((View) activity.findViewById(R.id.header_card_view)
                .findViewById(R.id.bookmarked)).isNotVisible();
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_clear));
        assertThat(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_search)).isNotVisible();
    }

    @Test
    public void testDataChanged() {
        assertThat((View) activity.findViewById(R.id.empty_search)).isNotVisible();
        reset(favoriteManager);
        controller.newIntent(new Intent().putExtra(SearchManager.QUERY, "query"));
        verify(favoriteManager, atLeastOnce()).attach(observerCaptor.capture(), any());
        observerCaptor.getValue().onChanged();
        assertThat((View) activity.findViewById(R.id.empty_search)).isVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

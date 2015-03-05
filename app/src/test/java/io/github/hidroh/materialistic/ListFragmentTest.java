package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;

import org.assertj.android.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import java.util.ArrayList;
import java.util.List;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.ShadowSwipeRefreshLayout;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestItemManager;

import static junit.framework.Assert.assertNotNull;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf_;


@Config(emulateSdk = 18, reportSdk = 18, shadows = {ShadowSwipeRefreshLayout.class})
@RunWith(RobolectricTestRunner.class)
public class ListFragmentTest {
    private ActivityController<ListActivity> controller;
    private ListActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(ListActivity.class)
                        .create().start().resume().visible();
        activity = controller.get();
    }

    @Test
    public void testOnCreate() {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, ListFragment.instantiate(activity, new TestItemManager(), ItemManager.FetchMode.top.name()))
                .commit();
        assertThat((SwipeRefreshLayout) activity.findViewById(R.id.swipe_layout)).isRefreshing();
    }

    @Test
    public void testRefresh() {
        ItemManager manager = mock(ItemManager.class);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, ListFragment.instantiate(activity, manager, ItemManager.FetchMode.top.name()))
                .commit();
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = shadowOf_(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        // should trigger another data request
        verify(manager, times(2)).getStories(any(String.class),
                any(ItemManager.ResponseListener.class));
    }

    @Test
    public void testConfigurationChanged() {
        ItemManager manager = mock(ItemManager.class);
        ListFragment fragment = ListFragment.instantiate(activity, manager, ItemManager.FetchMode.top.name());
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commit();
        Bundle state = new Bundle();
        fragment.onSaveInstanceState(state);
        fragment.onActivityCreated(state);
        // should not trigger another data request
        verify(manager).getStories(any(String.class),
                any(ItemManager.ResponseListener.class));
    }

    @Test
    public void testEmptyResponse() {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, ListFragment.instantiate(activity, new TestItemManager() {
                    @Override
                    public void getStories(String filter, ResponseListener<Item[]> listener) {
                        listener.onResponse(new Item[0]);
                    }
                }, ItemManager.FetchMode.top.name()))
                .commit();
        assertThat((SwipeRefreshLayout) activity.findViewById(R.id.swipe_layout)).isNotRefreshing();
        Assertions.assertThat(activity.findViewById(android.R.id.empty)).isNotVisible();
    }

    @Test
    public void testErrorResponse() {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, ListFragment.instantiate(activity, new TestItemManager() {
                    @Override
                    public void getStories(String filter, ResponseListener<Item[]> listener) {
                        listener.onError(null);
                    }
                }, ItemManager.FetchMode.top.name()))
                .commit();
        assertThat((SwipeRefreshLayout) activity.findViewById(R.id.swipe_layout)).isNotRefreshing();
        Assertions.assertThat(activity.findViewById(android.R.id.empty)).isVisible();
    }

    @Test
    public void testRefreshError() {
        final List<Boolean> response = new ArrayList<>();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, ListFragment.instantiate(activity, new TestItemManager() {
                    @Override
                    public void getStories(String filter, ResponseListener<Item[]> listener) {
                        if (response.size() == 0) {
                            response.add(true);
                            listener.onResponse(new Item[]{new TestItem() {}});
                        } else {
                            response.add(false);
                            listener.onError(null);
                        }
                    }
                }, ItemManager.FetchMode.top.name()))
                .commit();
        Assertions.assertThat(activity.findViewById(android.R.id.empty)).isNotVisible();
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = shadowOf_(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        Assertions.assertThat(activity.findViewById(android.R.id.empty)).isNotVisible();
        assertNotNull(ShadowToast.getLatestToast());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

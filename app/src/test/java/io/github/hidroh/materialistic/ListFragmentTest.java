package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import org.assertj.android.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.ShadowSwipeRefreshLayout;
import io.github.hidroh.materialistic.test.TestItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Config(shadows = {ShadowSwipeRefreshLayout.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ListFragmentTest {
    private ActivityController<ListActivity> controller;
    private ListActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Captor ArgumentCaptor<ItemManager.ResponseListener<ItemManager.Item[]>> listener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
        controller = Robolectric.buildActivity(ListActivity.class)
                        .create().start().resume().visible();
        activity = controller.get();
    }

    @Test
    public void testOnCreate() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        assertThat((SwipeRefreshLayout) activity.findViewById(R.id.swipe_layout)).isRefreshing();
    }

    @Test
    public void testRefresh() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = (ShadowSwipeRefreshLayout)
                ShadowExtractor.extract(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        // should trigger another data request
        verify(itemManager, times(2)).getStories(any(String.class),
                any(ItemManager.ResponseListener.class));
    }

    @Test
    public void testHighlightNewItems() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        verify(itemManager).getStories(anyString(), listener.capture());
        listener.getValue().onResponse(new ItemManager.Item[]{new TestItem() {
            @Override
            public String getId() {
                return "1";
            }
        }});
        reset(itemManager);
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = (ShadowSwipeRefreshLayout)
                ShadowExtractor.extract(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        // should trigger another data request
        verify(itemManager).getStories(any(String.class), listener.capture());
        listener.getValue().onResponse(new ItemManager.Item[]{
                new TestItem() {
                    @Override
                    public String getId() {
                        return "1";
                    }
                },
                new TestItem() {
                    @Override
                    public String getId() {
                        return "2";
                    }
                }
        });
        assertEquals(2, ((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter().getItemCount());
        Assertions.assertThat((TextView) activity.findViewById(R.id.snackbar_text))
                .isNotNull()
                .containsText(activity.getString(R.string.new_stories_count, 1));
        activity.findViewById(R.id.snackbar_action).performClick();
        assertEquals(1, ((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter().getItemCount());
        Assertions.assertThat((TextView) activity.findViewById(R.id.snackbar_text))
                .isNotNull()
                .containsText(activity.getString(R.string.showing_new_stories, 1));
        activity.findViewById(R.id.snackbar_action).performClick();
        assertEquals(2, ((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter().getItemCount());
    }

    @Test
    public void testConfigurationChanged() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        Fragment fragment = Fragment.instantiate(activity, ListFragment.class.getName(), args);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commit();
        verify(itemManager).getStories(anyString(), listener.capture());
        listener.getValue().onResponse(new ItemManager.Item[]{new TestItem() {
        }});
        reset(itemManager);
        Bundle state = new Bundle();
        fragment.onSaveInstanceState(state);
        fragment.onActivityCreated(state);
        // should not trigger another data request
        verify(itemManager, never()).getStories(any(String.class),
                any(ItemManager.ResponseListener.class));
    }

    @Test
    public void testEmptyResponse() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        verify(itemManager).getStories(anyString(), listener.capture());
        listener.getValue().onResponse(new ItemManager.Item[0]);
        assertThat((SwipeRefreshLayout) activity.findViewById(R.id.swipe_layout)).isNotRefreshing();
        Assertions.assertThat(activity.findViewById(android.R.id.empty)).isNotVisible();
    }

    @Test
    public void testErrorResponse() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        verify(itemManager).getStories(anyString(), listener.capture());
        listener.getValue().onError(null);
        assertThat((SwipeRefreshLayout) activity.findViewById(R.id.swipe_layout)).isNotRefreshing();
        Assertions.assertThat(activity.findViewById(android.R.id.empty)).isVisible();
    }

    @Test
    public void testRefreshError() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        verify(itemManager).getStories(anyString(), listener.capture());
        listener.getValue().onResponse(new ItemManager.Item[]{new TestItem() {}});
        Assertions.assertThat(activity.findViewById(android.R.id.empty)).isNotVisible();
        reset(itemManager);
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = (ShadowSwipeRefreshLayout)
                ShadowExtractor.extract(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        verify(itemManager).getStories(anyString(), listener.capture());
        listener.getValue().onError(null);
        Assertions.assertThat(activity.findViewById(android.R.id.empty)).isNotVisible();
        assertNotNull(ShadowToast.getLatestToast());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

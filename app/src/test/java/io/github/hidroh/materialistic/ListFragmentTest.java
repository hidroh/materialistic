package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.assertj.android.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadow.api.Shadow;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowPreferenceFragmentCompat;
import io.github.hidroh.materialistic.test.shadow.ShadowSnackbar;
import io.github.hidroh.materialistic.test.shadow.ShadowSwipeRefreshLayout;
import io.github.hidroh.materialistic.widget.ListRecyclerViewAdapter;

import static junit.framework.Assert.assertEquals;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowSwipeRefreshLayout.class, ShadowSnackbar.class, ShadowPreferenceFragmentCompat.class})
@RunWith(TestRunner.class)
public class ListFragmentTest {
    private ActivityController<ListActivity> controller;
    private ListActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
        controller = Robolectric.buildActivity(ListActivity.class)
                        .create().postCreate(null).start().resume().visible();
        activity = controller.get();
    }

    @Test
    public void testOnCreate() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.list,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        assertThat((SwipeRefreshLayout) activity.findViewById(R.id.swipe_layout)).isRefreshing();
        controller.pause().stop().destroy();
    }

    @Test
    public void testRefresh() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.list,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = Shadow.extract(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        // should trigger another data request
        verify(itemManager).getStories(any(String.class), eq(ItemManager.MODE_DEFAULT));
        verify(itemManager).getStories(any(String.class), eq(ItemManager.MODE_NETWORK));
        controller.pause().stop().destroy();
    }

    @Test
    public void testHighlightNewItems() {
        when(itemManager.getStories(any(), eq(ItemManager.MODE_DEFAULT))).thenReturn(new Item[]{new TestItem() {
            @Override
            public String getId() {
                return "1";
            }
        }});
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.list,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        verify(itemManager).getStories(any(), eq(ItemManager.MODE_DEFAULT));
        reset(itemManager);
        when(itemManager.getStories(any(), eq(ItemManager.MODE_NETWORK))).thenReturn(new Item[]{
                new TestHnItem(1L),
                new TestHnItem(2L)
        });
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = Shadow.extract(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        // should trigger another data request
        verify(itemManager).getStories(any(String.class), eq(ItemManager.MODE_NETWORK));
        assertEquals(2, ((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter().getItemCount());
        View snackbarView = ShadowSnackbar.getLatestView();
        Assertions.assertThat((TextView) snackbarView.findViewById(R.id.snackbar_text))
                .isNotNull()
                .containsText(activity.getResources().getQuantityString(R.plurals.new_stories_count, 1, 1));
        snackbarView.findViewById(R.id.snackbar_action).performClick();
        assertEquals(1, ((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter().getItemCount());
        snackbarView = ShadowSnackbar.getLatestView();
        Assertions.assertThat((TextView) snackbarView.findViewById(R.id.snackbar_text))
                .isNotNull()
                .containsText(activity.getResources().getQuantityString(R.plurals.showing_new_stories, 1, 1));
        snackbarView.findViewById(R.id.snackbar_action).performClick();
        assertEquals(2, ((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter().getItemCount());
        controller.pause().stop().destroy();
    }

    @Test
    public void testConfigurationChanged() {
        when(itemManager.getStories(any(), eq(ItemManager.MODE_DEFAULT))).thenReturn(new Item[]{new TestItem() {
        }});
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        Fragment fragment = Fragment.instantiate(activity, ListFragment.class.getName(), args);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.list, fragment)
                .commit();
        verify(itemManager).getStories(any(), eq(ItemManager.MODE_DEFAULT));
        reset(itemManager);
        Bundle state = new Bundle();
        fragment.onSaveInstanceState(state);
        fragment.onActivityCreated(state);
        // should not trigger another data request
        verify(itemManager, never()).getStories(any(String.class), eq(ItemManager.MODE_DEFAULT));
        controller.pause().stop().destroy();
    }

    @Test
    public void testEmptyResponse() {
        when(itemManager.getStories(any(), eq(ItemManager.MODE_DEFAULT))).thenReturn(new Item[0]);
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.list,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        verify(itemManager).getStories(any(), eq(ItemManager.MODE_DEFAULT));
        assertThat((SwipeRefreshLayout) activity.findViewById(R.id.swipe_layout)).isNotRefreshing();
        Assertions.assertThat((View) activity.findViewById(R.id.empty)).isNotVisible();
        controller.pause().stop().destroy();
    }

    @Test
    public void testRefreshError() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.list,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        verify(itemManager).getStories(any(), eq(ItemManager.MODE_DEFAULT));
        Assertions.assertThat((View) activity.findViewById(R.id.empty)).isNotVisible();
        reset(itemManager);
        when(itemManager.getStories(any(), eq(ItemManager.MODE_DEFAULT))).thenReturn(new Item[]{});
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = (ShadowSwipeRefreshLayout)
                Shadow.extract(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        verify(itemManager).getStories(any(), eq(ItemManager.MODE_NETWORK));
        Assertions.assertThat((View) activity.findViewById(R.id.empty)).isNotVisible();
        controller.pause().stop().destroy();
    }

    @Test
    public void testLayoutToggle() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.list,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        assertCompactView();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_list_item_view), true)
                .apply();
        assertCardView();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_list_item_view), false)
                .apply();
        assertCompactView();
        controller.pause().stop().destroy();
    }

    @Test
    public void testTogglePreferenceChange() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.list,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args),
                        ListFragment.class.getName())
                .commit();
        assertCompactView();
        controller.pause();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_list_item_view), true)
                .apply();
        controller.resume().postResume();
        activity.getSupportFragmentManager().findFragmentByTag(ListFragment.class.getName())
                .onPrepareOptionsMenu(shadowOf(activity).getOptionsMenu());
        assertCardView();
        controller.pause().stop().destroy();
    }

    @Test
    public void testListMenu() {
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.list,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args),
                        ListFragment.class.getName())
                .commit();
        activity.getSupportFragmentManager().findFragmentByTag(ListFragment.class.getName())
                .onOptionsItemSelected(new RoboMenuItem(R.id.menu_list));
        assertThat(activity.getSupportFragmentManager())
                .hasFragmentWithTag(PopupSettingsFragment.class.getName());
    }

    private void assertCardView() {
        assertThat(((ListRecyclerViewAdapter)
                ((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter())
                .isCardViewEnabled())
                .isTrue();
    }

    private void assertCompactView() {
        assertThat(((ListRecyclerViewAdapter)
                ((RecyclerView) activity.findViewById(R.id.recycler_view)).getAdapter())
                .isCardViewEnabled())
                .isFalse();
    }
}

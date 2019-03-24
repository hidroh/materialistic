package io.github.hidroh.materialistic;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import androidx.fragment.app.Fragment;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.android.controller.ActivityController;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.LocalItemManager;
import io.github.hidroh.materialistic.data.MaterialisticDatabase;
import io.github.hidroh.materialistic.data.SyncScheduler;
import io.github.hidroh.materialistic.data.TestFavorite;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.TestFavoriteActivity;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowItemTouchHelper;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerViewAdapter;

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowRecyclerViewAdapter.class})
@RunWith(TestRunner.class)
public class FavoriteActivityTest {
    private ActivityController<TestFavoriteActivity> controller;
    private TestFavoriteActivity activity;
    private RecyclerView.Adapter adapter;
    private RecyclerView recyclerView;
    private Fragment fragment;
    @Inject FavoriteManager favoriteManager;
    @Inject ActionViewResolver actionViewResolver;
    @Inject UserServices userServices;
    @Inject KeyDelegate keyDelegate;
    @Inject SyncScheduler syncScheduler;
    @Captor ArgumentCaptor<Set<String>> selection;
    @Captor ArgumentCaptor<View.OnClickListener> searchViewClickListener;
    @Captor ArgumentCaptor<SearchView.OnCloseListener> searchViewCloseListener;
    @Captor ArgumentCaptor<UserServices.Callback> userServicesCallback;
    @Captor ArgumentCaptor<LocalItemManager.Observer> observerCaptor;
    private ShadowRecyclerViewAdapter shadowAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(favoriteManager);
        reset(userServices);
        reset(keyDelegate);
        reset(actionViewResolver.getActionView(mock(MenuItem.class)));
        controller = Robolectric.buildActivity(TestFavoriteActivity.class);
        when(favoriteManager.getSize()).thenReturn(2);
        when(favoriteManager.getItem(eq(0))).thenReturn(new TestFavorite(
                "1", "http://example.com", "title", System.currentTimeMillis()));
        when(favoriteManager.getItem(eq(1))).thenReturn(new TestFavorite(
                "2", "http://example.com", "ask HN", System.currentTimeMillis()));
        activity = controller.create().postCreate(null).start().resume().visible().get(); // skip menu due to search view
        recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        adapter = recyclerView.getAdapter();
        shadowAdapter = customShadowOf(adapter);
        fragment = activity.getSupportFragmentManager().findFragmentById(android.R.id.list);
        verify(keyDelegate).attach(any(Activity.class));
        verify(favoriteManager).attach(observerCaptor.capture(), any());
    }

    @Test
    public void testNewIntent() {
        when(favoriteManager.getSize()).thenReturn(1);
        controller.newIntent(new Intent().putExtra(SearchManager.QUERY, "title"));
        assertEquals(1, adapter.getItemCount());
        controller.newIntent(new Intent()); // should not clear filter
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void testOptionsMenuClear() {
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_clear).isVisible());
        shadowOf(activity).clickMenuItem(R.id.menu_clear);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        assertEquals(2, adapter.getItemCount());

        shadowOf(activity).clickMenuItem(R.id.menu_clear);
        dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        verify(favoriteManager).clear(any(Context.class), any());
        when(favoriteManager.getSize()).thenReturn(0);
        observerCaptor.getValue().onChanged();
        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void testSearchView() {
        SearchView searchView = (SearchView) actionViewResolver.getActionView(mock(MenuItem.class));
        verify(searchView, atLeastOnce()).setOnSearchClickListener(searchViewClickListener.capture());
        verify(searchView, atLeastOnce()).setOnCloseListener(searchViewCloseListener.capture());
        searchViewClickListener.getAllValues()
                .get(searchViewClickListener.getAllValues().size() - 1)
                .onClick(searchView);
        assertFalse(((FavoriteFragment) fragment).startActionMode(null));
        SearchView.OnCloseListener closeListener = searchViewCloseListener.getAllValues()
                .get(searchViewCloseListener.getAllValues().size() - 1);
        closeListener.onClose();
        assertEquals(2, adapter.getItemCount());
        ((FavoriteFragment) fragment).filter("ask");
        verify(favoriteManager, times(2)).attach(observerCaptor.capture(), any());
        when(favoriteManager.getSize()).thenReturn(1);
        when(favoriteManager.getItem(eq(0))).thenReturn(new TestFavorite(
                "2", "http://example.com", "ask HN", System.currentTimeMillis()));
        observerCaptor.getValue().onChanged();
        assertEquals(1, adapter.getItemCount());
        reset(searchView);
        closeListener.onClose();
        verify(searchView).setQuery(eq(FavoriteActivity.EMPTY_QUERY), eq(true));
    }

    @Test
    public void testItemClick() {
        assertEquals(2, adapter.getItemCount());
        RecyclerView.ViewHolder holder = shadowAdapter.getViewHolder(0);
        holder.itemView.performClick();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testActionMode() {
        RecyclerView.ViewHolder holder = shadowAdapter.getViewHolder(0);
        holder.itemView.performLongClick();
        assertNotNull(activity.actionModeCallback);
    }

    @Test
    public void testSelectionToggle() {
        RecyclerView.ViewHolder holder = shadowAdapter.getViewHolder(0);
        holder.itemView.performLongClick();
        holder.itemView.performClick();
        assertNull(shadowOf(activity).getNextStartedActivity());
        holder.itemView.performClick();
        assertNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testDelete() {
        RecyclerView.ViewHolder holder = shadowAdapter.getViewHolder(0);
        holder.itemView.performLongClick();

        ActionMode actionMode = mock(ActionMode.class);
        activity.actionModeCallback.onActionItemClicked(actionMode, new RoboMenuItem(R.id.menu_clear));
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        assertEquals(2, adapter.getItemCount());

        activity.actionModeCallback.onActionItemClicked(actionMode, new RoboMenuItem(R.id.menu_clear));
        dialog = ShadowAlertDialog.getLatestAlertDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        verify(favoriteManager).remove(any(Context.class), selection.capture());
        assertThat(selection.getValue()).contains("1");
        verify(actionMode).finish();

        when(favoriteManager.getSize()).thenReturn(1);
        observerCaptor.getValue().onChanged();
        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void testRefresh() {
        RecyclerView.ViewHolder holder = shadowAdapter.getViewHolder(0);
        holder.itemView.performLongClick();

        ActionMode actionMode = mock(ActionMode.class);
        activity.actionModeCallback.onActionItemClicked(actionMode, new RoboMenuItem(R.id.menu_refresh));
        verify(syncScheduler).scheduleSync(any(), any());
        verify(actionMode).finish();
    }

    @Config(shadows = {ShadowRecyclerView.class, ShadowItemTouchHelper.class})
    @Test
    public void testSwipeToDelete() {
        RecyclerView.ViewHolder holder = shadowAdapter.getViewHolder(0);
        customShadowOf(recyclerView).getItemTouchHelperCallback()
                .onSwiped(holder, ItemTouchHelper.LEFT);
        verify(favoriteManager).remove(any(Context.class), anyCollection());
        when(favoriteManager.getSize()).thenReturn(1);
        observerCaptor.getValue().onChanged();
        assertEquals(1, adapter.getItemCount());
        assertThat((TextView) activity.findViewById(R.id.snackbar_text))
                .isNotNull()
                .containsText(R.string.toast_removed);
        activity.findViewById(R.id.snackbar_action).performClick();
        verify(favoriteManager).add(any(Context.class), any(WebItem.class));
        when(favoriteManager.getSize()).thenReturn(2);
        observerCaptor.getValue().onChanged();
        assertEquals(2, adapter.getItemCount());
    }

    @Config(shadows = {ShadowRecyclerView.class, ShadowItemTouchHelper.class})
    @Test
    public void testSwipeToRefresh() {
        RecyclerView.ViewHolder holder = shadowAdapter.getViewHolder(0);
        customShadowOf(recyclerView).getItemTouchHelperCallback()
                .onSwiped(holder, ItemTouchHelper.RIGHT);
        verify(syncScheduler).scheduleSync(any(), any());
    }

    @Test
    public void testExport() {
        shadowOf(activity).clickMenuItem(R.id.menu_export);
        verify(favoriteManager).export(any(Context.class), any());
    }

    @Test
    public void testFilter() {
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_search).isVisible());
        Intent intent = new Intent();
        intent.putExtra(SearchManager.QUERY, "blah");
        controller.newIntent(intent);
        verify(favoriteManager).attach(any(LocalItemManager.Observer.class), eq("blah"));
        intent = new Intent();
        intent.putExtra(SearchManager.QUERY, "ask");
        controller.newIntent(intent);
        verify(favoriteManager).attach(any(LocalItemManager.Observer.class), eq("ask"));
    }

    @Test
    public void testSaveState() {
        Bundle outState = new Bundle();
        controller.saveInstanceState(outState);
        ActivityController<TestFavoriteActivity> controller = Robolectric
                .buildActivity(TestFavoriteActivity.class)
                .create(outState)
                .postCreate(outState)
                .start()
                .resume()
                .visible();
        assertEquals(2, ((RecyclerView) controller.get().findViewById(R.id.recycler_view))
                .getAdapter().getItemCount());
        controller.pause().stop().destroy();
        reset(keyDelegate);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItem() {
        shadowAdapter.getViewHolder(0).itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        Assert.assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), any(), userServicesCallback.capture());
        userServicesCallback.getValue().onDone(true);
        assertEquals(activity.getString(R.string.voted), ShadowToast.getTextOfLatestToast());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItemPromptToLogin() {
        shadowAdapter.getViewHolder(0).itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        Assert.assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), any(), userServicesCallback.capture());
        userServicesCallback.getValue().onDone(false);
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, LoginActivity.class);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItemFailed() {
        shadowAdapter.getViewHolder(0).itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        Assert.assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), any(), userServicesCallback.capture());
        userServicesCallback.getValue().onError(new IOException());
        assertEquals(activity.getString(R.string.vote_failed), ShadowToast.getTextOfLatestToast());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testReply() {
        shadowAdapter.getViewHolder(0).itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        Assert.assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_comment));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ComposeActivity.class);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testShare() {
        TestApplication.addResolver(new Intent(Intent.ACTION_SEND));
        shadowAdapter.getViewHolder(0).itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        Assert.assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_share));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasAction(Intent.ACTION_SEND);
    }

    @Test
    public void testRemoveClearSelection() {
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("remove")
                .appendPath("3")
                .build());
        assertNull(activity.getSelectedItem());
        activity.onItemSelected(new TestHnItem(1L));
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("remove")
                .appendPath("2")
                .build());
        assertNotNull(activity.getSelectedItem());
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("remove")
                .appendPath("1")
                .build());
        assertNull(activity.getSelectedItem());
    }

    @Test
    public void testClearSelection() {
        activity.onItemSelected(new TestHnItem(1L));
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                        .buildUpon()
                        .appendPath("clear")
                        .build());
        assertNull(activity.getSelectedItem());
    }

    @Test
    public void testVolumeNavigation() {
        activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(keyDelegate).setScrollable(any(Scrollable.class), any(AppBarLayout.class));
        verify(keyDelegate).onKeyDown(anyInt(), any(KeyEvent.class));
        activity.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP));
        verify(keyDelegate).onKeyUp(anyInt(), any(KeyEvent.class));
        activity.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(keyDelegate).onKeyLongPress(anyInt(), any(KeyEvent.class));
    }

    @After
    public void tearDown() {
        controller.pause().stop();
        verify(keyDelegate).detach(any(Activity.class));
        controller.destroy();
    }
}

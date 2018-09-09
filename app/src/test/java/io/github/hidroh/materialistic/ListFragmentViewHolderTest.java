package io.github.hidroh.materialistic;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.android.controller.ActivityController;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.MaterialisticDatabase;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.TestLayoutManager;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowAnimation;
import io.github.hidroh.materialistic.test.shadow.ShadowItemTouchHelper;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerViewAdapter;
import io.github.hidroh.materialistic.test.shadow.ShadowSnackbar;
import io.github.hidroh.materialistic.test.shadow.ShadowSwipeRefreshLayout;

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowSwipeRefreshLayout.class,
        ShadowRecyclerViewAdapter.class,
        ShadowRecyclerView.class,
        ShadowItemTouchHelper.class,
        ShadowAnimation.class,
        ShadowSnackbar.class})
@RunWith(TestRunner.class)
public class ListFragmentViewHolderTest {
    private ActivityController<ListActivity> controller;
    private ShadowRecyclerViewAdapter adapter;
    private ListActivity activity;
    private TestHnItem item;
    @Inject SessionManager sessionManager;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Inject FavoriteManager favoriteManager;
    @Inject UserServices userServices;
    @Captor ArgumentCaptor<ResponseListener<Item>> itemListener;
    @Captor ArgumentCaptor<UserServices.Callback> voteCallback;
    private RecyclerView recyclerView;
    private ItemTouchHelper.SimpleCallback swipeCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(sessionManager);
        reset(favoriteManager);
        reset(itemManager);
        reset(userServices);
        item = new TestHnItem(1) {
            @Override
            public int getRank() {
                return 46;
            }

            @Override
            public String getBy() {
                return "author";
            }
        };
        when(itemManager.getStories(anyString(), anyInt())).thenReturn(new Item[]{item});
        controller = Robolectric.buildActivity(ListActivity.class)
                .create().start().resume().visible();
        activity = controller.get();
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        verify(itemManager).getStories(any(), eq(ItemManager.MODE_DEFAULT));
        recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        swipeCallback = (ItemTouchHelper.SimpleCallback) customShadowOf(recyclerView).getItemTouchHelperCallback();
        adapter = customShadowOf(recyclerView.getAdapter());
        item.populate(new PopulatedStory(1));
    }

    @Test
    public void testStory() {
        item.setIsViewed(true);
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        assertThat((View) holder.itemView.findViewById(R.id.bookmarked)).isNotVisible();
        assertThat((TextView) holder.itemView.findViewById(R.id.rank)).hasTextString("46");
        assertThat((TextView) holder.itemView.findViewById(R.id.title)).hasTextString("title");
        assertThat((TextView) holder.itemView.findViewById(R.id.comment))
                .isVisible()
                .isEmpty();
        assertViewed();
    }

    @Test
    public void testComment() {
        item.populate(new PopulatedStory(1) {
            @Override
            public int getDescendants() {
                return 1;
            }

            @Override
            public long[] getKids() {
                return new long[]{2};
            }
        });
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        View commentButton = holder.itemView.findViewById(R.id.comment);
        assertThat(commentButton).isVisible();
        reset(activity.multiPaneListener);
        commentButton.performClick();
        verify(activity.multiPaneListener, never()).onItemSelected(any(WebItem.class)
        );
        Intent actual = shadowOf(activity).getNextStartedActivity();
        assertEquals(ItemActivity.class.getName(), actual.getComponent().getClassName());
        assertThat(actual).hasExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true);
    }

    @Test
    public void testJob() {
        item.populate(new PopulatedStory(1) {
            @Override
            public String getRawType() {
                return JOB_TYPE;
            }
        });
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        assertThat((TextView) holder.itemView.findViewById(R.id.source)).isEmpty();
    }

    @Test
    public void testPoll() {
        item.populate(new PopulatedStory(1) {
            @Override
            public String getRawType() {
                return POLL_TYPE;
            }
        });
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        assertThat((TextView) holder.itemView.findViewById(R.id.source)).isEmpty();
    }

    @Test
    public void testItemClick() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        adapter.getViewHolder(0).itemView.performClick();
        verify(activity.multiPaneListener).onItemSelected(any(WebItem.class));
    }

    @Test
    public void testViewedObserver() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        assertNotViewed();
        controller.pause();
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseReadUri()
                .buildUpon().appendPath("2").build()); // not in view
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseReadUri()
                    .buildUpon().appendPath("1").build()); // in view
        controller.resume();
        assertViewed();
    }

    @Test
    public void testFavoriteObserver() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        item.setFavorite(true);
        itemListener.getValue().onResponse(item);
        assertTrue(item.isFavorite());

        controller.pause();

        // observed clear
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("clear")
                .build());
        RecyclerView.ViewHolder viewHolder = adapter.getViewHolder(0);
        assertFalse(item.isFavorite());
        assertThat((View) viewHolder.itemView.findViewById(R.id.bookmarked)).isNotVisible();
        // observed add
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("add")
                .appendPath("1")
                .build());
        assertTrue(item.isFavorite());
        // observed remove
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("remove")
                .appendPath("1")
                .build());
        assertFalse(item.isFavorite());

        controller.resume();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testSaveItem() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        adapter.getViewHolder(0).itemView.performLongClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        assertThat(popupMenu.getMenu().findItem(R.id.menu_contextual_save).isVisible()).isFalse();
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_save));
        verify(favoriteManager).add(any(Context.class), eq(item));
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("add")
                .appendPath("1")
                .build());
        assertTrue(item.isFavorite());
        View snackbarView = ShadowSnackbar.getLatestView();
        assertThat((TextView) snackbarView.findViewById(R.id.snackbar_text))
                .isNotNull()
                .containsText(R.string.toast_saved);
        snackbarView.findViewById(R.id.snackbar_action).performClick();
        verify(favoriteManager).remove(any(Context.class), eq("1"));
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("remove")
                .appendPath("1")
                .build());
        assertFalse(item.isFavorite());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testSwipeToSaveItem() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        assertThat(swipeCallback.onMove(recyclerView, holder, holder)).isFalse();
        assertThat(swipeCallback.getSwipeThreshold(holder)).isGreaterThan(0f);
        assertThat(swipeCallback.getSwipeDirs(recyclerView, holder))
                .isEqualTo(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

        Canvas canvas = mock(Canvas.class);
        swipeCallback.onChildDraw(canvas, recyclerView, holder, -1f, 0f,
                ItemTouchHelper.ACTION_STATE_SWIPE, true);
        verify(canvas).drawText(eq(activity.getString(R.string.save).toUpperCase()),
                anyFloat(), anyFloat(), any(Paint.class));

        swipeCallback.onSwiped(holder, ItemTouchHelper.LEFT);
        verify(favoriteManager).add(any(Context.class), eq(item));

        item.setFavorite(true);
        assertThat(swipeCallback.getSwipeDirs(recyclerView, holder))
                .isEqualTo(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    @Test
    public void testDisableSwipe() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_list_swipe_left),
                        Preferences.SwipeAction.None.name())
                .putString(activity.getString(R.string.pref_list_swipe_right),
                        Preferences.SwipeAction.None.name())
                .apply();
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        assertThat(swipeCallback.getSwipeDirs(recyclerView, holder)).isEqualTo(0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testViewUser() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        adapter.getViewHolder(0).itemView.performLongClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_profile));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, UserActivity.class)
                .hasExtra(UserActivity.EXTRA_USERNAME, "author");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItem() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        adapter.getViewHolder(0).itemView.performLongClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        assertThat(popupMenu.getMenu().findItem(R.id.menu_contextual_vote).isVisible()).isFalse();
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), eq(item.getId()), voteCallback.capture());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testSwipeToVoteItem() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        assertThat(swipeCallback.getSwipeDirs(recyclerView, holder))
                .isEqualTo(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

        Canvas canvas = mock(Canvas.class);
        swipeCallback.onChildDraw(canvas, recyclerView, holder, 1f, 0f,
                ItemTouchHelper.ACTION_STATE_SWIPE, true);
        verify(canvas).drawText(eq(activity.getString(R.string.vote_up).toUpperCase()),
                anyFloat(), anyFloat(), any(Paint.class));

        swipeCallback.onSwiped(holder, ItemTouchHelper.RIGHT);
        verify(userServices).voteUp(any(Context.class), eq(item.getId()), voteCallback.capture());

        item.incrementScore();
        assertThat(swipeCallback.getSwipeDirs(recyclerView, holder))
                .isEqualTo(ItemTouchHelper.LEFT);

        item.clearPendingVoted();
        assertThat(swipeCallback.getSwipeDirs(recyclerView, holder))
                .isEqualTo(ItemTouchHelper.LEFT);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItemPromptToLogin() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        adapter.getViewHolder(0).itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), eq(item.getId()), voteCallback.capture());
        voteCallback.getValue().onDone(false);
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, LoginActivity.class);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItemFailed() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        adapter.getViewHolder(0).itemView.performLongClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), eq(item.getId()), voteCallback.capture());
        voteCallback.getValue().onError(new IOException());
        assertEquals(activity.getString(R.string.vote_failed), ShadowToast.getTextOfLatestToast());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testReply() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        adapter.getViewHolder(0).itemView.performLongClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_comment));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ComposeActivity.class)
                .hasExtra(ComposeActivity.EXTRA_PARENT_ID, "1");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testRefresh() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        reset(itemManager);
        adapter.getViewHolder(0).itemView.performLongClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_refresh));
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), any());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testSwipeToRefresh() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        reset(itemManager);
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_list_swipe_left),
                        Preferences.SwipeAction.None.name())
                .putString(activity.getString(R.string.pref_list_swipe_right),
                        Preferences.SwipeAction.Refresh.name())
                .apply();
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        assertThat(swipeCallback.getSwipeDirs(recyclerView, holder))
                .isEqualTo(ItemTouchHelper.RIGHT);

        Canvas canvas = mock(Canvas.class);
        swipeCallback.onChildDraw(canvas, recyclerView, holder, 1f, 0f,
                ItemTouchHelper.ACTION_STATE_SWIPE, true);
        verify(canvas).drawText(eq(activity.getString(R.string.refresh).toUpperCase()),
                anyFloat(), anyFloat(), any(Paint.class));

        swipeCallback.onSwiped(holder, ItemTouchHelper.RIGHT);
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), any());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testShare() {
        TestApplication.addResolver(new Intent(Intent.ACTION_SEND));
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        adapter.getViewHolder(0).itemView.performLongClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_share));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasAction(Intent.ACTION_SEND);
    }

    @Test
    public void testAutoMarkAsViewed() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_auto_viewed), true)
                .apply();

        ShadowRecyclerView shadowRecyclerView = customShadowOf(recyclerView);
        TestLayoutManager testLayout = new TestLayoutManager(activity);
        recyclerView.setLayoutManager(testLayout);
        testLayout.firstVisiblePosition = 0;
        shadowRecyclerView.getScrollListener().onScrolled(recyclerView, 0, 1);
        verify(sessionManager, never()).view(any());

        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), itemListener.capture());
        itemListener.getValue().onResponse(item);
        testLayout.firstVisiblePosition = 0;
        shadowRecyclerView.getScrollListener().onScrolled(recyclerView, 0, 1);
        verify(sessionManager, never()).view(any());

        testLayout.firstVisiblePosition = 1;
        shadowRecyclerView.getScrollListener().onScrolled(recyclerView, 0, 1);
        verify(sessionManager).view(any());

        item.setIsViewed(true);
        testLayout.firstVisiblePosition = 1;
        shadowRecyclerView.getScrollListener().onScrolled(recyclerView, 0, 1);
        verify(sessionManager).view(any()); // should not trigger again

        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_auto_viewed), false)
                .apply();
        assertNull(shadowRecyclerView.getScrollListener());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void assertViewed() {
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasCurrentTextColor(ContextCompat.getColor(activity, AppUtils.getThemedResId(activity, android.R.attr.textColorSecondary)));
    }

    private void assertNotViewed() {
        RecyclerView.ViewHolder holder = adapter.getViewHolder(0);
        assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasCurrentTextColor(ContextCompat.getColor(activity, R.color.blackT87));
    }

    @SuppressLint("ParcelCreator")
    private static class PopulatedStory extends TestHnItem {
        public PopulatedStory(long id) {
            super(id);
        }

        @Override
        public String getTitle() {
            return "title";
        }

        @Override
        public String getRawType() {
            return STORY_TYPE;
        }

        @Override
        public long[] getKids() {
            return new long[0];
        }

        @Override
        public int getDescendants() {
            return 0;
        }
    }

}

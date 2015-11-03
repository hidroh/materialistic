package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

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
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;
import org.robolectric.util.ActivityController;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.ShadowSwipeRefreshLayout;
import io.github.hidroh.materialistic.test.TestItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.support.v4.Shadows.shadowOf;

@Config(shadows = {ShadowSwipeRefreshLayout.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ListFragmentViewHolderTest {
    private ActivityController<ListActivity> controller;
    private RecyclerView.Adapter adapter;
    private RecyclerView.ViewHolder holder;
    private ListActivity activity;
    private TestStory item;
    @Inject SessionManager sessionManager;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Inject FavoriteManager favoriteManager;
    @Captor ArgumentCaptor<SessionManager.OperationCallbacks> sessionCallbacks;
    @Captor ArgumentCaptor<FavoriteManager.OperationCallbacks> favoriteCallbacks;
    @Captor ArgumentCaptor<ItemManager.ResponseListener<ItemManager.Item[]>> storiesListener;
    @Captor ArgumentCaptor<ItemManager.ResponseListener<ItemManager.Item>> itemListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(sessionManager);
        reset(favoriteManager);
        reset(itemManager);
        item = new TestStory();
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
        item.title = "title";
        verify(itemManager).getStories(anyString(), storiesListener.capture());
        storiesListener.getValue().onResponse(new ItemManager.Item[]{item});
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        adapter = recyclerView.getAdapter();
        holder = adapter.createViewHolder(recyclerView, 0);
    }

    @Test
    public void testStory() {
        adapter.bindViewHolder(holder, 0);
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(item);
        assertThat(holder.itemView.findViewById(R.id.bookmarked)).isNotVisible();
        assertNotViewed();
        assertThat((TextView) holder.itemView.findViewById(R.id.rank)).hasTextString("46");
        assertThat((TextView) holder.itemView.findViewById(R.id.title)).hasText("title");
        assertThat(holder.itemView.findViewById(R.id.comment)).isNotVisible();
        verify(sessionManager).isViewed(any(Context.class), anyString(), sessionCallbacks.capture());
        sessionCallbacks.getValue().onCheckComplete(true);
        assertViewed();
    }

    @Test
    public void testNewStory() {
        TestStory newItem = new TestStory() {
            @Override
            public String getId() {
                return "2";
            }
        };
        reset(itemManager);
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = (ShadowSwipeRefreshLayout)
                ShadowExtractor.extract(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        verify(itemManager).getStories(anyString(), storiesListener.capture());
        storiesListener.getValue().onResponse(new ItemManager.Item[]{newItem});
        activity.findViewById(R.id.snackbar_action).performClick();
        adapter.bindViewHolder(holder, 0);
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(newItem);
        assertThat((TextView) holder.itemView.findViewById(R.id.rank)).hasTextString("46*");
    }

    @Test
    public void testTrending() {
        TestStory newItem = new TestStory() {
            @Override
            public int getRank() {
                return 45;
            }
        };
        reset(itemManager);
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = (ShadowSwipeRefreshLayout)
                ShadowExtractor.extract(activity.findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        verify(itemManager).getStories(anyString(), storiesListener.capture());
        storiesListener.getValue().onResponse(new ItemManager.Item[]{newItem});
        adapter.bindViewHolder(holder, 0);
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(newItem);
        assertThat((TextView) holder.itemView.findViewById(R.id.rank))
                .hasCurrentTextColor(ContextCompat.getColor(activity, R.color.rank_up));
    }

    @Test
    public void testComment() {
        adapter.bindViewHolder(holder, 0);
        item.kidCount = 1;
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(item);
        View commentButton = holder.itemView.findViewById(R.id.comment);
        assertThat(commentButton).isVisible();
        commentButton.performClick();
        verify(activity.multiPaneListener, never()).onItemSelected(any(ItemManager.WebItem.class)
        );
        assertEquals(ItemActivity.class.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
        assertViewed();
    }

    @Test
    public void testJob() {
        adapter.bindViewHolder(holder, 0);
        item.type = ItemManager.Item.JOB_TYPE;
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(item);
        assertEquals(R.drawable.ic_work_grey600_18dp,
                shadowOf(((TextView) holder.itemView.findViewById(R.id.source))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
    }

    @Test
    public void testPoll() {
        adapter.bindViewHolder(holder, 0);
        item.type = ItemManager.Item.POLL_TYPE;
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(item);
        assertEquals(R.drawable.ic_poll_grey600_18dp,
                shadowOf(((TextView) holder.itemView.findViewById(R.id.source))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
    }

    @Test
    public void testItemClick() {
        adapter.bindViewHolder(holder, 0);
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(item);
        holder.itemView.performClick();
        assertViewed();
        verify(activity.multiPaneListener).onItemSelected(any(ItemManager.WebItem.class)
        );
    }

    @Test
    public void testViewedBroadcast() {
        adapter.bindViewHolder(holder, 0);
        item.setIsViewed(false);
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(item);
        assertNotViewed();
        reset(itemManager);

        ShadowLocalBroadcastManager manager = shadowOf(LocalBroadcastManager.getInstance(activity));
        List<ShadowLocalBroadcastManager.Wrapper> receivers = manager.getRegisteredBroadcastReceivers();
        Intent intent = new Intent(SessionManager.ACTION_ADD);
        intent.putExtra(SessionManager.ACTION_ADD_EXTRA_DATA, "1");
        receivers.get(0).broadcastReceiver.onReceive(activity, intent);
        adapter.bindViewHolder(holder, 0);
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(item);
        assertViewed();
    }

    @Test
    public void testFavoriteClearedBroadcast() {
        assertFalse(item.isFavorite());
        ShadowLocalBroadcastManager manager = shadowOf(LocalBroadcastManager.getInstance(activity));
        List<ShadowLocalBroadcastManager.Wrapper> receivers = manager.getRegisteredBroadcastReceivers();
        receivers.get(0).broadcastReceiver.onReceive(activity, new Intent(FavoriteManager.ACTION_CLEAR));
        adapter.bindViewHolder(holder, 0);
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(item);
        verify(favoriteManager).check(any(Context.class), eq("1"), favoriteCallbacks.capture());
        favoriteCallbacks.getValue().onCheckComplete(true);
        assertTrue(item.isFavorite());
    }

    @Test
    public void testItemLongClick() {
        adapter.bindViewHolder(holder, 0);
        verify(itemManager).getItem(anyString(), itemListener.capture());
        itemListener.getValue().onResponse(item);
        holder.itemView.performLongClick();
        verify(favoriteManager).add(any(Context.class), eq(item));
        assertTrue(item.isFavorite());
        assertThat((TextView) activity.findViewById(R.id.snackbar_text))
                .isNotNull()
                .containsText(R.string.toast_saved);
        activity.findViewById(R.id.snackbar_action).performClick();
        verify(favoriteManager).remove(any(Context.class), eq("1"));
        assertFalse(item.isFavorite());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void assertViewed() {
        assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasCurrentTextColor(activity.getResources().getColor(R.color.textColorSecondaryInverse));
    }

    private void assertNotViewed() {
        assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasCurrentTextColor(activity.getResources().getColor(R.color.textColorPrimaryInverse));
    }

    private class TestStory extends TestItem {
        public @Type String type = STORY_TYPE;
        public int kidCount = 0;
        public String title = null;

        @Override
        public String getId() {
            return "1";
        }

        @Override
        public String getDisplayedTitle() {
            return title;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public int getKidCount() {
            return kidCount;
        }

        @Override
        public String getUrl() {
            return "http://hidroh.github.io";
        }

        @Override
        public int getRank() {
            return 46;
        }
    }
}

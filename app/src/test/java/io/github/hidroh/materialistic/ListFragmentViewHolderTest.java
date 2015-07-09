package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
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
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;
import org.robolectric.util.ActivityController;

import java.util.List;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestItemManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.support.v4.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
public class ListFragmentViewHolderTest {
    private ActivityController<ListActivity> controller;
    private RecyclerView.Adapter adapter;
    private RecyclerView.ViewHolder holder;
    private ListActivity activity;
    private TestStory item;
    @Inject SessionManager sessionManager;
    @Inject FavoriteManager favoriteManager;
    @Captor ArgumentCaptor<SessionManager.OperationCallbacks> sessionCallbacks;
    @Captor ArgumentCaptor<FavoriteManager.OperationCallbacks> favoriteCallbacks;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(sessionManager);
        reset(favoriteManager);
        item = new TestStory();
        controller = Robolectric.buildActivity(ListActivity.class)
                .create().start().resume().visible();
        activity = controller.get();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, ListFragment.instantiate(activity, new TestItemManager() {
                    @Override
                    public void getStories(String filter, ResponseListener<Item[]> listener) {
                        listener.onResponse(new Item[]{item});
                    }

                    @Override
                    public void getItem(String itemId, ResponseListener<Item> listener) {
                        item.title = "title";
                        listener.onResponse(item);
                    }
                }, ItemManager.TOP_FETCH_MODE))
                .commit();
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        adapter = recyclerView.getAdapter();
        holder = adapter.createViewHolder(recyclerView, 0);
    }

    @Test
    public void testStory() {
        adapter.bindViewHolder(holder, 0);
        Assertions.assertThat(holder.itemView.findViewById(R.id.bookmarked)).isNotVisible();
        assertNotViewed();
        Assertions.assertThat((TextView) holder.itemView.findViewById(R.id.title)).hasText("title");
        Assertions.assertThat(holder.itemView.findViewById(R.id.comment)).isNotVisible();
        verify(sessionManager).isViewed(any(Context.class), anyString(), sessionCallbacks.capture());
        sessionCallbacks.getValue().onCheckComplete(true);
        assertViewed();
    }

    @Test
    public void testComment() {
        item.kidCount = 1;
        adapter.bindViewHolder(holder, 0);
        View commentButton = holder.itemView.findViewById(R.id.comment);
        Assertions.assertThat(commentButton).isVisible();
        commentButton.performClick();
        verify(activity.multiPaneListener, never()).onItemSelected(any(ItemManager.WebItem.class),
                any(View.class));
        assertEquals(ItemActivity.class.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
        assertViewed();
    }

    @Test
    public void testJob() {
        item.type = ItemManager.Item.JOB_TYPE;
        item.setIsViewed(true);
        adapter.bindViewHolder(holder, 0);
        assertEquals(R.drawable.ic_work_grey600_18dp,
                shadowOf(((TextView) holder.itemView.findViewById(R.id.source))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
        assertViewed();
    }

    @Test
    public void testPoll() {
        item.type = ItemManager.Item.POLL_TYPE;
        adapter.bindViewHolder(holder, 0);
        assertEquals(R.drawable.ic_poll_grey600_18dp,
                shadowOf(((TextView) holder.itemView.findViewById(R.id.source))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
    }

    @Test
    public void testItemClick() {
        adapter.bindViewHolder(holder, 0);
        holder.itemView.performClick();
        assertViewed();
        verify(activity.multiPaneListener).onItemSelected(any(ItemManager.WebItem.class),
                any(View.class));
    }

    @Test
    public void testViewedBroadcast() {
        item.setIsViewed(false);
        adapter.bindViewHolder(holder, 0);
        assertNotViewed();

        ShadowLocalBroadcastManager manager = shadowOf(LocalBroadcastManager.getInstance(activity));
        List<ShadowLocalBroadcastManager.Wrapper> receivers = manager.getRegisteredBroadcastReceivers();
        Intent intent = new Intent(SessionManager.ACTION_ADD);
        intent.putExtra(SessionManager.ACTION_ADD_EXTRA_DATA, "1");
        receivers.get(0).broadcastReceiver.onReceive(activity, intent);
        adapter.bindViewHolder(holder, 0);
        assertViewed();
    }

    @Test
    public void testFavoriteClearedBroadcast() {
        assertFalse(item.isFavorite());
        ShadowLocalBroadcastManager manager = shadowOf(LocalBroadcastManager.getInstance(activity));
        List<ShadowLocalBroadcastManager.Wrapper> receivers = manager.getRegisteredBroadcastReceivers();
        receivers.get(0).broadcastReceiver.onReceive(activity, new Intent(FavoriteManager.ACTION_CLEAR));
        adapter.bindViewHolder(holder, 0);
        verify(favoriteManager).check(any(Context.class), eq("1"), favoriteCallbacks.capture());
        favoriteCallbacks.getValue().onCheckComplete(true);
        assertTrue(item.isFavorite());
    }

    @Test
    public void testItemLongClick() {
        adapter.bindViewHolder(holder, 0);
        holder.itemView.performLongClick();
        verify(favoriteManager).add(any(Context.class), eq(item));
        assertTrue(item.isFavorite());
        holder.itemView.performLongClick();
        verify(favoriteManager).remove(any(Context.class), eq("1"));
        assertFalse(item.isFavorite());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void assertViewed() {
        Assertions.assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasCurrentTextColor(activity.getResources().getColor(R.color.textColorSecondaryInverse));
    }

    private void assertNotViewed() {
        Assertions.assertThat((TextView) holder.itemView.findViewById(R.id.title))
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
    }
}

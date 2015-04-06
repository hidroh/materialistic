package io.github.hidroh.materialistic;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.assertj.android.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestItemManager;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ListFragmentViewHolderTest {
    private ActivityController<ListActivity> controller;
    private RecyclerView.Adapter adapter;
    private RecyclerView.ViewHolder holder;
    private ListActivity activity;
    private TestStory item;

    @Before
    public void setUp() {
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
                }, ItemManager.FetchMode.top.name()))
                .commit();
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        adapter = recyclerView.getAdapter();
        holder = adapter.createViewHolder(recyclerView, 0);
    }

    @Test
    public void testStory() {
        adapter.bindViewHolder(holder, 0);
        Assertions.assertThat(holder.itemView.findViewById(R.id.bookmarked)).isNotVisible();
        Assertions.assertThat((TextView) holder.itemView.findViewById(R.id.title)).hasText("title");
        Assertions.assertThat(holder.itemView.findViewById(R.id.comment)).isNotVisible();
    }

    @Test
    public void testComment() {
        item.kidCount = 1;
        adapter.bindViewHolder(holder, 0);
        View commentButton = holder.itemView.findViewById(R.id.comment);
        Assertions.assertThat(commentButton).isVisible();
        commentButton.performClick();
        verify(activity.multiPaneListener).onItemSelected(any(ItemManager.WebItem.class),
                any(View.class));
    }

    @Test
    public void testJob() {
        item.type = ItemManager.Item.Type.job;
        adapter.bindViewHolder(holder, 0);
        assertEquals(R.drawable.ic_work_grey600_18dp,
                shadowOf(((TextView) holder.itemView.findViewById(R.id.source))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
    }

    @Test
    public void testPoll() {
        item.type = ItemManager.Item.Type.poll;
        adapter.bindViewHolder(holder, 0);
        assertEquals(R.drawable.ic_poll_grey600_18dp,
                shadowOf(((TextView) holder.itemView.findViewById(R.id.source))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
    }

    @Test
    public void testItemClick() {
        adapter.bindViewHolder(holder, 0);
        holder.itemView.performClick();
        verify(activity.multiPaneListener).onItemSelected(any(ItemManager.WebItem.class),
                any(View.class));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private class TestStory extends TestItem {
        public Type type = Type.story;
        public int kidCount = 0;
        public String title = null;

        @Override
        public String getDisplayedTitle() {
            return title;
        }

        @Override
        public Type getType() {
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

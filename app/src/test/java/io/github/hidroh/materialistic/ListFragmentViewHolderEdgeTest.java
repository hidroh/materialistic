package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import org.robolectric.util.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.TestItem;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
public class ListFragmentViewHolderEdgeTest {
    private ActivityController<ListActivity> controller;
    private RecyclerView.ViewHolder holder;
    private ListActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Captor ArgumentCaptor<ItemManager.ResponseListener<ItemManager.Item>> listener;
    @Captor ArgumentCaptor<ItemManager.ResponseListener<ItemManager.Item[]>> storiesListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
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
        verify(itemManager).getStories(anyString(), storiesListener.capture());
        storiesListener.getValue().onResponse(new ItemManager.Item[]{new TestItem() {
        }});
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        holder = recyclerView.getAdapter().createViewHolder(recyclerView, 0);
        recyclerView.getAdapter().bindViewHolder(holder, 0);
    }

    @Test
    public void testNullResponse() {
        verify(itemManager).getItem(anyString(), listener.capture());
        listener.getValue().onResponse(null);
        Assertions.assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasText(activity.getString(R.string.loading_text));
    }

    @Test
    public void testErrorResponse() {
        verify(itemManager).getItem(anyString(), listener.capture());
        listener.getValue().onError(null);
        Assertions.assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasText(activity.getString(R.string.loading_text));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

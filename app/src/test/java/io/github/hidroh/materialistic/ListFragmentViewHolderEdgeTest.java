package io.github.hidroh.materialistic;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerViewAdapter;

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Config(shadows = {ShadowRecyclerViewAdapter.class})
@SuppressWarnings("ConstantConditions")
@SuppressLint("WrongViewCast")
@RunWith(TestRunner.class)
public class ListFragmentViewHolderEdgeTest {
    private ActivityController<ListActivity> controller;
    private RecyclerView.ViewHolder holder;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Captor ArgumentCaptor<ResponseListener<Item>> listener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
        when(itemManager.getStories(any(), eq(ItemManager.MODE_DEFAULT))).thenReturn(new Item[]{new TestHnItem(1L)});
        controller = Robolectric.buildActivity(ListActivity.class)
                .create().start().resume().visible();
        ListActivity activity = controller.get();
        Bundle args = new Bundle();
        args.putString(ListFragment.EXTRA_ITEM_MANAGER, HackerNewsClient.class.getName());
        args.putString(ListFragment.EXTRA_FILTER, ItemManager.TOP_FETCH_MODE);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content,
                        Fragment.instantiate(activity, ListFragment.class.getName(), args))
                .commit();
        verify(itemManager).getStories(any(), eq(ItemManager.MODE_DEFAULT));
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        ShadowRecyclerViewAdapter shadowAdapter = customShadowOf(recyclerView.getAdapter());
        holder = shadowAdapter.getViewHolder(0);
    }

    @Test
    public void testNullResponse() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), listener.capture());
        listener.getValue().onResponse(null);
        assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasText(R.string.loading_text);
    }

    @Test
    public void testErrorResponse() {
        verify(itemManager).getItem(any(), eq(ItemManager.MODE_DEFAULT), listener.capture());
        listener.getValue().onError(null);
        assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasText(R.string.loading_text);
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

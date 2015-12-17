package io.github.hidroh.materialistic;

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
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.test.ListActivity;
import io.github.hidroh.materialistic.test.ShadowRecyclerViewAdapter;
import io.github.hidroh.materialistic.test.TestItem;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@Config(shadows = {ShadowRecyclerViewAdapter.class, ShadowRecyclerViewAdapter.ShadowViewHolder.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ListFragmentViewHolderEdgeTest {
    private ActivityController<ListActivity> controller;
    private RecyclerView.ViewHolder holder;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Captor ArgumentCaptor<ResponseListener<ItemManager.Item>> listener;
    @Captor ArgumentCaptor<ResponseListener<ItemManager.Item[]>> storiesListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
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
        verify(itemManager).getStories(anyString(), storiesListener.capture());
        storiesListener.getValue().onResponse(new ItemManager.Item[]{new TestItem() {
        }});
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        ShadowRecyclerViewAdapter shadowAdapter = ((ShadowRecyclerViewAdapter) ShadowExtractor
                .extract(recyclerView.getAdapter()));
        shadowAdapter.makeItemVisible(0);
        holder = shadowAdapter.getViewHolder(0);
    }

    @Test
    public void testNullResponse() {
        verify(itemManager).getItem(anyString(), listener.capture());
        listener.getValue().onResponse(null);
        assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasText(R.string.loading_text);
    }

    @Test
    public void testErrorResponse() {
        verify(itemManager).getItem(anyString(), listener.capture());
        listener.getValue().onError(null);
        assertThat((TextView) holder.itemView.findViewById(R.id.title))
                .hasText(R.string.loading_text);
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

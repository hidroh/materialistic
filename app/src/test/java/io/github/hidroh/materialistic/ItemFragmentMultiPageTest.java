package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
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
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.CustomShadows;
import io.github.hidroh.materialistic.test.shadow.ShadowPreferenceFragmentCompat;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerViewAdapter;
import io.github.hidroh.materialistic.test.shadow.ShadowSwipeRefreshLayout;
import io.github.hidroh.materialistic.test.suite.SlowTest;

import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Category(SlowTest.class)
@Config(shadows = {ShadowSwipeRefreshLayout.class, ShadowRecyclerViewAdapter.class, ShadowPreferenceFragmentCompat.class})
@RunWith(TestRunner.class)
public class ItemFragmentMultiPageTest {
    @Inject @Named(ActivityModule.HN) ItemManager hackerNewsClient;
    @Captor ArgumentCaptor<ResponseListener<Item>> listener;
    private ActivityController<TestItemActivity> controller;
    private TestItemActivity activity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(hackerNewsClient);
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putString(RuntimeEnvironment.application.getString(R.string.pref_comment_display),
                        RuntimeEnvironment.application.getString(R.string.pref_comment_display_value_multiple))
                .putBoolean(RuntimeEnvironment.application.getString(R.string.pref_lazy_load), false)
                .apply();
        controller = Robolectric.buildActivity(TestItemActivity.class);
        activity = controller.create().start().resume().get();
    }

    @Test
    public void testEmptyView() {
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, new TestItem() { });
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        makeVisible(fragment);
        assertThat((View) fragment.getView().findViewById(R.id.empty)).isVisible();
    }

    @Test
    public void testWebItem() {
        WebItem webItem = mock(WebItem.class);
        when(webItem.getId()).thenReturn("1");
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, webItem);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        makeVisible(fragment);
        verify(hackerNewsClient).getItem(eq("1"),
                eq(ItemManager.MODE_DEFAULT),
                listener.capture());
        listener.getValue().onResponse(new TestItem() {
            @Override
            public Item[] getKidItems() {
                return new Item[]{new TestItem() {
                }};
            }

            @Override
            public int getKidCount() {
                return 1;
            }
        });
        assertThat((View) fragment.getView().findViewById(R.id.empty)).isNotVisible();
    }

    @Test
    public void testBindLocalKidData() {
        Item story = new TestHnItem(0L);
        story.populate(new TestItem() {
            @Override
            public int getDescendants() {
                return 1;
            }

            @Override
            public long[] getKids() {
                return new long[]{1L};
            }
        });
        story.getKidItems()[0].populate(new TestItem() {
            @Override
            public String getText() {
                return "text";
            }

            @Override
            public long[] getKids() {
                return new long[]{2L};
            }

            @Override
            public int getDescendants() {
                return 1;
            }
        });
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, story);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        makeVisible(fragment);
        assertThat((View) fragment.getView().findViewById(R.id.empty)).isNotVisible();
        RecyclerView recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        RecyclerView.ViewHolder viewHolder = CustomShadows.customShadowOf(recyclerView.getAdapter())
                .getViewHolder(0);
        assertThat((TextView) viewHolder.itemView.findViewById(R.id.text)).hasTextString("text");
        assertThat((View) viewHolder.itemView.findViewById(R.id.comment)).isVisible();
        viewHolder.itemView.findViewById(R.id.comment).performClick();
        Intent actual = shadowOf(fragment.getActivity()).getNextStartedActivity();
        assertEquals(ItemActivity.class.getName(), actual.getComponent().getClassName());
        assertThat(actual).hasExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true);
    }

    @Test
    public void testBindRemoteKidData() {
        Bundle args = new Bundle();
        Item item = new TestHnItem(2L);
        item.populate(new TestHnItem(2L) {
            @Override
            public long[] getKids() {
                return new long[]{1L};
            }
        });
        args.putParcelable(ItemFragment.EXTRA_ITEM, item);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        makeVisible(fragment);
        verify(hackerNewsClient).getItem(eq("1"),
                eq(ItemManager.MODE_DEFAULT),
                listener.capture());
        listener.getValue().onResponse(new TestHnItem(1L) {
            @Override
            public String getTitle() {
                return "title";
            }
        });
        assertEquals(1, item.getKidItems()[0].getLocalRevision());
        assertEquals("title", item.getKidItems()[0].getTitle());
    }

    @Test
    public void testRefresh() {
        WebItem webItem = mock(WebItem.class);
        when(webItem.getId()).thenReturn("1");
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, webItem);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        makeVisible(fragment);
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = (ShadowSwipeRefreshLayout)
                ShadowExtractor.extract(fragment.getView().findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        verify(hackerNewsClient).getItem(eq("1"),
                eq(ItemManager.MODE_DEFAULT),
                listener.capture());
        verify(hackerNewsClient).getItem(eq("1"),
                eq(ItemManager.MODE_NETWORK),
                listener.capture());
        listener.getAllValues().get(1).onResponse(new TestHnItem(1L));
        assertThat((SwipeRefreshLayout) fragment.getView().findViewById(R.id.swipe_layout))
                .isNotRefreshing();
        verify(((TestItemActivity) fragment.getActivity()).itemChangedListener)
                .onItemChanged(any(Item.class));
    }

    @Test
    public void testRefreshFailed() {
        WebItem webItem = mock(WebItem.class);
        when(webItem.getId()).thenReturn("1");
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, webItem);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        makeVisible(fragment);
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = (ShadowSwipeRefreshLayout)
                ShadowExtractor.extract(fragment.getView().findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        verify(hackerNewsClient).getItem(eq("1"),
                eq(ItemManager.MODE_DEFAULT),
                listener.capture());
        verify(hackerNewsClient).getItem(eq("1"),
                eq(ItemManager.MODE_NETWORK),
                listener.capture());
        listener.getAllValues().get(1).onError(null);
        assertThat((SwipeRefreshLayout) fragment.getView().findViewById(R.id.swipe_layout))
                .isNotRefreshing();
    }

    @Test
    public void testDisplayMenu() {
        WebItem webItem = mock(WebItem.class);
        when(webItem.getId()).thenReturn("1");
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, webItem);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        makeVisible(fragment);
        fragment.onOptionsItemSelected(new RoboMenuItem(R.id.menu_comments));
        assertThat(fragment.getFragmentManager())
                .hasFragmentWithTag(PopupSettingsFragment.class.getName());
    }

    @After
    public void tearDown() {
        reset(hackerNewsClient);
        controller.pause().stop().destroy();
    }

    private void makeVisible(Fragment fragment) {
        activity.getSupportFragmentManager().beginTransaction()
                .add(R.id.content_frame, fragment, "tag")
                .commit();
        View view = activity.findViewById(R.id.recycler_view);
        view.measure(0, 0);
        view.layout(0, 0, 100, 1000);
    }

    public static class TestItemActivity extends InjectableActivity
            implements ItemFragment.ItemChangedListener {
        ItemFragment.ItemChangedListener itemChangedListener = mock(ItemFragment.ItemChangedListener.class);
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_item);
            setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        }

        @Override
        public void onItemChanged(@NonNull Item item) {
            itemChangedListener.onItemChanged(item);
        }
    }
}

package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.ShadowSwipeRefreshLayout;
import io.github.hidroh.materialistic.test.TestItemActivity;
import io.github.hidroh.materialistic.test.TestItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowSwipeRefreshLayout.class, ShadowSupportPreferenceManager.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ItemFragmentMultiPageTest {
    @Inject @Named(ActivityModule.HN) ItemManager hackerNewsClient;
    @Captor ArgumentCaptor<ItemManager.ResponseListener<ItemManager.Item>> listener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(hackerNewsClient);
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putString(RuntimeEnvironment.application.getString(R.string.pref_comment_display),
                        RuntimeEnvironment.application.getString(R.string.pref_comment_display_value_multiple))
                .putBoolean(RuntimeEnvironment.application.getString(R.string.pref_lazy_load), false)
                .commit();
    }

    @Test
    public void testEmptyView() {
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, new TestItem() { });
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestItemActivity.class,
                R.id.content_frame);
        assertThat(fragment.getView().findViewById(android.R.id.empty)).isVisible();
    }

    @Test
    public void testWebItem() {
        ItemManager.WebItem webItem = mock(ItemManager.WebItem.class);
        when(webItem.getId()).thenReturn("1");
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, webItem);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestItemActivity.class,
                R.id.content_frame);
        verify(hackerNewsClient).getItem(eq("1"), listener.capture());
        listener.getValue().onResponse(new TestItem() {
            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{new TestItem() {
                }};
            }

            @Override
            public int getKidCount() {
                return 1;
            }
        });
        assertThat(fragment.getView().findViewById(android.R.id.empty)).isNotVisible();
    }

    @Test
    public void testBindLocalKidData() {
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, new TestItem() {
            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{new TestItem() {
                    @Override
                    public String getText() {
                        return "text";
                    }

                    @Override
                    public int getKidCount() {
                        return 1;
                    }
                }};
            }

            @Override
            public int getKidCount() {
                return 1;
            }
        });
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestItemActivity.class,
                R.id.content_frame);
        assertThat(fragment.getView().findViewById(android.R.id.empty)).isNotVisible();
        RecyclerView recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder, 0);
        assertThat((TextView) viewHolder.itemView.findViewById(R.id.text)).hasTextString("text");
        assertThat(viewHolder.itemView.findViewById(R.id.comment)).isVisible();
        viewHolder.itemView.findViewById(R.id.comment).performClick();
        Intent actual = shadowOf(fragment.getActivity()).getNextStartedActivity();
        assertEquals(ItemActivity.class.getName(), actual.getComponent().getClassName());
        assertThat(actual).hasExtra(ItemActivity.EXTRA_OPEN_COMMENTS, true);
    }

    @Test
    public void testBindRemotelKidData() {
        final ItemManager.Item kidItem = mock(ItemManager.Item.class);
        when(kidItem.getId()).thenReturn("1");
        when(kidItem.getLocalRevision()).thenReturn(-1); // force remote retrieval
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, new TestItem() {
            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{kidItem};
            }

            @Override
            public int getKidCount() {
                return 1;
            }
        });
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestItemActivity.class,
                R.id.content_frame);
        RecyclerView recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        RecyclerView.ViewHolder viewHolder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder, 0);
        verify(hackerNewsClient).getItem(eq("1"), listener.capture());
        TestItem remoteKidItem = new TestItem() {
            @Override
            public String getId() {
                return "1";
            }
        };
        listener.getValue().onResponse(remoteKidItem);
        verify(kidItem).populate(eq(remoteKidItem));
        verify(kidItem).setLocalRevision(eq(0));
    }

    @Test
    public void testRefresh() {
        ItemManager.WebItem webItem = mock(ItemManager.WebItem.class);
        when(webItem.getId()).thenReturn("1");
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, webItem);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestItemActivity.class,
                R.id.content_frame);
        ShadowSwipeRefreshLayout shadowSwipeRefreshLayout = (ShadowSwipeRefreshLayout)
                ShadowExtractor.extract(fragment.getView().findViewById(R.id.swipe_layout));
        shadowSwipeRefreshLayout.getOnRefreshListener().onRefresh();
        verify(hackerNewsClient, times(2)).getItem(eq("1"), listener.capture());
        listener.getAllValues().get(1).onError(null);
        assertThat((SwipeRefreshLayout) fragment.getView().findViewById(R.id.swipe_layout))
                .isNotRefreshing();
    }

    @Test
    public void testDisabledColorCode() {
        ItemManager.WebItem webItem = mock(ItemManager.WebItem.class);
        when(webItem.getId()).thenReturn("1");
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, webItem);
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestItemActivity.class,
                R.id.content_frame);
        FragmentActivity activity = fragment.getActivity();
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_color_code).isEnabled());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_color_code).isChecked());
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_color_code), true)
                .commit();
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_color_code).isEnabled());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_color_code).isChecked());
    }

    @After
    public void tearDown() {
        reset(hackerNewsClient);
    }
}

package io.github.hidroh.materialistic;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.ShadowTextView;
import io.github.hidroh.materialistic.test.TestInjectableActivity;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.ToggleItemViewHolder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowRecyclerView.class, ShadowSupportPreferenceManager.class, ShadowTextView.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ItemFragmentSinglePageTest {
    @Inject
    @Named(ActivityModule.HN)
    ItemManager hackerNewsClient;
    @Captor
    ArgumentCaptor<ItemManager.ResponseListener<ItemManager.Item>> listener;
    private RecyclerView recyclerView;
    private SinglePageItemRecyclerViewAdapter adapter;
    private ToggleItemViewHolder viewHolder;
    private ToggleItemViewHolder viewHolder1;
    private ToggleItemViewHolder viewHolder2;
    private TestInjectableActivity activity;

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
                        RuntimeEnvironment.application.getString(R.string.pref_comment_display_value_single))
                .commit();
        final TestItem item2 = new TestItem() { // level 2
            @Override
            public String getId() {
                return "3";
            }

            @Override
            public int getKidCount() {
                return 0;
            }

            @Override
            public String getParent() {
                return "2";
            }

            @Override
            public boolean isDead() {
                return true;
            }

            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{};
            }
        };
        final TestItem item1 = new TestItem() { // level 1
            @Override
            public String getId() {
                return "2";
            }

            @Override
            public String getParent() {
                return "1";
            }

            @Override
            public boolean isDeleted() {
                return true;
            }

            @Override
            public String getText() {
                return "text";
            }

            @Override
            public int getKidCount() {
                return 1;
            }

            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{item2};
            }
        };
        final TestItem item0 = new TestItem() { // level 0
            @Override
            public String getId() {
                return "1";
            }

            @Override
            public int getLevel() {
                return 1;
            }

            @Override
            public String getText() {
                return "text";
            }

            @Override
            public int getKidCount() {
                return 1;
            }

            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{item1};
            }
        };
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, new TestItem() {
            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{item0};
            }

            @Override
            public int getKidCount() {
                return 1;
            }
        });
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        activity = Robolectric.buildActivity(TestInjectableActivity.class)
                .create().start().resume().visible().get();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment)
                .commit();
        recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        adapter = (SinglePageItemRecyclerViewAdapter) recyclerView.getAdapter();
        // auto expand all
        viewHolder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder, 0);
        viewHolder1 = adapter.createViewHolder(recyclerView, 1);
        adapter.bindViewHolder(viewHolder1, 1);
        viewHolder2 = adapter.createViewHolder(recyclerView, 2);
        adapter.bindViewHolder(viewHolder2, 2);
    }

    @Test
    public void testExpand() {
        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void testGetItemType() {
        assertEquals(0, adapter.getItemViewType(0));
    }

    @Test
    public void testPendingItem() {
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, new TestItem() {
            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{new TestItem() {
                    @Override
                    public int getLocalRevision() {
                        return -1;
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
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestInjectableActivity.class,
                android.R.id.content);
        recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        adapter = (SinglePageItemRecyclerViewAdapter) recyclerView.getAdapter();
        ToggleItemViewHolder viewHolder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder, 0);
    }

    @Test
    public void testToggle() {
        // collapse all
        viewHolder.itemView.findViewById(R.id.toggle).performClick();
        adapter.bindViewHolder(viewHolder, 0);
        assertEquals(1, adapter.getItemCount());

        // expand again, should add item when binding
        viewHolder.itemView.findViewById(R.id.toggle).performClick();
        adapter.bindViewHolder(viewHolder, 0);
        adapter.bindViewHolder(viewHolder1, 1);
        adapter.bindViewHolder(viewHolder2, 2);
        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void testScrollToParent() {
        // test smooth scroll
        ShadowRecyclerView shadowRecyclerView =
                (ShadowRecyclerView) ShadowExtractor.extract(recyclerView);
        assertEquals(-1, shadowRecyclerView.getSmoothScrollToPosition());
        viewHolder2.itemView.findViewById(R.id.posted).performClick();
        assertEquals(1, shadowRecyclerView.getSmoothScrollToPosition());
    }

    @Test
    public void testDeleted() {
        assertNull(shadowOf(viewHolder1.itemView.findViewById(R.id.posted)).getOnClickListener());
    }

    @Test
    public void testDead() {
        assertThat(((TextView) viewHolder.itemView.findViewById(R.id.text)))
                .hasCurrentTextColor(ContextCompat.getColor(activity, R.color.blackT87));
        assertThat(((TextView) viewHolder2.itemView.findViewById(R.id.text)))
                .hasCurrentTextColor(ContextCompat.getColor(activity,
                        AppUtils.getThemedResId(activity, android.R.attr.textColorSecondary)));
    }

    @Test
    public void testDefaultCollapsed() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putString(RuntimeEnvironment.application.getString(R.string.pref_comment_display),
                        RuntimeEnvironment.application.getString(R.string.pref_comment_display_value_collapsed))
                .commit();
        final TestItem item0 = new TestItem() { // level 0
            @Override
            public String getId() {
                return "1";
            }

            @Override
            public int getKidCount() {
                return 1;
            }

            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{new TestItem() {
                    @Override
                    public String getId() {
                        return "2";
                    }
                }};
            }
        };
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, new TestItem() {
            @Override
            public ItemManager.Item[] getKidItems() {
                return new ItemManager.Item[]{item0};
            }

            @Override
            public int getKidCount() {
                return 1;
            }
        });
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, TestInjectableActivity.class,
                android.R.id.content);
        recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        adapter = (SinglePageItemRecyclerViewAdapter) recyclerView.getAdapter();
        assertEquals(1, adapter.getItemCount());
        ToggleItemViewHolder viewHolder = adapter.createViewHolder(recyclerView, 0);
        adapter.bindViewHolder(viewHolder, 0);
        assertEquals(1, adapter.getItemCount()); // should not add kid to adapter

    }

    @Test
    public void testSavedState() {
        shadowOf(activity).recreate();
        assertEquals(3, adapter.getItemCount());
    }

    @Test
    public void testDefaultDisplayAllLines() {
        assertThat(viewHolder.itemView.findViewById(R.id.more)).isNotVisible();
    }

    @Test
    public void testDisplayMaxLines() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_max_lines), "3")
                .commit();
        adapter.onAttachedToRecyclerView(recyclerView);
        TextView textView = (TextView) viewHolder.itemView.findViewById(R.id.text);
        View more = viewHolder.itemView.findViewById(R.id.more);
        ((ShadowTextView) ShadowExtractor.extract(textView)).setLineCount(10);
        adapter.bindViewHolder(viewHolder, 0);
        assertThat(textView).hasMaxLines(3);
        assertThat(more).isVisible();
        more.performClick();
        assertThat(textView).hasMaxLines(10);
        assertThat(more).isNotVisible();
        adapter.bindViewHolder(viewHolder, 0);
        assertThat(more).isNotVisible();
    }

    @Test
    public void testMaxLines() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_max_lines), "5")
                .commit();
        adapter.onAttachedToRecyclerView(recyclerView);
        TextView textView = (TextView) viewHolder.itemView.findViewById(R.id.text);
        View more = viewHolder.itemView.findViewById(R.id.more);
        ((ShadowTextView) ShadowExtractor.extract(textView)).setLineCount(3);
        adapter.bindViewHolder(viewHolder, 0);
        assertThat(textView).hasMaxLines(Integer.MAX_VALUE);
        assertThat(more).isNotVisible();
    }

    @After
    public void tearDown() {
        recyclerView.setAdapter(null);
        reset(hackerNewsClient);
    }
}

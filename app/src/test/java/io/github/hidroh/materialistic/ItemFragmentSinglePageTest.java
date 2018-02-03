package io.github.hidroh.materialistic;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;
import org.robolectric.util.ActivityController;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestLayoutManager;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowItemTouchHelper;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerViewAdapter;
import io.github.hidroh.materialistic.test.shadow.ShadowTextView;
import io.github.hidroh.materialistic.test.suite.SlowTest;
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.ToggleItemViewHolder;

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Category(SlowTest.class)
@Config(shadows = {ShadowRecyclerViewAdapter.class,
        ShadowTextView.class})
@RunWith(TestRunner.class)
public class ItemFragmentSinglePageTest {
    @Inject @Named(ActivityModule.HN) ItemManager hackerNewsClient;
    @Inject UserServices userServices;
    @Captor ArgumentCaptor<ResponseListener<Item>> listener;
    @Captor ArgumentCaptor<UserServices.Callback> voteCallback;
    private RecyclerView recyclerView;
    private SinglePageItemRecyclerViewAdapter adapter;
    private ToggleItemViewHolder viewHolder;
    private ToggleItemViewHolder viewHolder1;
    private ToggleItemViewHolder viewHolder2;
    private ItemFragmentMultiPageTest.TestItemActivity activity;
    private ItemFragment fragment;
    private ActivityController<ItemFragmentMultiPageTest.TestItemActivity> controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(hackerNewsClient);
        reset(userServices);
        shadowOf(((WindowManager) RuntimeEnvironment.application
                .getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay())
                .setHeight(0); // no preload
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        final TestHnItem item0 = new TestHnItem(1, 1) {
            @Override
            public long getNeighbour(int direction) {
                if (direction == Navigable.DIRECTION_DOWN) {
                    return 4L;
                }
                return super.getNeighbour(direction);
            }
        };
        item0.populate(new TestItem() { // level 0
            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getText() {
                return "text";
            }

            @Override
            public int getDescendants() {
                return 1;
            }

            @Override
            public long[] getKids() {
                return new long[]{2L};
            }
        });
        final TestHnItem item1 = new TestHnItem(2, 2);
        item1.populate( new TestItem() { // level 1
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
            public int getDescendants() {
                return 1;
            }

            @Override
            public long[] getKids() {
                return new long[]{3L};
            }
        });
        item0.getKidItems()[0] = item1;
        final TestHnItem item2 = new TestHnItem(3, 3);
        item2.populate(new TestItem() { // level 2
            @Override
            public String getId() {
                return "3";
            }

            @Override
            public long[] getKids() {
                return new long[0];
            }

            @Override
            public int getDescendants() {
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
        });
        item1.getKidItems()[0] = item2;
        TestHnItem item3 = new TestHnItem(4, 4) {
            @Override
            public long getNeighbour(int direction) {
                if (direction == Navigable.DIRECTION_UP) {
                    return 1L;
                }
                return super.getNeighbour(direction);
            }
        };
        item3.populate(new TestItem() { // level 0
            @Override
            public String getId() {
                return "4";
            }

            @Override
            public String getText() {
                return "text";
            }

            @Override
            public int getDescendants() {
                return 0;
            }

            @Override
            public long[] getKids() {
                return new long[0];
            }
        });
        TestHnItem story = new TestHnItem(0);
        story.populate(new TestItem() {
            @Override
            public long[] getKids() {
                return new long[]{1L, 4L};
            }

            @Override
            public int getDescendants() {
                return 4;
            }
        });
        story.getKidItems()[0] = item0;
        story.getKidItems()[1] = item3;
        Bundle args = new Bundle();
        args.putParcelable(ItemFragment.EXTRA_ITEM, story);
        fragment = (ItemFragment) Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        controller = Robolectric.buildActivity(ItemFragmentMultiPageTest.TestItemActivity.class);
        activity = controller.create().start().resume().visible().get();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_lazy_load), false)
                .apply();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.content_frame, fragment, ItemFragment.class.getName())
                .commit();
        recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        adapter = (SinglePageItemRecyclerViewAdapter) recyclerView.getAdapter();
        // auto expand all
        viewHolder = (ToggleItemViewHolder) customShadowOf(adapter).getViewHolder(0);
        viewHolder1 = (ToggleItemViewHolder) customShadowOf(adapter).getViewHolder(1);
        viewHolder2 = (ToggleItemViewHolder) customShadowOf(adapter).getViewHolder(2);
    }

    @Test
    public void testExpand() {
        assertEquals(5, adapter.getItemCount()); // 4 items + footer
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
            public Item[] getKidItems() {
                return new Item[]{new TestItem() {
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
        SupportFragmentTestUtil.startVisibleFragment(fragment, ItemFragmentMultiPageTest.TestItemActivity.class,
                R.id.content_frame);
        recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        adapter = (SinglePageItemRecyclerViewAdapter) recyclerView.getAdapter();
    }

    @Test @Ignore
    public void testToggle() {
        // collapse all
        viewHolder.itemView.findViewById(R.id.button_toggle).performClick();
        assertEquals(3, adapter.getItemCount()); // 2 items + footer

        // expand again, should add item when binding
        viewHolder.itemView.findViewById(R.id.button_toggle).performClick();
        assertEquals(5, adapter.getItemCount()); // 4 items + footer
    }

    @Config(shadows = {ShadowRecyclerView.class, ShadowItemTouchHelper.class})
    @Test
    public void testSwipeToToggle() {
        ItemTouchHelper.SimpleCallback callback = (ItemTouchHelper.SimpleCallback)
                customShadowOf(recyclerView).getItemTouchHelperCallback();
        assertThat(callback.getSwipeThreshold(viewHolder)).isGreaterThan(0f);
        assertThat(callback.onMove(recyclerView, viewHolder, viewHolder)).isFalse();
        assertThat(callback.getSwipeDirs(recyclerView, viewHolder))
                .isEqualTo(ItemTouchHelper.RIGHT);
        callback.onChildDraw(mock(Canvas.class), recyclerView, viewHolder, 1f, 0f,
                ItemTouchHelper.ACTION_STATE_SWIPE, true);
        // collapse all
        callback.onSwiped(viewHolder, ItemTouchHelper.RIGHT);
        assertEquals(3, adapter.getItemCount()); // 2 items + footer
    }

    @Test
    public void testDeleted() {
        assertNull(shadowOf((View) viewHolder1.itemView.findViewById(R.id.posted)).getOnClickListener());
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
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putString(RuntimeEnvironment.application.getString(R.string.pref_comment_display),
                        RuntimeEnvironment.application.getString(R.string.pref_comment_display_value_collapsed))
                .apply();
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
            public Item[] getKidItems() {
                return new Item[]{new TestItem() {
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
            public Item[] getKidItems() {
                return new Item[]{item0};
            }

            @Override
            public int getKidCount() {
                return 1;
            }
        });
        Fragment fragment = Fragment.instantiate(RuntimeEnvironment.application,
                ItemFragment.class.getName(), args);
        SupportFragmentTestUtil.startVisibleFragment(fragment, ItemFragmentMultiPageTest.TestItemActivity.class,
                R.id.content_frame);
        recyclerView = (RecyclerView) fragment.getView().findViewById(R.id.recycler_view);
        adapter = (SinglePageItemRecyclerViewAdapter) recyclerView.getAdapter();
        assertEquals(2, adapter.getItemCount()); // item + footer
        assertEquals(2, adapter.getItemCount()); // should not add kid to adapter
    }

    @Test
    public void testSavedState() {
        shadowOf(activity).recreate();
        assertEquals(5, adapter.getItemCount());
    }

    @Test
    public void testDefaultDisplayAllLines() {
        assertThat((View) viewHolder.itemView.findViewById(R.id.more)).isNotVisible();
    }

    @Test
    public void testChangeThreadDisplay() {
        assertSinglePage();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_comment_display),
                        activity.getString(R.string.pref_comment_display_value_single))
                .apply(); // still single
        assertSinglePage();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_comment_display),
                        activity.getString(R.string.pref_comment_display_value_multiple))
                .apply(); // multiple
        assertMultiplePage();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVote() {
        viewHolder.itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), any(), voteCallback.capture());
        voteCallback.getValue().onDone(true);
        assertEquals(activity.getString(R.string.voted), ShadowToast.getTextOfLatestToast());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItemPromptToLogin() {
        viewHolder.itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), any(), voteCallback.capture());
        voteCallback.getValue().onDone(false);
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, LoginActivity.class);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testVoteItemFailed() {
        viewHolder.itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_vote));
        verify(userServices).voteUp(any(Context.class), any(), voteCallback.capture());
        voteCallback.getValue().onError(new IOException());
        assertEquals(activity.getString(R.string.vote_failed), ShadowToast.getTextOfLatestToast());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testReply() {
        viewHolder.itemView.findViewById(R.id.button_more).performClick();
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
    public void testShare() {
        TestApplication.addResolver(new Intent(Intent.ACTION_SEND));
        viewHolder.itemView.findViewById(R.id.button_more).performClick();
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        shadowOf(popupMenu).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_contextual_share));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasAction(Intent.ACTION_SEND);
    }

    @Config(shadows = ShadowRecyclerView.class)
    @Test @Ignore
    public void testNavigate() {
        ShadowRecyclerView shadowRecyclerView = customShadowOf(recyclerView);
        TestLayoutManager testLayout
                = new TestLayoutManager(activity);
        recyclerView.setLayoutManager(testLayout);
        fragment.onNavigate(Navigable.DIRECTION_DOWN);
        assertThat(shadowRecyclerView.getScrollPosition()).isEqualTo(3);
        shadowRecyclerView.getScrollListener()
                .onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE);

        testLayout.firstVisiblePosition = 3;
        fragment.onNavigate(Navigable.DIRECTION_UP);
        assertThat(shadowRecyclerView.getScrollPosition()).isEqualTo(0);

        testLayout.firstVisiblePosition = 0;
        fragment.onNavigate(Navigable.DIRECTION_RIGHT);
        assertThat(shadowRecyclerView.getScrollPosition()).isEqualTo(1);
        shadowRecyclerView.getScrollListener()
                .onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE);

        testLayout.firstVisiblePosition = 1;
        fragment.onNavigate(Navigable.DIRECTION_LEFT);
        assertThat(shadowRecyclerView.getScrollPosition()).isEqualTo(0);
        shadowRecyclerView.getScrollListener()
                .onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE);
    }

    @After
    public void tearDown() {
        reset(hackerNewsClient);
        controller.pause().stop().destroy();
    }

    private void assertSinglePage() {
        assertEquals(activity.getString(R.string.pref_comment_display_value_single),
                Preferences.getCommentDisplayOption(activity));
        assertThat(recyclerView.getAdapter()).isInstanceOf(SinglePageItemRecyclerViewAdapter.class);
    }

    private void assertMultiplePage() {
        assertEquals(activity.getString(R.string.pref_comment_display_value_multiple),
                Preferences.getCommentDisplayOption(activity));
        assertThat(recyclerView.getAdapter()).isInstanceOf(MultiPageItemRecyclerViewAdapter.class);
    }
}

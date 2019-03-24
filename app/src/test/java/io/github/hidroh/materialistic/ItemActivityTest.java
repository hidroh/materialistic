package io.github.hidroh.materialistic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowGestureDetector;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.shadows.ShadowResolveInfo;
import org.robolectric.shadows.ShadowToast;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.MaterialisticDatabase;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.TestWebItem;
import io.github.hidroh.materialistic.test.shadow.ShadowFloatingActionButton;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerView;

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@RunWith(TestRunner.class)
public class ItemActivityTest {
    private ActivityController<ItemActivity> controller;
    private ItemActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager hackerNewsClient;
    @Inject FavoriteManager favoriteManager;
    @Inject UserServices userServices;
    @Inject KeyDelegate keyDelegate;
    @Captor ArgumentCaptor<ResponseListener<Item>> listener;
    @Captor ArgumentCaptor<UserServices.Callback> userServicesCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(hackerNewsClient);
        reset(favoriteManager);
        reset(userServices);
        reset(keyDelegate);
        controller = Robolectric.buildActivity(ItemActivity.class);
        activity = controller.get();
    }

    @Test
    public void testCustomScheme() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(BuildConfig.APPLICATION_ID + "://item/1"));
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        verify(hackerNewsClient).getItem(eq("1"),
                eq(ItemManager.MODE_DEFAULT),
                any(ResponseListener.class));
    }

    @Test
    public void testJobGivenDeepLink() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://news.ycombinator.com/item?id=1"));
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        verify(hackerNewsClient).getItem(eq("1"),
                eq(ItemManager.MODE_DEFAULT),
                listener.capture());
        listener.getValue().onResponse(new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return JOB_TYPE;
            }

            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getUrl() {
                return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
            }

            @Override
            public String getSource() {
                return "http://example.com";
            }

            @Override
            public boolean isStoryType() {
                return true;
            }
        });
        assertEquals(R.drawable.ic_work_white_18dp,
                shadowOf(((TextView) activity.findViewById(R.id.posted))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
        assertThat((TextView) activity.findViewById(R.id.source)).hasText("http://example.com");
        reset(hackerNewsClient);
        activity.recreate();
        verify(hackerNewsClient, never()).getItem(any(),
                eq(ItemManager.MODE_DEFAULT),
                any(ResponseListener.class));
    }

    @Test
    public void testPoll() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return POLL_TYPE;
            }

            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }
        });
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        assertThat((View) activity.findViewById(R.id.source)).isNotVisible();
        assertEquals(R.drawable.ic_poll_white_18dp,
                shadowOf(((TextView) activity.findViewById(R.id.posted))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
    }

    @SuppressLint("NewApi")
    @Test
    public void testOptionExternal() {
        ShadowPackageManager packageManager = shadowOf(RuntimeEnvironment.application.getPackageManager());
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://example.com")),
                ShadowResolveInfo.newResolveInfo("label", "com.android.chrome", "DefaultActivity"));
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse(String.format(HackerNewsClient.WEB_ITEM_PATH, "1"))),
                ShadowResolveInfo.newResolveInfo("label", "com.android.chrome", "DefaultActivity"));
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }

            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();

        // inflate menu, see https://github.com/robolectric/robolectric/issues/1326
        ShadowLooper.pauseMainLooper();
        controller.visible();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();

        // open article
        shadowOf(activity).clickMenuItem(R.id.menu_external);
        shadowOf(ShadowPopupMenu.getLatestPopupMenu())
                .getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_article));
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_VIEW);

        // open item
        shadowOf(activity).clickMenuItem(R.id.menu_external);
        shadowOf(ShadowPopupMenu.getLatestPopupMenu())
                .getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_comments));
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_VIEW);
    }

    @SuppressLint("NewApi")
    @Test
    public void testShare() {
        TestApplication.addResolver(new Intent(Intent.ACTION_SEND));
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }

            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();

        // inflate menu, see https://github.com/robolectric/robolectric/issues/1326
        ShadowLooper.pauseMainLooper();
        controller.visible();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();

        // share article
        shadowOf(activity).clickMenuItem(R.id.menu_share);
        shadowOf(ShadowPopupMenu.getLatestPopupMenu())
                .getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_article));
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        Intent actual = shadowOf(activity).getNextStartedActivity();
        assertThat(actual)
                .hasAction(Intent.ACTION_SEND);

        // share item
        shadowOf(activity).clickMenuItem(R.id.menu_share);
        shadowOf(ShadowPopupMenu.getLatestPopupMenu())
                .getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_comments));
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        actual = shadowOf(activity).getNextStartedActivity();
        assertThat(actual)
                .hasAction(Intent.ACTION_SEND);
    }

    @Test
    public void testHeaderOpenExternal() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_custom_tab), false)
                .apply();
        TestApplication.addResolver(new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")));
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }

            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_external), true)
                .apply();
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        activity.findViewById(R.id.button_article).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_VIEW);
    }

    @Test
    public void testFavoriteStory() {
        Intent intent = new Intent();
        TestHnItem item = new TestHnItem(1L) {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }
        };
        item.setFavorite(true);
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        assertTrue(item.isFavorite());
        activity.findViewById(R.id.bookmarked).performClick();
        verify(favoriteManager).remove(any(Context.class), eq("1"));
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("remove")
                .appendPath("1")
                .build());
        assertFalse(item.isFavorite());
        assertThat((TextView) activity.findViewById(R.id.snackbar_text))
                .isNotNull()
                .containsText(R.string.toast_removed);
        activity.findViewById(R.id.snackbar_action).performClick();
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("add")
                .appendPath("1")
                .build());
        assertTrue(item.isFavorite());
    }

    @Test
    public void testNonFavoriteStory() {
        TestHnItem item = new TestHnItem(1L) {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }
        };
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, item);
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        assertFalse(item.isFavorite());
        activity.findViewById(R.id.bookmarked).performClick();
        MaterialisticDatabase.getInstance(RuntimeEnvironment.application).setLiveValue(MaterialisticDatabase
                .getBaseSavedUri()
                .buildUpon()
                .appendPath("add")
                .appendPath("1")
                .build());
        assertTrue(item.isFavorite());
    }

    @Config(shadows = ShadowRecyclerView.class)
    @Test
    public void testScrollToTop() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getId() {
                return "1";
            }

            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public int getKidCount() {
                return 10;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }
        });
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        // see https://github.com/robolectric/robolectric/issues/1326
        ShadowLooper.pauseMainLooper();
        controller.visible();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        RecyclerView recyclerView = activity.findViewById(R.id.recycler_view);
        recyclerView.smoothScrollToPosition(1);
        assertThat(customShadowOf(recyclerView).getScrollPosition()).isEqualTo(1);
        TabLayout tabLayout = activity.findViewById(R.id.tab_layout);
        assertThat(tabLayout.getTabCount()).isEqualTo(2);
        tabLayout.getTabAt(1).select();
        tabLayout.getTabAt(0).select();
        tabLayout.getTabAt(0).select();
        assertThat(customShadowOf(recyclerView).getScrollPosition()).isEqualTo(0);
    }

    @Test
    public void testDefaultReadabilityView() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_readability))
                .apply();
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getId() {
                return "1";
            }

            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public int getKidCount() {
                return 10;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }
        });
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        TabLayout tabLayout = activity.findViewById(R.id.tab_layout);
        assertEquals(2, tabLayout.getTabCount());
        assertEquals(1, tabLayout.getSelectedTabPosition());
    }

    @Test
    public void testVotePromptToLogin() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestHnItem(1));
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        activity.findViewById(R.id.vote_button).performClick();
        verify(userServices).voteUp(any(Context.class), eq("1"), userServicesCallback.capture());
        userServicesCallback.getValue().onDone(false);
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, LoginActivity.class);
    }

    @Test
    public void testVote() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestHnItem(1));
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        activity.findViewById(R.id.vote_button).performClick();
        verify(userServices).voteUp(any(Context.class), eq("1"), userServicesCallback.capture());
        userServicesCallback.getValue().onDone(true);
        assertEquals(activity.getString(R.string.voted), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testVoteError() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestHnItem(1));
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        activity.findViewById(R.id.vote_button).performClick();
        verify(userServices).voteUp(any(Context.class), eq("1"), userServicesCallback.capture());
        userServicesCallback.getValue().onError(new IOException());
        assertEquals(activity.getString(R.string.vote_failed), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testReply() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_comments))
                .apply();
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestHnItem(1L));
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume();
        activity = controller.get();
        activity.findViewById(R.id.reply_button).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ComposeActivity.class);
    }

    @Test
    public void testVolumeNavigation() {
        Intent intent = new Intent();
        WebItem webItem = new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }

            @Override
            public String getId() {
                return "1";
            }
        };
        intent.putExtra(ItemActivity.EXTRA_ITEM, webItem);
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume().visible();
        activity = controller.get();
        activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(keyDelegate).setScrollable(any(Scrollable.class), any(AppBarLayout.class));
        verify(keyDelegate).onKeyDown(anyInt(), any(KeyEvent.class));
        activity.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP));
        verify(keyDelegate).onKeyUp(anyInt(), any(KeyEvent.class));
        activity.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(keyDelegate).onKeyLongPress(anyInt(), any(KeyEvent.class));
    }

    @Test
    public void testBackPressed() {
        Intent intent = new Intent();
        WebItem webItem = new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }

            @Override
            public String getId() {
                return "1";
            }
        };
        intent.putExtra(ItemActivity.EXTRA_ITEM, webItem);
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume().visible();
        activity = controller.get();
        activity.onKeyDown(KeyEvent.KEYCODE_BACK,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        verify(keyDelegate).setBackInterceptor(any());
        verify(keyDelegate).onKeyDown(anyInt(), any(KeyEvent.class));
    }

    @Config(shadows = {ShadowFloatingActionButton.class})
    @Test
    public void testFullscreen() {
        Intent intent = new Intent();
        WebItem webItem = new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }

            @Override
            public String getId() {
                return "1";
            }
        };
        intent.putExtra(ItemActivity.EXTRA_ITEM, webItem);
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume().visible();
        activity = controller.get();
        ShadowFloatingActionButton shadowFab = Shadow.extract(activity.findViewById(R.id.reply_button));
        assertTrue(shadowFab.isVisible());
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true));
        assertFalse(shadowFab.isVisible());
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, false));
        assertTrue(shadowFab.isVisible());
    }

    @Test
    public void testFullscreenBackPressed() {
        Intent intent = new Intent();
        WebItem webItem = new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }

            @Override
            public String getId() {
                return "1";
            }
        };
        intent.putExtra(ItemActivity.EXTRA_ITEM, webItem);
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume().visible();
        activity = controller.get();
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true));
        activity.onBackPressed();
        assertThat(activity).isNotFinishing();
        activity.onBackPressed();
        assertThat(activity).isFinishing();
    }

    @Test
    public void testItemChanged() {
        startWithIntent();
        TabLayout tabLayout = activity.findViewById(R.id.tab_layout);
        assertEquals(activity.getResources().getQuantityString(R.plurals.comments_count, 0, 0),
                tabLayout.getTabAt(0).getText());
        activity.onItemChanged(new TestHnItem(1L) {
            @Override
            public int getKidCount() {
                return 10;
            }
        });
        assertEquals(activity.getResources().getQuantityString(R.plurals.comments_count, 10, 10),
                tabLayout.getTabAt(0).getText());
    }

    @Test
    public void testNavButtonHint() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_comments))
                .putBoolean(activity.getString(R.string.pref_navigation), true)
                .apply();
        startWithIntent();
        View navButton = activity.findViewById(R.id.navigation_button);
        assertThat(navButton).isVisible();
        ((GestureDetector.SimpleOnGestureListener) getDetector(navButton).getListener())
                .onSingleTapConfirmed(mock(MotionEvent.class));
        assertThat(ShadowToast.getTextOfLatestToast())
                .contains(activity.getString(R.string.hint_nav_short));
    }

    @Test
    public void testNavButtonDrag() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_comments))
                .putBoolean(activity.getString(R.string.pref_navigation), true)
                .apply();
        startWithIntent();
        View navButton = activity.findViewById(R.id.navigation_button);
        assertThat(navButton).isVisible();
        getDetector(navButton).getListener().onLongPress(mock(MotionEvent.class));
        assertThat(ShadowToast.getTextOfLatestToast())
                .contains(activity.getString(R.string.hint_drag));
        MotionEvent motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_MOVE);
        when(motionEvent.getRawX()).thenReturn(1f);
        when(motionEvent.getRawY()).thenReturn(1f);
        shadowOf(navButton).getOnTouchListener().onTouch(navButton, motionEvent);
        motionEvent = mock(MotionEvent.class);
        when(motionEvent.getAction()).thenReturn(MotionEvent.ACTION_UP);
        shadowOf(navButton).getOnTouchListener().onTouch(navButton, motionEvent);
        assertThat(navButton).hasX(1f).hasY(1f);
    }

    @After
    public void tearDown() {
        reset(hackerNewsClient);
        reset(favoriteManager);
        controller.pause().stop().destroy();
    }

    private void startWithIntent() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }

            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        controller = Robolectric.buildActivity(ItemActivity.class, intent);
        controller.create().start().resume().visible();
        activity = controller.get();
    }

    private ShadowGestureDetector getDetector(View view) {
        shadowOf(view).getOnTouchListener()
                .onTouch(view, MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0));
        return shadowOf(ShadowGestureDetector.getLastActiveDetector());

    }
}

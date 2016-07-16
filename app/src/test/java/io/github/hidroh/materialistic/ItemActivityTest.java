package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ShadowContentResolverCompatJellybean;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
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
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentObserver;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowResolveInfo;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;
import org.robolectric.util.ActivityController;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.MaterialisticProvider;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.ShadowFloatingActionButton;
import io.github.hidroh.materialistic.test.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestWebItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowSupportPreferenceManager.class, ShadowRecyclerView.class, ShadowFloatingActionButton.class, ShadowContentResolverCompatJellybean.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ItemActivityTest {
    private ActivityController<ItemActivity> controller;
    private ItemActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager hackerNewsClient;
    @Inject FavoriteManager favoriteManager;
    @Inject UserServices userServices;
    @Inject VolumeNavigationDelegate volumeNavigationDelegate;
    @Captor ArgumentCaptor<ResponseListener<Item>> listener;
    @Captor ArgumentCaptor<FavoriteManager.OperationCallbacks> callbacks;
    @Captor ArgumentCaptor<UserServices.Callback> userServicesCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(hackerNewsClient);
        reset(favoriteManager);
        reset(userServices);
        reset(volumeNavigationDelegate);
        controller = Robolectric.buildActivity(ItemActivity.class);
        activity = controller.get();
    }

    @Test
    public void testCustomScheme() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(BuildConfig.APPLICATION_ID + "://item/1"));
        controller.withIntent(intent).create().start().resume();
        verify(hackerNewsClient).getItem(eq("1"),
                eq(ItemManager.MODE_DEFAULT),
                any(ResponseListener.class));
    }

    @Test
    public void testJobGivenDeepLink() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://news.ycombinator.com/item?id=1"));
        controller.withIntent(intent).create().start().resume();
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
        shadowOf(activity).recreate();
        verify(hackerNewsClient, never()).getItem(anyString(),
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
        controller.withIntent(intent).create().start().resume();
        assertThat(activity.findViewById(R.id.source)).isNotVisible();
        assertEquals(R.drawable.ic_poll_white_18dp,
                shadowOf(((TextView) activity.findViewById(R.id.posted))
                        .getCompoundDrawables()[0]).getCreatedFromResId());
    }

    @Test
    public void testOptionExternal() {
        RobolectricPackageManager packageManager = (RobolectricPackageManager)
                RuntimeEnvironment.application.getPackageManager();
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
        controller.withIntent(intent).create().start().resume();

        // inflate menu, see https://github.com/robolectric/robolectric/issues/1326
        ShadowLooper.pauseMainLooper();
        controller.visible();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();

        // open article
        shadowOf(activity).clickMenuItem(R.id.menu_external);
        ShadowAlertDialog.getLatestAlertDialog()
                .getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_VIEW);

        // open item
        shadowOf(activity).clickMenuItem(R.id.menu_external);
        ShadowAlertDialog.getLatestAlertDialog()
                .getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_VIEW);
    }

    @Test
    public void testShare() {
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
        controller.withIntent(intent).create().start().resume();

        // inflate menu, see https://github.com/robolectric/robolectric/issues/1326
        ShadowLooper.pauseMainLooper();
        controller.visible();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();

        // share article
        shadowOf(activity).clickMenuItem(R.id.menu_share);
        ShadowAlertDialog.getLatestAlertDialog()
                .getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        Intent actual = shadowOf(activity).getNextStartedActivity();
        assertThat(actual)
                .hasAction(Intent.ACTION_CHOOSER);
        assertThat((Intent) actual.getParcelableExtra(Intent.EXTRA_INTENT))
                .hasExtra(Intent.EXTRA_TEXT, "http://example.com");

        // share item
        shadowOf(activity).clickMenuItem(R.id.menu_share);
        ShadowAlertDialog.getLatestAlertDialog()
                .getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        actual = shadowOf(activity).getNextStartedActivity();
        assertThat(actual)
                .hasAction(Intent.ACTION_CHOOSER);
        assertThat((Intent) actual.getParcelableExtra(Intent.EXTRA_INTENT))
                .hasExtra(Intent.EXTRA_TEXT, "https://news.ycombinator.com/item?id=1");
    }

    @Test
    public void testHeaderOpenExternal() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_custom_tab), false)
                .apply();
        RobolectricPackageManager packageManager = (RobolectricPackageManager)
                RuntimeEnvironment.application.getPackageManager();
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://example.com")),
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
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_external), true)
                .apply();
        controller.withIntent(intent).create().start().resume();
        activity.findViewById(R.id.header_card_view).performClick();
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
        controller.withIntent(intent).create().start().resume();
        assertTrue(item.isFavorite());
        ShadowContentObserver observer = shadowOf(shadowOf(activity.getContentResolver())
                .getContentObservers(MaterialisticProvider.URI_FAVORITE)
                .iterator()
                .next());
        activity.findViewById(R.id.bookmarked).performClick();
        verify(favoriteManager).remove(any(Context.class), eq("1"));
        observer.dispatchChange(false, MaterialisticProvider.URI_FAVORITE
                .buildUpon()
                .appendPath("remove")
                .appendPath("1")
                .build());
        assertFalse(item.isFavorite());
        assertThat((TextView) activity.findViewById(R.id.snackbar_text))
                .isNotNull()
                .containsText(R.string.toast_removed);
        activity.findViewById(R.id.snackbar_action).performClick();
        observer.dispatchChange(false, MaterialisticProvider.URI_FAVORITE
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
        controller.withIntent(intent).create().start().resume();
        assertFalse(item.isFavorite());
        activity.findViewById(R.id.bookmarked).performClick();
        ShadowContentObserver observer = shadowOf(shadowOf(activity.getContentResolver())
                .getContentObservers(MaterialisticProvider.URI_FAVORITE)
                .iterator()
                .next());
        observer.dispatchChange(false, MaterialisticProvider.URI_FAVORITE
                .buildUpon()
                .appendPath("add")
                .appendPath("1")
                .build());
        assertTrue(item.isFavorite());
    }

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
        controller.withIntent(intent).create().start().resume();
        // see https://github.com/robolectric/robolectric/issues/1326
        ShadowLooper.pauseMainLooper();
        controller.visible();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
        RecyclerView recyclerView = (RecyclerView) activity.findViewById(R.id.recycler_view);
        recyclerView.smoothScrollToPosition(1);
        assertEquals(1, ((ShadowRecyclerView) ShadowExtractor.extract(recyclerView))
                .getSmoothScrollToPosition());
        TabLayout tabLayout = (TabLayout) activity.findViewById(R.id.tab_layout);
        assertEquals(3, tabLayout.getTabCount());
        tabLayout.getTabAt(1).select();
        tabLayout.getTabAt(0).select();
        tabLayout.getTabAt(0).select();
        assertEquals(0, ((ShadowRecyclerView) ShadowExtractor.extract(recyclerView))
                .getSmoothScrollToPosition());
    }

    @Test
    public void testDefaultReadabilityView() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
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
        controller.withIntent(intent).create().start().resume();
        TabLayout tabLayout = (TabLayout) activity.findViewById(R.id.tab_layout);
        assertEquals(3, tabLayout.getTabCount());
        assertEquals(2, tabLayout.getSelectedTabPosition());
    }

    @Test
    public void testVotePromptToLogin() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestHnItem(1));
        controller.withIntent(intent).create().start().resume();
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
        controller.withIntent(intent).create().start().resume();
        activity.findViewById(R.id.vote_button).performClick();
        verify(userServices).voteUp(any(Context.class), eq("1"), userServicesCallback.capture());
        userServicesCallback.getValue().onDone(true);
        assertEquals(activity.getString(R.string.voted), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testVoteError() {
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestHnItem(1));
        controller.withIntent(intent).create().start().resume();
        activity.findViewById(R.id.vote_button).performClick();
        verify(userServices).voteUp(any(Context.class), eq("1"), userServicesCallback.capture());
        userServicesCallback.getValue().onError(new IOException());
        assertEquals(activity.getString(R.string.vote_failed), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testReply() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_comments))
                .apply();
        Intent intent = new Intent();
        intent.putExtra(ItemActivity.EXTRA_ITEM, new TestHnItem(1L));
        controller.withIntent(intent).create().start().resume();
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
        controller.withIntent(intent).create().start().resume().visible();
        activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(volumeNavigationDelegate).setScrollable(any(Scrollable.class), any(AppBarLayout.class));
        verify(volumeNavigationDelegate).onKeyDown(anyInt(), any(KeyEvent.class));
        activity.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP));
        verify(volumeNavigationDelegate).onKeyUp(anyInt(), any(KeyEvent.class));
        activity.onKeyLongPress(KeyEvent.KEYCODE_VOLUME_UP,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
        verify(volumeNavigationDelegate).onKeyLongPress(anyInt(), any(KeyEvent.class));
    }

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
        controller.withIntent(intent).create().start().resume().visible();
        ShadowFloatingActionButton shadowFab = (ShadowFloatingActionButton) ShadowExtractor
                .extract(activity.findViewById(R.id.reply_button));
        assertTrue(shadowFab.isVisible());
        ShadowLocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(BaseWebFragment.ACTION_FULLSCREEN)
                        .putExtra(BaseWebFragment.EXTRA_FULLSCREEN, true));
        assertFalse(shadowFab.isVisible());
        ShadowLocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(BaseWebFragment.ACTION_FULLSCREEN)
                        .putExtra(BaseWebFragment.EXTRA_FULLSCREEN, false));
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
        controller.withIntent(intent).create().start().resume().visible();
        ShadowLocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(BaseWebFragment.ACTION_FULLSCREEN)
                        .putExtra(BaseWebFragment.EXTRA_FULLSCREEN, true));
        activity.onBackPressed();
        assertThat(activity).isNotFinishing();
        activity.onBackPressed();
        assertThat(activity).isFinishing();
    }

    @After
    public void tearDown() {
        reset(hackerNewsClient);
        reset(favoriteManager);
        controller.pause().stop().destroy();
    }
}

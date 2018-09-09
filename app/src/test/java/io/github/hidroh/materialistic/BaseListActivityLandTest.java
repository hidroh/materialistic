package io.github.hidroh.materialistic;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.PopupMenu;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPopupMenu;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.TestWebItem;
import io.github.hidroh.materialistic.test.shadow.ShadowFloatingActionButton;
import io.github.hidroh.materialistic.test.shadow.ShadowRecyclerView;

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@Config(qualifiers = "w820dp-land", shadows = {ShadowFloatingActionButton.class})
@RunWith(TestRunner.class)
public class BaseListActivityLandTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;
    @Inject KeyDelegate keyDelegate;

    @Before
    public void setUp() {
        TestApplication.applicationGraph.inject(this);
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create().postCreate(null).start().resume().get();
        // inflate menu, see https://github.com/robolectric/robolectric/issues/1326
        ShadowLooper.pauseMainLooper();
        controller.visible();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
    }

    @Test
    public void testCreateLand() {
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share).isVisible());
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_external));
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_external).isVisible());
        assertFalse(((ShadowFloatingActionButton) Shadow.extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    @Test
    public void testRotate() {
        activity.onItemSelected(new TestHnItem(1L) {
            @Override
            public String getDisplayedTitle() {
                return "item title";
            }

            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }
        });
        assertThat(activity).hasTitle("item title");

        Bundle savedState = new Bundle();
        activity.onSaveInstanceState(savedState);
        RuntimeEnvironment.setQualifiers("");
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create(savedState).postCreate(null).start().resume().get();
        assertThat(activity).hasTitle(activity.getString(R.string.title_activity_list));

        savedState = new Bundle();
        activity.onSaveInstanceState(savedState);
        RuntimeEnvironment.setQualifiers("w820dp-land");
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create(savedState).postCreate(null).start().resume().get();
        assertThat(activity).hasTitle("item title");
        assertTrue(((ShadowFloatingActionButton) Shadow.extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testSelectItemOpenStory() {
        assertThat((View) activity.findViewById(R.id.empty_selection)).isVisible();
        activity.onItemSelected(new TestHnItem(1L) {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }
        });
        assertThat((View) activity.findViewById(R.id.empty_selection)).isNotVisible();
        assertStoryMode();
        shadowOf(activity).clickMenuItem(R.id.menu_share);
        PopupMenu popupMenu = ShadowPopupMenu.getLatestPopupMenu();
        assertNotNull(popupMenu);
        assertThat(popupMenu.getMenu()).hasItem(R.id.menu_article).hasItem(R.id.menu_comments);
        shadowOf(activity).clickMenuItem(R.id.menu_external);
        assertNotNull(ShadowPopupMenu.getLatestPopupMenu());
    }

    @Test
    public void testDefaultCommentView() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_comments))
                .apply();
        controller.pause().resume();
        activity.onItemSelected(new TestHnItem(1L) {
            @Override
            public String getId() {
                return "1";
            }

            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }
        });
        assertCommentMode();
        activity.findViewById(R.id.reply_button).performClick();
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, ComposeActivity.class);
    }

    @Test
    public void testDefaultReadabilityView() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_readability))
                .apply();
        controller.pause().resume();
        activity.onItemSelected(new TestHnItem(1L) {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }
        });
        ViewPager viewPager = activity.findViewById(R.id.content);
        viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
        assertStoryMode();
    }

    @Test
    public void testGetSelectedItem() {
        activity.onItemSelected(createWebItem());
        assertNotNull(activity.getSelectedItem());
        shadowOf(activity).recreate();
        assertNotNull(activity.getSelectedItem());
    }

    @Test
    public void testClearSelection() {
        activity.onItemSelected(createWebItem());
        assertThat((View) activity.findViewById(R.id.empty_selection)).isNotVisible();
        activity.onItemSelected(null);
        assertThat((View) activity.findViewById(R.id.empty_selection)).isVisible();
        assertFalse(((ShadowFloatingActionButton) Shadow.extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    @Test
    public void testToggleItemView() {
        activity.onItemSelected(createWebItem());
        TabLayout tabLayout = activity.findViewById(R.id.tab_layout);
        assertEquals(2, tabLayout.getTabCount());
        assertStoryMode();
        tabLayout.getTabAt(0).select();
        assertCommentMode();
        tabLayout.getTabAt(1).select();
        assertStoryMode();
    }

    @Config(shadows = ShadowRecyclerView.class)
    @Test
    public void testScrollItemToTop() {
        activity.onItemSelected(new TestHnItem(1L) {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }
        });
        TabLayout tabLayout = activity.findViewById(R.id.tab_layout);
        assertThat(tabLayout.getTabCount()).isEqualTo(2);
        tabLayout.getTabAt(0).select();
        ViewPager viewPager = activity.findViewById(R.id.content);
        viewPager.getAdapter().instantiateItem(viewPager, 0);
        viewPager.getAdapter().finishUpdate(viewPager);
        RecyclerView itemRecyclerView = viewPager.findViewById(R.id.recycler_view);
        itemRecyclerView.smoothScrollToPosition(1);
        assertThat(customShadowOf(itemRecyclerView).getScrollPosition()).isEqualTo(1);
        tabLayout.getTabAt(1).select();
        tabLayout.getTabAt(0).select();
        tabLayout.getTabAt(0).select();
        assertThat(customShadowOf(itemRecyclerView).getScrollPosition()).isEqualTo(0);
    }

    @Test
    public void testBackPressed() {
        activity.onItemSelected(new TestHnItem(1L) {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }
        });
        activity.onKeyDown(KeyEvent.KEYCODE_BACK,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        verify(keyDelegate).setBackInterceptor(any(KeyDelegate.BackInterceptor.class));
        verify(keyDelegate).onKeyDown(anyInt(), any(KeyEvent.class));
    }

    private void assertCommentMode() {
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(0);
        assertTrue(((ShadowFloatingActionButton) Shadow.extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    private void assertStoryMode() {
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(1);
        assertTrue(((ShadowFloatingActionButton) Shadow.extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    private WebItem createWebItem() {
        return new TestWebItem() {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }
        };
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

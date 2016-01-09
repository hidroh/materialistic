package io.github.hidroh.materialistic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.ShadowFloatingActionButton;
import io.github.hidroh.materialistic.test.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestWebItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@Config(qualifiers = "w820dp-land", shadows = {ShadowRecyclerView.class, ShadowSupportPreferenceManager.class, ShadowFloatingActionButton.class})
@RunWith(RobolectricGradleTestRunner.class)
public class BaseListActivityLandTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;

    @Before
    public void setUp() {
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
        assertFalse(((ShadowFloatingActionButton) ShadowExtractor
                .extract(activity.findViewById(R.id.reply_button))).isVisible());
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
        assertTrue(((ShadowFloatingActionButton) ShadowExtractor
                .extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    @Test
    public void testSelectItemOpenStory() {
        assertThat(activity.findViewById(R.id.empty_selection)).isVisible();
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
        assertThat(activity.findViewById(R.id.empty_selection)).isNotVisible();
        assertStoryMode();
        shadowOf(activity).clickMenuItem(R.id.menu_share);
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertEquals(activity.getString(R.string.share), shadowOf(alertDialog).getMessage());
        assertThat(alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)).hasText(R.string.article);
        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)).hasText(R.string.comments);
        shadowOf(activity).clickMenuItem(R.id.menu_external);
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void testDefaultCommentView() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_comments))
                .commit();
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
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_readability))
                .commit();
        controller.pause().resume();
        activity.onItemSelected(new TestHnItem(1L) {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }
        });
        ViewPager viewPager = (ViewPager) activity.findViewById(R.id.content);
        viewPager.getAdapter().instantiateItem(viewPager, viewPager.getCurrentItem());
        assertReadabilityMode();
    }

    @Test
    public void testGetSelectedItem() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        assertNotNull(activity.getSelectedItem());
        shadowOf(activity).recreate();
        assertNotNull(activity.getSelectedItem());
    }

    @Test
    public void testClearSelection() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        assertThat(activity.findViewById(R.id.empty_selection)).isNotVisible();
        activity.onItemSelected(null);
        assertThat(activity.findViewById(R.id.empty_selection)).isVisible();
        assertFalse(((ShadowFloatingActionButton) ShadowExtractor
                .extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    @Test
    public void testToggleItemView() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        TabLayout tabLayout = (TabLayout) activity.findViewById(R.id.tab_layout);
        assertEquals(3, tabLayout.getTabCount());
        assertStoryMode();
        tabLayout.getTabAt(0).select();
        assertCommentMode();
        tabLayout.getTabAt(1).select();
        assertStoryMode();
        tabLayout.getTabAt(2).select();
        assertReadabilityMode();
    }

    @Test
    public void testScrollItemToTop() {
        activity.onItemSelected(new TestHnItem(1L) {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }
        });
        TabLayout tabLayout = (TabLayout) activity.findViewById(R.id.tab_layout);
        assertEquals(3, tabLayout.getTabCount());
        tabLayout.getTabAt(0).select();
        ViewPager viewPager = (ViewPager) activity.findViewById(R.id.content);
        viewPager.getAdapter().instantiateItem(viewPager, 0);
        viewPager.getAdapter().finishUpdate(viewPager);
        RecyclerView itemRecyclerView = (RecyclerView) viewPager.findViewById(R.id.recycler_view);
        itemRecyclerView.smoothScrollToPosition(1);
        assertEquals(1, ((ShadowRecyclerView) ShadowExtractor.extract(itemRecyclerView))
                .getSmoothScrollToPosition());
        tabLayout.getTabAt(1).select();
        tabLayout.getTabAt(0).select();
        tabLayout.getTabAt(0).select();
        assertEquals(0, ((ShadowRecyclerView) ShadowExtractor.extract(itemRecyclerView))
                .getSmoothScrollToPosition());
    }

    private void assertCommentMode() {
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(0);
        assertTrue(((ShadowFloatingActionButton) ShadowExtractor
                .extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    private void assertStoryMode() {
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(1);
        assertTrue(((ShadowFloatingActionButton) ShadowExtractor
                .extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    private void assertReadabilityMode() {
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(2);
        assertTrue(((ShadowFloatingActionButton) ShadowExtractor
                .extract(activity.findViewById(R.id.reply_button))).isVisible());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

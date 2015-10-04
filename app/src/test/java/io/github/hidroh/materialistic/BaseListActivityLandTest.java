package io.github.hidroh.materialistic;

import android.support.v4.view.ViewPager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestWebItem;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

// TODO switch to API 21 once ShareActionProvider is fixed
@Config(qualifiers = "w820dp-land", sdk = 19, shadows = {ShadowSupportPreferenceManager.class})
@RunWith(RobolectricGradleTestRunner.class)
public class BaseListActivityLandTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create().start().resume().get();
        // inflate menu, see https://github.com/robolectric/robolectric/issues/1326
        ShadowLooper.pauseMainLooper();
        controller.visible();
        ShadowApplication.getInstance().getForegroundThreadScheduler().advanceToLastPostedRunnable();
    }

    @Test
    public void testCreateLand() {
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share).isVisible());
    }

    @Test
    public void testRotate() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getDisplayedTitle() {
                return "item title";
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        assertThat(activity).hasTitle("item title");

        RuntimeEnvironment.setQualifiers("");
        activity.onConfigurationChanged(RuntimeEnvironment.application.getResources().getConfiguration());
        assertThat(activity).hasTitle(activity.getString(R.string.title_activity_list));

        RuntimeEnvironment.setQualifiers("w820dp-land");
        activity.onConfigurationChanged(RuntimeEnvironment.application.getResources().getConfiguration());
        assertThat(activity).hasTitle("item title");
    }

    @Test
    public void testSelectItemOpenStory() {
        assertThat(activity.findViewById(R.id.empty)).isVisible();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getId() {
                return "1";
            }
        });
        assertThat(activity.findViewById(R.id.empty)).isNotVisible();
        assertStoryMode();
    }

    @Test
    public void testSelectItemOpenComment() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_comments))
                .commit();
        controller.pause().resume();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getId() {
                return "1";
            }
        });
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story).isVisible());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment).isVisible());
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(0); // comment is now default view
    }

    @Test
    public void testGetSelectedItem() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public boolean isShareable() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        assertNotNull(activity.getSelectedItem());
    }

    @Test
    public void testClearSelection() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public boolean isShareable() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        assertThat(activity.findViewById(R.id.empty)).isNotVisible();
        activity.clearSelection();
        assertThat(activity.findViewById(R.id.empty)).isVisible();
    }

    @Test
    public void testToggleItemView() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public boolean isShareable() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        });
        assertStoryMode();
        shadowOf(activity).clickMenuItem(R.id.menu_comment);
        assertCommentMode();
        shadowOf(activity).clickMenuItem(R.id.menu_story);
        assertStoryMode();
    }

    private void assertCommentMode() {
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story).isVisible());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment).isVisible());
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(1); // story is default view
    }

    private void assertStoryMode() {
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment).isVisible());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story).isVisible());
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(0); // story is default view
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

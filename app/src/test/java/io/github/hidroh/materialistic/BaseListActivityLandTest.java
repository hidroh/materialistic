package io.github.hidroh.materialistic;

import android.preference.PreferenceManager;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestWebItem;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@Config(qualifiers = "w820dp-land")
@RunWith(RobolectricTestRunner.class)
public class BaseListActivityLandTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create().start().resume().visible().get();
    }

    @Test
    public void testCreateLand() {
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share).isVisible());
    }

    @Test
    public void testSelectItemOpenStory() {
        assertThat(activity.findViewById(R.id.empty)).isVisible();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getId() {
                return "1";
            }
        }, new View(activity));
        assertThat(activity.findViewById(R.id.empty)).isNotVisible();
        assertThat(activity.getSupportFragmentManager()).hasFragmentWithTag(WebFragment.class.getName());
        assertThat(activity.getSupportFragmentManager()).hasFragmentWithTag(ItemFragment.class.getName());
        assertStoryMode();
    }

    @Test
    public void testSelectItemOpenComment() {
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_item_click), true)
                .commit();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getId() {
                return "1";
            }
        }, new View(activity));
        assertCommentMode();
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
        }, new View(activity));
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
        }, new View(activity));
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
        }, new View(activity));
        assertStoryMode();
        shadowOf(activity).clickMenuItem(R.id.menu_comment);
        assertCommentMode();
        shadowOf(activity).clickMenuItem(R.id.menu_story);
        assertStoryMode();
    }

    private void assertCommentMode() {
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story).isVisible());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment).isVisible());
        assertThat(activity.getSupportFragmentManager()
                .findFragmentByTag(ItemFragment.class.getName())).isVisible();
        assertThat(activity.getSupportFragmentManager()
                .findFragmentByTag(WebFragment.class.getName())).isNotVisible();
    }

    private void assertStoryMode() {
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment).isVisible());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story).isVisible());
        assertThat(activity.getSupportFragmentManager()
                .findFragmentByTag(WebFragment.class.getName())).isVisible();
        assertThat(activity.getSupportFragmentManager()
                .findFragmentByTag(ItemFragment.class.getName())).isNotVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

package io.github.hidroh.materialistic;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import io.github.hidroh.materialistic.test.ShadowRecyclerView;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestWebItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

@Config(qualifiers = "w820dp-land", shadows = {ShadowRecyclerView.class, ShadowSupportPreferenceManager.class})
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
        assertNull(activity.findViewById(R.id.reply_button));
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
        assertNull(activity.findViewById(R.id.reply_button));
    }

    @Test
    public void testSelectItemOpenStory() {
        assertThat(activity.findViewById(R.id.empty)).isVisible();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }
        });
        assertThat(activity.findViewById(R.id.empty)).isNotVisible();
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
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getId() {
                return "1";
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
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getId() {
                return "1";
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
        assertThat(activity.findViewById(R.id.empty)).isNotVisible();
        activity.clearSelection();
        assertThat(activity.findViewById(R.id.empty)).isVisible();
        assertNull(activity.findViewById(R.id.reply_button));
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
        RecyclerView itemRecyclerView = (RecyclerView) activity.findViewById(R.id.content)
                .findViewById(R.id.recycler_view);
        itemRecyclerView.smoothScrollToPosition(1);
        assertEquals(1, ((ShadowRecyclerView) ShadowExtractor.extract(itemRecyclerView))
                .getSmoothScrollToPosition());
        TabLayout tabLayout = (TabLayout) activity.findViewById(R.id.tab_layout);
        assertEquals(3, tabLayout.getTabCount());
        tabLayout.getTabAt(1).select();
        tabLayout.getTabAt(0).select();
        tabLayout.getTabAt(0).select();
        assertEquals(0, ((ShadowRecyclerView) ShadowExtractor.extract(itemRecyclerView))
                .getSmoothScrollToPosition());
    }

    private void assertCommentMode() {
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(0);
        assertNotNull(activity.findViewById(R.id.reply_button));
    }

    private void assertStoryMode() {
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(1);
        assertNull(activity.findViewById(R.id.reply_button));
    }

    private void assertReadabilityMode() {
        assertThat((ViewPager) activity.findViewById(R.id.content)).hasCurrentItem(2);
        assertNull(activity.findViewById(R.id.reply_button));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

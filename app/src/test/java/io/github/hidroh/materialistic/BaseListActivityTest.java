package io.github.hidroh.materialistic;

import android.preference.PreferenceManager;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestWebItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class BaseListActivityTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create().start().resume().visible().get();
    }

    @Test
    public void testCreate() {
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment));
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story));
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
    }

    @Test
    public void testRotate() {
        RuntimeEnvironment.setQualifiers("w820dp-land");
        activity.onConfigurationChanged(RuntimeEnvironment.application.getResources().getConfiguration());
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
    }

    @Test
    public void testSelectItemOpenWeb() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }
        }, new View(activity));
        assertEquals(WebActivity.class.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    @Test
    public void testSelectItemOpenItem() {
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
        assertEquals(ItemActivity.class.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
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
        assertNull(activity.getSelectedItem());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

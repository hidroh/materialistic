package io.github.hidroh.materialistic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.TestListActivity;

import static org.assertj.android.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@Config(shadows = {ShadowSupportPreferenceManager.class})
@RunWith(RobolectricGradleTestRunner.class)
public class DrawerFragmentLoginTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestListActivity.class)
                .create()
                .postCreate(null)
                .start()
                .resume()
                .visible();
        activity = controller.get();
    }

    @Test
    public void test() {
        TextView drawerAccount = (TextView) activity.findViewById(R.id.drawer_account);
        View drawerLogout = activity.findViewById(R.id.drawer_logout);
        assertThat(drawerAccount).hasText(R.string.login);
        assertThat(drawerAccount).isClickable();
        assertThat(drawerLogout).isNotVisible();
        Preferences.setUsername(activity, "username");
        assertThat(drawerAccount).hasText("username");
        assertThat(drawerAccount).isNotClickable();
        assertThat(drawerLogout).isVisible();
        drawerLogout.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertThat(drawerAccount).hasText(R.string.login);
        assertThat(drawerAccount).isClickable();
        assertThat(drawerLogout).isNotVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

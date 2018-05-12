package io.github.hidroh.materialistic;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAlertDialog;

import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowSupportDrawerLayout;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowSupportDrawerLayout.class})
@RunWith(TestRunner.class)
public class DrawerActivityLoginTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;
    private TextView drawerAccount;
    private View drawerLogout;
    private View drawerUser;

    @Before
    public void setUp() {
        Preferences.sReleaseNotesSeen = true;
        controller = Robolectric.buildActivity(TestListActivity.class)
                .create()
                .postCreate(null)
                .start()
                .resume()
                .visible();
        activity = controller.get();
        drawerAccount = activity.findViewById(R.id.drawer_account);
        drawerLogout = activity.findViewById(R.id.drawer_logout);
        drawerUser = activity.findViewById(R.id.drawer_user);
    }

    @Test
    public void testNoExistingAccount() {
        assertThat(drawerAccount).hasText(R.string.login);
        assertThat(drawerLogout).isNotVisible();
        assertThat(drawerUser).isNotVisible();
        Preferences.setUsername(activity, "username");
        assertThat(drawerAccount).hasText("username");
        assertThat(drawerLogout).isVisible();
        assertThat(drawerUser).isVisible();
        drawerLogout.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertThat(drawerAccount).hasText(R.string.login);
        assertThat(drawerLogout).isNotVisible();
    }

    @Test
    public void testOpenUserProfile() {
        Preferences.setUsername(activity, "username");
        drawerUser.performClick();
        ((ShadowSupportDrawerLayout) Shadow.extract(activity.findViewById(R.id.drawer_layout)))
                .getDrawerListeners().get(0)
                .onDrawerClosed(activity.findViewById(R.id.drawer));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, UserActivity.class)
                .hasExtra(UserActivity.EXTRA_USERNAME, "username");
    }

    @Test
    public void testExistingAccount() {
        AccountManager.get(activity).addAccountExplicitly(new Account("existing",
                BuildConfig.APPLICATION_ID), "password", null);
        drawerAccount.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertThat(alertDialog.getListView().getAdapter()).hasCount(1);
        shadowOf(alertDialog).clickOnItem(0);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertThat(alertDialog).isNotShowing();
        assertThat(drawerAccount).hasText("existing");
        assertThat(drawerLogout).isVisible();
        drawerAccount.performClick();
        alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertThat(alertDialog.getListView().getAdapter()).hasCount(1);
    }

    @Test
    public void testAddAccount() {
        AccountManager.get(activity).addAccountExplicitly(new Account("existing",
                BuildConfig.APPLICATION_ID), "password", null);
        drawerAccount.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertThat(alertDialog.getListView().getAdapter()).hasCount(1);
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        assertThat(alertDialog).isNotShowing();
        ((ShadowSupportDrawerLayout) Shadow.extract(activity.findViewById(R.id.drawer_layout)))
                .getDrawerListeners().get(0)
                .onDrawerClosed(activity.findViewById(R.id.drawer));
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, LoginActivity.class);
    }

    @Config(sdk = 21)
    @Test
    public void testRemoveAccount() {
        AccountManager.get(activity).addAccountExplicitly(new Account("existing",
                BuildConfig.APPLICATION_ID), "password", null);
        Preferences.setUsername(activity, "existing");
        drawerAccount.performClick();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertThat(alertDialog.getListView().getAdapter()).hasCount(1);
        alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).performClick();
        assertThat(alertDialog).isNotShowing();
        assertThat(AccountManager.get(activity).getAccounts()).isEmpty();
    }

    @Test
    public void testMoreToggle() {
        activity.findViewById(R.id.drawer_more).performClick();
        assertThat((View) activity.findViewById(R.id.drawer_more_container)).isVisible();
        activity.findViewById(R.id.drawer_more).performClick();
        assertThat((View) activity.findViewById(R.id.drawer_more_container)).isNotVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
        Preferences.sReleaseNotesSeen = null;
    }
}

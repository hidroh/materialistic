package io.github.hidroh.materialistic;

import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.EditText;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAccountManager;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import java.io.IOException;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.test.TestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(TestRunner.class)
public class LoginActivityTest {
    private ActivityController<LoginActivity> controller;
    private LoginActivity activity;
    @Inject UserServices userServices;
    @Captor ArgumentCaptor<UserServices.Callback> callback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(userServices);
        controller = Robolectric.buildActivity(LoginActivity.class);
        activity = controller.create().postCreate(null).start().resume().get();
    }

    @Test
    public void testEmptyLoginInput() {
        activity.findViewById(R.id.login_button).performClick();
        assertNotNull(((TextInputLayout) activity.findViewById(R.id.textinput_username)).getError());
        assertNotNull(((TextInputLayout) activity.findViewById(R.id.textinput_password)).getError());
    }

    @Test
    public void testEmptyRegisterInput() {
        activity.findViewById(R.id.register_button).performClick();
        assertNotNull(((TextInputLayout) activity.findViewById(R.id.textinput_username)).getError());
        assertNotNull(((TextInputLayout) activity.findViewById(R.id.textinput_password)).getError());
    }

    @Test
    public void testLoginSuccessful() {
        ((EditText) activity.findViewById(R.id.edittext_username)).setText("username");
        ((EditText) activity.findViewById(R.id.edittext_password)).setText("password");
        activity.findViewById(R.id.login_button).performClick();
        assertNull(((TextInputLayout) activity.findViewById(R.id.textinput_username)).getError());
        assertNull(((TextInputLayout) activity.findViewById(R.id.textinput_password)).getError());
        verify(userServices).login(eq("username"), eq("password"), eq(false), callback.capture());
        callback.getValue().onDone(true);
        assertThat(activity).isFinishing();
        assertEquals(activity.getString(R.string.welcome, "username"), ShadowToast.getTextOfLatestToast());
        assertThat(ShadowAccountManager.get(activity).getAccounts()).hasSize(1);
        assertEquals("username", Preferences.getUsername(activity));
    }

    @Test
    public void testRegisterFailed() {
        ((EditText) activity.findViewById(R.id.edittext_username)).setText("username");
        ((EditText) activity.findViewById(R.id.edittext_password)).setText("password");
        activity.findViewById(R.id.register_button).performClick();
        verify(userServices).login(eq("username"), eq("password"), eq(true), callback.capture());
        callback.getValue().onDone(false);
        assertThat(activity).isNotFinishing();
        assertEquals(activity.getString(R.string.login_failed), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testLoginError() {
        ((EditText) activity.findViewById(R.id.edittext_username)).setText("username");
        ((EditText) activity.findViewById(R.id.edittext_password)).setText("password");
        activity.findViewById(R.id.login_button).performClick();
        verify(userServices).login(eq("username"), eq("password"), eq(false), callback.capture());
        callback.getValue().onError(new IOException());
        assertThat(activity).isNotFinishing();
        assertEquals(activity.getString(R.string.login_failed), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testReLogin() {
        controller = Robolectric.buildActivity(LoginActivity.class);
        Preferences.setUsername(RuntimeEnvironment.application, "existing");
        activity = controller.create().postCreate(null).start().resume().get();
        assertThat(activity).hasTitle(R.string.re_enter_password);
        assertThat((View) activity.findViewById(R.id.register_button)).isNotVisible();
        assertThat((EditText) activity.findViewById(R.id.edittext_username))
                .hasTextString("existing");
    }

    @Test
    public void testAddAccount() {
        controller = Robolectric.buildActivity(LoginActivity.class);
        Preferences.setUsername(RuntimeEnvironment.application, "existing");
        Intent intent = new Intent();
        intent.putExtra(LoginActivity.EXTRA_ADD_ACCOUNT, true);
        activity = controller.withIntent(intent).create().postCreate(null).start().resume().get();
        assertThat(activity).hasTitle(R.string.title_activity_login);
        assertThat((View) activity.findViewById(R.id.register_button)).isVisible();
        assertThat((EditText) activity.findViewById(R.id.edittext_username)).isEmpty();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

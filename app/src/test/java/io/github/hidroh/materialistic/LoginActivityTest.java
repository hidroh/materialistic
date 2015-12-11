package io.github.hidroh.materialistic;

import android.support.design.widget.TextInputLayout;
import android.widget.EditText;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.shadows.ShadowAccountManager;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
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
    public void testEmptyInput() {
        activity.findViewById(R.id.login_button).performClick();
        assertNotNull(((TextInputLayout) activity.findViewById(R.id.textinput_username)).getError());
        assertNotNull(((TextInputLayout) activity.findViewById(R.id.textinput_password)).getError());
    }

    @Test
    public void testSuccessful() {
        ((EditText) activity.findViewById(R.id.edittext_username)).setText("username");
        ((EditText) activity.findViewById(R.id.edittext_password)).setText("password");
        activity.findViewById(R.id.login_button).performClick();
        assertNull(((TextInputLayout) activity.findViewById(R.id.textinput_username)).getError());
        assertNull(((TextInputLayout) activity.findViewById(R.id.textinput_password)).getError());
        verify(userServices).login(eq("username"), eq("password"), callback.capture());
        callback.getValue().onDone(true);
        assertThat(activity).isFinishing();
        assertEquals(activity.getString(R.string.welcome, "username"), ShadowToast.getTextOfLatestToast());
        assertThat(ShadowAccountManager.get(activity).getAccounts()).hasSize(1);
    }

    @Test
    public void testFailed() {
        ((EditText) activity.findViewById(R.id.edittext_username)).setText("username");
        ((EditText) activity.findViewById(R.id.edittext_password)).setText("password");
        activity.findViewById(R.id.login_button).performClick();
        verify(userServices).login(eq("username"), eq("password"), callback.capture());
        callback.getValue().onDone(false);
        assertThat(activity).isNotFinishing();
        assertEquals(activity.getString(R.string.login_failed), ShadowToast.getTextOfLatestToast());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

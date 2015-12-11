package io.github.hidroh.materialistic;

import android.support.design.widget.TextInputLayout;
import android.widget.EditText;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.util.ActivityController;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricGradleTestRunner.class)
public class LoginActivityTest {
    private ActivityController<LoginActivity> controller;
    private LoginActivity activity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
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
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

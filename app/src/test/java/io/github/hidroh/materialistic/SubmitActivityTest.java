package io.github.hidroh.materialistic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;

import static org.assertj.android.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
public class SubmitActivityTest {
    private ActivityController<SubmitActivity> controller;
    private SubmitActivity activity;
    @Inject UserServices userServices;
    @Captor ArgumentCaptor<UserServices.Callback> submitCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(userServices);
        controller = Robolectric.buildActivity(SubmitActivity.class);
        activity = controller.create().start().resume().visible().get();
    }

    @Test
    public void testOnBackPressed() {
        shadowOf(activity).clickMenuItem(android.R.id.home);
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertThat(activity).isFinishing();
    }

    @Test
    public void testMissingInput() {
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        ((EditText) activity.findViewById(R.id.edittext_title)).setText(null);
        ((EditText) activity.findViewById(R.id.edittext_content)).setText("content");
        verify(userServices, never()).submit(any(Context.class), anyString(), anyString(),
                anyBoolean(), any(UserServices.Callback.class));
        assertThat(activity).isNotFinishing();
    }

    @Test
    public void testSubmitText() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_content)).setText("content");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertEquals(activity.getString(R.string.confirm_submit_question),
                shadowOf(alertDialog).getMessage());
    }

    @Test
    public void testSubmitUrl() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_content)).setText("http://example.com");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertEquals(activity.getString(R.string.confirm_submit_url),
                shadowOf(alertDialog).getMessage());
    }

    @Test
    public void testGuidelines() {
        shadowOf(activity).clickMenuItem(R.id.menu_guidelines);
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void testSubmitError() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_content)).setText("http://example.com");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)
                .performClick();
        verify(userServices).submit(any(Context.class), eq("title"), eq("http://example.com"),
                eq(true), submitCallback.capture());
        submitCallback.getValue().onError();
        assertEquals(activity.getString(R.string.submit_failed), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testSubmitFailed() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_content)).setText("http://example.com");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)
                .performClick();
        verify(userServices).submit(any(Context.class), eq("title"), eq("http://example.com"),
                eq(true), submitCallback.capture());
        submitCallback.getValue().onDone(false);
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, LoginActivity.class);
    }

    @Test
    public void testSubmitSuccessful() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_content)).setText("http://example.com");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)
                .performClick();
        verify(userServices).submit(any(Context.class), eq("title"), eq("http://example.com"),
                eq(true), submitCallback.capture());
        submitCallback.getValue().onDone(true);
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, NewActivity.class);
        assertThat(activity).isFinishing();
    }

    @Test
    public void testSubmitDelayedError() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_content)).setText("http://example.com");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)
                .performClick();
        verify(userServices).submit(any(Context.class), eq("title"), eq("http://example.com"),
                eq(true), submitCallback.capture());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)
                .performClick();
        submitCallback.getValue().onError();
        assertEquals(activity.getString(R.string.submit_failed), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testSubmitDelayedFailed() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_content)).setText("http://example.com");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)
                .performClick();
        verify(userServices).submit(any(Context.class), eq("title"), eq("http://example.com"),
                eq(true), submitCallback.capture());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)
                .performClick();
        submitCallback.getValue().onDone(false);
        assertNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testSubmitDelayedSuccessful() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_content)).setText("http://example.com");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)
                .performClick();
        verify(userServices).submit(any(Context.class), eq("title"), eq("http://example.com"),
                eq(true), submitCallback.capture());
        shadowOf(activity).clickMenuItem(android.R.id.home);
        ShadowAlertDialog.getLatestAlertDialog().getButton(DialogInterface.BUTTON_POSITIVE)
                .performClick();
        submitCallback.getValue().onDone(true);
        assertNull(shadowOf(activity).getNextStartedActivity());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

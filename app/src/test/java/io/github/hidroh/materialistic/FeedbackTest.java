package io.github.hidroh.materialistic;

import android.app.Dialog;
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
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FeedbackClient;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
public class FeedbackTest {
    private ActivityController<AboutActivity> controller;
    private AboutActivity activity;
    @Inject FeedbackClient feedbackClient;
    @Captor ArgumentCaptor<FeedbackClient.Callback> callback;
    private Dialog dialog;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(feedbackClient);
        controller = Robolectric.buildActivity(AboutActivity.class);
        activity = controller.create().postCreate(null).start().resume().get();
        activity.findViewById(R.id.drawer_feedback).performClick();
        dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);
    }

    @Test
    public void testEmptyInput() {
        dialog.findViewById(R.id.feedback_button).performClick();
        assertNotNull(((TextInputLayout) dialog.findViewById(R.id.textinput_title)).getError());
        assertNotNull(((TextInputLayout) dialog.findViewById(R.id.textinput_body)).getError());
        verify(feedbackClient, never()).send(anyString(), anyString(), any(FeedbackClient.Callback.class));
        assertThat(dialog).isShowing();
        controller.pause().stop().destroy();
    }

    @Test
    public void testSuccessful() {
        ((EditText) dialog.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) dialog.findViewById(R.id.edittext_body)).setText("body");
        dialog.findViewById(R.id.feedback_button).performClick();
        verify(feedbackClient).send(eq("title"), eq("body"), callback.capture());
        callback.getValue().onSent(true);
        assertThat(dialog).isNotShowing();
        assertEquals(activity.getString(R.string.feedback_sent), ShadowToast.getTextOfLatestToast());
        controller.pause().stop().destroy();
    }

    @Test
    public void testFailed() {
        ((EditText) dialog.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) dialog.findViewById(R.id.edittext_body)).setText("body");
        dialog.findViewById(R.id.feedback_button).performClick();
        verify(feedbackClient).send(eq("title"), eq("body"), callback.capture());
        callback.getValue().onSent(false);
        assertThat(dialog).isShowing();
        assertEquals(activity.getString(R.string.feedback_failed), ShadowToast.getTextOfLatestToast());
        controller.pause().stop().destroy();
    }

    @Test
    public void testDismissBeforeResult() {
        ((EditText) dialog.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) dialog.findViewById(R.id.edittext_body)).setText("body");
        dialog.findViewById(R.id.feedback_button).performClick();
        verify(feedbackClient).send(eq("title"), eq("body"), callback.capture());
        dialog.dismiss();
        callback.getValue().onSent(true);
        controller.pause().stop().destroy();
    }

    @Test
    public void testFinishBeforeResult() {
        ((EditText) dialog.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) dialog.findViewById(R.id.edittext_body)).setText("body");
        dialog.findViewById(R.id.feedback_button).performClick();
        verify(feedbackClient).send(eq("title"), eq("body"), callback.capture());
        controller.pause().stop().destroy();
        callback.getValue().onSent(true);
    }

    @After
    public void tearDown() {
        reset(feedbackClient);
    }
}

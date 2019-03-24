package io.github.hidroh.materialistic;

import android.annotation.SuppressLint;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.EditText;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import io.github.hidroh.materialistic.test.TestRunner;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.android.controller.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FeedbackClient;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@SuppressLint("SetTextI18n")
@RunWith(TestRunner.class)
public class FeedbackActivityTest {
    private ActivityController<FeedbackActivity> controller;
    private FeedbackActivity activity;
    @Inject FeedbackClient feedbackClient;
    @Captor ArgumentCaptor<FeedbackClient.Callback> callback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(feedbackClient);
        controller = Robolectric.buildActivity(FeedbackActivity.class);
        activity = controller.create().start().resume().get();
    }

    @Test
    public void testEmptyInput() {
        activity.findViewById(R.id.feedback_button).performClick();
        assertNotNull(((TextInputLayout) activity.findViewById(R.id.textinput_title)).getError());
        assertNotNull(((TextInputLayout) activity.findViewById(R.id.textinput_body)).getError());
        verify(feedbackClient, never()).send(any(), any(), any(FeedbackClient.Callback.class));
        assertThat(activity).isNotFinishing();
        controller.pause().stop().destroy();
    }

    @Test
    public void testSuccessful() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_body)).setText("body");
        activity.findViewById(R.id.feedback_button).performClick();
        verify(feedbackClient).send(eq("title"), eq("body"), callback.capture());
        callback.getValue().onSent(true);
        assertThat(activity).isFinishing();
        assertEquals(activity.getString(R.string.feedback_sent), ShadowToast.getTextOfLatestToast());
        controller.pause().stop().destroy();
    }

    @Test
    public void testFailed() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_body)).setText("body");
        activity.findViewById(R.id.feedback_button).performClick();
        verify(feedbackClient).send(eq("title"), eq("body"), callback.capture());
        callback.getValue().onSent(false);
        assertThat(activity).isNotFinishing();
        assertEquals(activity.getString(R.string.feedback_failed), ShadowToast.getTextOfLatestToast());
        controller.pause().stop().destroy();
    }

    @Test
    public void testDismissBeforeResult() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_body)).setText("body");
        activity.findViewById(R.id.feedback_button).performClick();
        verify(feedbackClient).send(eq("title"), eq("body"), callback.capture());
        activity.finish();
        callback.getValue().onSent(true);
        controller.pause().stop().destroy();
    }

    @Test
    public void testFinishBeforeResult() {
        ((EditText) activity.findViewById(R.id.edittext_title)).setText("title");
        ((EditText) activity.findViewById(R.id.edittext_body)).setText("body");
        activity.findViewById(R.id.feedback_button).performClick();
        verify(feedbackClient).send(eq("title"), eq("body"), callback.capture());
        controller.pause().stop().destroy();
        callback.getValue().onSent(true);
    }

    @Test
    public void testRate() {
        activity.findViewById(R.id.button_rate).performClick();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
        assertThat(activity).isFinishing();
    }

    @After
    public void tearDown() {
        reset(feedbackClient);
    }
}

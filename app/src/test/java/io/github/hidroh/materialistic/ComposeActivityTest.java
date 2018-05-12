package io.github.hidroh.materialistic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import java.io.IOException;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.test.TestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunner.class)
public class ComposeActivityTest {
    private ActivityController<ComposeActivity> controller;
    private ComposeActivity activity;
    @Inject UserServices userServices;
    @Captor ArgumentCaptor<UserServices.Callback> replyCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(userServices);
        Intent intent = new Intent();
        intent.putExtra(ComposeActivity.EXTRA_PARENT_ID, "1");
        intent.putExtra(ComposeActivity.EXTRA_PARENT_TEXT, "Paragraph 1<br/><br/>Paragraph 2<br/>");
        controller = Robolectric.buildActivity(ComposeActivity.class, intent);
        controller.create().start().resume().visible();
        activity = controller.get();
    }

    @Test
    public void testNoId() {
        controller = Robolectric.buildActivity(ComposeActivity.class)
                .create().start().resume().visible();
        activity = controller.get();
        assertThat(activity).isFinishing();
    }

    @Test
    public void testToggle() {
        assertThat((View) activity.findViewById(R.id.toggle)).isVisible();
        assertThat((View) activity.findViewById(R.id.text)).isNotVisible();
        activity.findViewById(R.id.toggle).performClick();
        assertThat((View) activity.findViewById(R.id.text)).isVisible();
        assertThat((TextView) activity.findViewById(R.id.text))
                .hasTextString("Paragraph 1\n\nParagraph 2");
        activity.findViewById(R.id.toggle).performClick();
        assertThat((View) activity.findViewById(R.id.text)).isNotVisible();
    }

    @Test
    public void testHomeButtonClick() {
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertThat(activity).isFinishing();
    }

    @Test
    public void testExitSaveDraft() {
        ((EditText) activity.findViewById(R.id.edittext_body)).setText("Reply");
        shadowOf(activity).clickMenuItem(android.R.id.home);
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertThat(activity).isFinishing();
        assertThat(Preferences.getDraft(activity, "1")).contains("Reply");
    }

    @Test
    public void testExitDiscardDraft() {
        ((EditText) activity.findViewById(R.id.edittext_body)).setText("Reply");
        shadowOf(activity).clickMenuItem(android.R.id.home);
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        assertThat(activity).isFinishing();
        assertThat(Preferences.getDraft(activity, "1")).isNullOrEmpty();
    }

    @Test
    public void testSendEmpty() {
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        assertEquals(activity.getString(R.string.comment_required), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testQuote() {
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_quote).isVisible());
        ((EditText) activity.findViewById(R.id.edittext_body)).setText("Reply");
        shadowOf(activity).clickMenuItem(R.id.menu_quote);
        assertThat((EditText) activity.findViewById(R.id.edittext_body))
                .hasTextString("> Paragraph 1\n\n> Paragraph 2\n\nReply");
    }

    @Test
    public void testSaveDiscardDraft() {
        ((EditText) activity.findViewById(R.id.edittext_body)).setText("Reply");
        shadowOf(activity).clickMenuItem(R.id.menu_save_draft);
        assertThat(Preferences.getDraft(activity, "1")).contains("Reply");
        shadowOf(activity).clickMenuItem(R.id.menu_discard_draft);
        assertThat(Preferences.getDraft(activity, "1")).isNullOrEmpty();
    }
    @Test
    public void testClickEmptyFocusEditText() {
        View editText = activity.findViewById(R.id.edittext_body);
        editText.clearFocus();
        assertThat(editText).isNotFocused();
        activity.findViewById(R.id.empty).performClick();
        assertThat(editText).isFocused();
    }

    @Test
    public void testGuidelines() {
        shadowOf(activity).clickMenuItem(R.id.menu_guidelines);
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void testEmptyQuote() {
        Intent intent = new Intent();
        intent.putExtra(ComposeActivity.EXTRA_PARENT_ID, "1");
        controller = Robolectric.buildActivity(ComposeActivity.class, intent)
                .create().start().resume().visible();
        activity = controller.get();
        assertThat((View) activity.findViewById(R.id.quote)).isNotVisible();
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_quote).isVisible());
    }

    @Test
    public void testSendPromptToLogin() {
        doSend();
        replyCallback.getValue().onDone(false);
        assertThat(shadowOf(activity).getNextStartedActivity())
                .hasComponent(activity, LoginActivity.class);
    }

    @Test
    public void testSendSuccessful() {
        doSend();
        assertThat(Preferences.getDraft(activity, "1")).isNotEmpty();
        replyCallback.getValue().onDone(true);
        assertThat(activity).isFinishing();
        assertThat(Preferences.getDraft(activity, "1")).isNullOrEmpty();
    }

    @Test
    public void testSendFailed() {
        doSend();
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_send).isEnabled());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_quote).isVisible());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_save_draft).isEnabled());
        assertFalse(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_discard_draft).isEnabled());
        replyCallback.getValue().onError(new IOException());
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_send).isEnabled());
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_quote).isVisible());
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_save_draft).isEnabled());
        assertTrue(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_discard_draft).isEnabled());
        assertThat(activity).isNotFinishing();
        assertEquals(activity.getString(R.string.comment_failed), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testDelayedSuccessfulResponse() {
        doSend();
        shadowOf(activity).clickMenuItem(android.R.id.home);
        assertThat(activity).isFinishing();
        replyCallback.getValue().onDone(true);
        assertEquals(activity.getString(R.string.comment_successful), ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testDelayedFailedResponse() {
        doSend();
        shadowOf(activity).clickMenuItem(android.R.id.home);
        replyCallback.getValue().onDone(false);
        assertNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testDelayedError() {
        doSend();
        shadowOf(activity).clickMenuItem(android.R.id.home);
        replyCallback.getValue().onError(new IOException());
        assertEquals(activity.getString(R.string.comment_failed), ShadowToast.getTextOfLatestToast());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    private void doSend() {
        ((EditText) activity.findViewById(R.id.edittext_body)).setText("Reply");
        shadowOf(activity).clickMenuItem(R.id.menu_send);
        verify(userServices).reply(any(Context.class), eq("1"), eq("Reply"), replyCallback.capture());
    }
}

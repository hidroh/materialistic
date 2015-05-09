package io.github.hidroh.materialistic;

import android.app.Activity;
import android.content.Intent;
import android.view.MotionEvent;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class AppUtilsTest {
    @Test
    public void testMakeShareIntent() {
        Intent actual = AppUtils.makeShareIntent("content");
        assertThat(actual).hasAction(Intent.ACTION_SEND);
        assertThat(actual).hasType("text/plain");
        assertThat(actual).hasExtra(Intent.EXTRA_TEXT);
    }

    @Test
    public void testSetTextWithLinks() {
        TextView textView = new TextView(RuntimeEnvironment.application);
        AppUtils.setTextWithLinks(textView, "<a href=\\\"http://www.justin.tv/problems/bml\\\" rel=\\\"nofollow\\\">http://www.justin.tv/problems/bml</a>");
        MotionEvent event = mock(MotionEvent.class);
        when(event.getAction()).thenReturn(MotionEvent.ACTION_DOWN);
        when(event.getX()).thenReturn(0f);
        when(event.getY()).thenReturn(0f);
        assertTrue(shadowOf(textView).getOnTouchListener().onTouch(textView, event));
        when(event.getAction()).thenReturn(MotionEvent.ACTION_UP);
        when(event.getX()).thenReturn(0f);
        when(event.getY()).thenReturn(0f);
        assertTrue(shadowOf(textView).getOnTouchListener().onTouch(textView, event));
        assertNotNull(ShadowApplication.getInstance().getNextStartedActivity());
    }

    @Test
    public void testDefaultTextSize() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        shadowOf(activity.getTheme()).setTo(activity.getResources().newTheme());
        Preferences.applyTheme(activity);
        assertEquals(R.style.AppTextSize, shadowOf(activity.getTheme()).getStyleResourceId());
    }
}

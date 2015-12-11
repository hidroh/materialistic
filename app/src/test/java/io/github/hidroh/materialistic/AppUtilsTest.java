package io.github.hidroh.materialistic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.MotionEvent;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowNetworkInfo;

import io.github.hidroh.materialistic.test.TestItemActivity;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
public class AppUtilsTest {
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
        Preferences.Theme.apply(activity, false);
        assertEquals(R.style.AppTextSize, shadowOf(activity.getTheme()).getStyleResourceId());
    }

    @Test
    public void testGetAbbreviatedTimeSpan() {
        assertEquals("0m", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() +
                DateUtils.SECOND_IN_MILLIS));
        assertEquals("0m", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis()));
        assertEquals("5m", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                5 * DateUtils.MINUTE_IN_MILLIS - 10 * DateUtils.SECOND_IN_MILLIS));
        assertEquals("1h", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                DateUtils.HOUR_IN_MILLIS - DateUtils.MINUTE_IN_MILLIS));
        assertEquals("6d", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                DateUtils.WEEK_IN_MILLIS + DateUtils.MINUTE_IN_MILLIS));
        assertEquals("1w", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                DateUtils.WEEK_IN_MILLIS - DateUtils.MINUTE_IN_MILLIS));
        assertEquals("10y", AppUtils.getAbbreviatedTimeSpan(System.currentTimeMillis() -
                10 * DateUtils.YEAR_IN_MILLIS - DateUtils.MINUTE_IN_MILLIS));
    }

    @Test
    public void testShareBroadcastReceiver() {
        TestItemActivity activity = Robolectric.buildActivity(TestItemActivity.class).create().get();
        Intent intent = new Intent();
        intent.setData(Uri.parse("http://example.com"));
        new AppUtils.ShareBroadcastReceiver().onReceive(activity, intent);
        Intent actual = shadowOf(activity).getNextStartedActivity();
        assertThat(actual)
                .isNotNull()
                .hasAction(Intent.ACTION_CHOOSER);
    }

    @Test
    public void testNoActiveNetwork() {
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE)).setActiveNetworkInfo(null);
        assertFalse(AppUtils.isOnWiFi(RuntimeEnvironment.application));
    }

    @Test
    public void testDisconnectedNetwork() {
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null, 0, 0, false, false));
        assertFalse(AppUtils.isOnWiFi(RuntimeEnvironment.application));
    }

    @Test
    public void testNonWiFiNetwork() {
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_MOBILE, 0, true, true));
        assertFalse(AppUtils.isOnWiFi(RuntimeEnvironment.application));
    }

    @Test
    public void testWiFiNetwork() {
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        assertTrue(AppUtils.isOnWiFi(RuntimeEnvironment.application));
    }
}

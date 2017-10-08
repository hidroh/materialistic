package io.github.hidroh.materialistic;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAccountManager;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.TestHnItem;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.widget.PopupMenu;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.support.v4.Shadows.shadowOf;

@RunWith(TestRunner.class)
public class AppUtilsTest {
    @Inject AlertDialogBuilder alertDialogBuilder;

    @Test
    public void testSetTextWithLinks() {
        TestApplication.addResolver(new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")));
        Preferences.set(RuntimeEnvironment.application, R.string.pref_custom_tab, false);
        TextView textView = new TextView(RuntimeEnvironment.application);
        AppUtils.setTextWithLinks(textView, AppUtils.fromHtml("<a href=\"http://example.com\">http://example.com</a>"));
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
    public void testSetTextWithLinksOpenChromeCustomTabs() {
        TestApplication.addResolver(new Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")));
        TextView textView = new TextView(new ContextThemeWrapper(RuntimeEnvironment.application, R.style.AppTheme));
        AppUtils.setTextWithLinks(textView, AppUtils.fromHtml("<a href=\"http://example.com\">http://example.com</a>"));
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
        float expected = activity.getTheme().obtainStyledAttributes(R.style.AppTextSize,
                new int[]{R.attr.contentTextSize}).getDimension(0, 0);
        float actual = activity.getTheme().obtainStyledAttributes(
                new int[]{R.attr.contentTextSize}).getDimension(0, 0);
        assertThat(actual).isEqualTo(expected);
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

    @Test
    public void testRemoveAccount() {
        Preferences.setUsername(RuntimeEnvironment.application, "olduser");
        AppUtils.registerAccountsUpdatedListener(RuntimeEnvironment.application);
        shadowOf(AccountManager.get(RuntimeEnvironment.application)).addAccount(
                new Account("newuser", BuildConfig.APPLICATION_ID));
        assertNull(Preferences.getUsername(RuntimeEnvironment.application));
        Preferences.setUsername(RuntimeEnvironment.application, "newuser");
        shadowOf(AccountManager.get(RuntimeEnvironment.application)).addAccount(
                new Account("olduser", BuildConfig.APPLICATION_ID));
        assertEquals("newuser", Preferences.getUsername(RuntimeEnvironment.application));
    }

    @Test
    public void testShareComment() {
        AppUtils.share(RuntimeEnvironment.application, mock(PopupMenu.class),
                new View(RuntimeEnvironment.application), new TestHnItem(1));
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        AppUtils.share(RuntimeEnvironment.application, mock(PopupMenu.class),
                new View(RuntimeEnvironment.application), new TestHnItem(1) {
                    @Override
                    public String getUrl() {
                        return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
                    }
                });
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void testOpenExternalComment() {
        ActivityController<TestListActivity> controller = Robolectric.buildActivity(TestListActivity.class);
        TestListActivity activity = controller.create().start().resume().get();
        AppUtils.openExternal(activity, mock(PopupMenu.class),
                new View(activity), new TestHnItem(1), null);
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        AppUtils.openExternal(activity, mock(PopupMenu.class),
                new View(activity), new TestHnItem(1) {
                    @Override
                    public String getUrl() {
                        return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
                    }
                }, null);
        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        controller.destroy();
    }

    @Test
    public void testLoginNoAccounts() {
        AppUtils.showLogin(RuntimeEnvironment.application, null);
        assertThat(shadowOf(RuntimeEnvironment.application).getNextStartedActivity())
                .hasComponent(RuntimeEnvironment.application, LoginActivity.class);
    }

    @Test
    public void testLoginStaleAccount() {
        Preferences.setUsername(RuntimeEnvironment.application, "username");
        shadowOf(ShadowAccountManager.get(RuntimeEnvironment.application))
                .addAccount(new Account("username", BuildConfig.APPLICATION_ID));
        AppUtils.showLogin(RuntimeEnvironment.application, null);
        assertThat(shadowOf(RuntimeEnvironment.application).getNextStartedActivity())
                .hasComponent(RuntimeEnvironment.application, LoginActivity.class);
    }

    @Test
    public void testLoginShowChooser() {
        TestApplication.applicationGraph.inject(this);
        shadowOf(ShadowAccountManager.get(RuntimeEnvironment.application))
                .addAccount(new Account("username", BuildConfig.APPLICATION_ID));
        AppUtils.showLogin(RuntimeEnvironment.application, alertDialogBuilder);
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void testTrimHtmlWhitespaces() {
        TextView textView = new TextView(RuntimeEnvironment.application);
        textView.setText(AppUtils.fromHtml("<p>paragraph</p><p><br/><br/><br/></p>"));
        assertThat(textView).hasTextString("paragraph");
        textView.setText(AppUtils.fromHtml(""));
        assertThat(textView).hasTextString("");
        textView.setText(AppUtils.fromHtml("paragraph"));
        assertThat(textView).hasTextString("paragraph");
    }

    @Test
    public void testOpenExternalUrlNoConnection() {
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(null);
        AppUtils.openWebUrlExternal(RuntimeEnvironment.application, new TestHnItem(1L) {
            @Override
            public String getUrl() {
                return "http://example.com";
            }
        }, "http://example.com", null);
        assertThat(shadowOf(RuntimeEnvironment.application).getNextStartedActivity())
                .hasComponent(RuntimeEnvironment.application, OfflineWebActivity.class)
                .hasExtra(OfflineWebActivity.EXTRA_URL, "http://example.com");
    }

    @Test
    public void testFullscreenButton() {
        ActivityController<TestListActivity> controller = Robolectric.buildActivity(TestListActivity.class);
        TestListActivity activity = controller.create().start().resume().get();
        FloatingActionButton fab = new FloatingActionButton(activity);
        AppUtils.toggleFabAction(fab, null, false);
        fab.performClick();
        assertThat(shadowOf(LocalBroadcastManager.getInstance(activity)).getSentBroadcastIntents()).isNotEmpty();
        controller.destroy();
    }

    @Test
    public void testNavigate() {
        AppBarLayout appBar = mock(AppBarLayout.class);
        when(appBar.getBottom()).thenReturn(1);
        Navigable navigable = mock(Navigable.class);
        AppUtils.navigate(Navigable.DIRECTION_DOWN, appBar, navigable);
        verify(appBar).setExpanded(eq(false), anyBoolean());
        verify(navigable, never()).onNavigate(anyInt());

        when(appBar.getBottom()).thenReturn(0);
        AppUtils.navigate(Navigable.DIRECTION_DOWN, appBar, navigable);
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_DOWN));

        AppUtils.navigate(Navigable.DIRECTION_RIGHT, appBar, navigable);
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_RIGHT));

        AppUtils.navigate(Navigable.DIRECTION_UP, appBar, navigable);
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_UP));

        AppUtils.navigate(Navigable.DIRECTION_LEFT, appBar, navigable);
        verify(navigable).onNavigate(eq(Navigable.DIRECTION_LEFT));
    }
}

package io.github.hidroh.materialistic;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenu;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowResolveInfo;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.ShadowWebView;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@Config(emulateSdk = 21, reportSdk = 21, shadows = {ShadowWebView.class})
@RunWith(RobolectricTestRunner.class)
public class WebActivityTest {
    private WebActivity activity;
    private ActivityController<WebActivity> controller;
    private ItemManager.WebItem item;

    @Before
    public void setUp() {
        item = mock(ItemManager.WebItem.class);
        Intent intent = new Intent();
        intent.putExtra(WebActivity.EXTRA_ITEM, item);
        controller = Robolectric.buildActivity(WebActivity.class);
        activity = controller.withIntent(intent).create().start().resume().get();
    }

    @Test
    public void testCreateOptionsMenu() {
        final ShareActionProvider actionProvider = mock(ShareActionProvider.class);
        activity.onCreateOptionsMenu(new RoboMenu() {
            @Override
            public MenuItem findItem(int id) {
                if (id == R.id.menu_share) {
                    SupportMenuItem menuItem = mock(SupportMenuItem.class);
                    when(menuItem.getItemId()).thenReturn(id);
                    when(menuItem.getSupportActionProvider()).thenReturn(actionProvider);
                    return menuItem;
                }
                return new RoboMenuItem(id);
            }
        });
        verify(actionProvider).setShareIntent(any(Intent.class));
    }

    @Test
    public void testPrepareOptionsMenu() {
        final MenuItem menuItem = new RoboMenuItem();
        Menu menu = new RoboMenu() {
            @Override
            public MenuItem findItem(int id) {
                if (id == R.id.menu_share) {
                    return menuItem;
                }
                return super.findItem(id);
            }
        };
        menuItem.setVisible(true);
        when(item.isShareable()).thenReturn(true);
        activity.onPrepareOptionsMenu(menu);
        assertThat(menuItem).isVisible();

        menuItem.setVisible(true);
        when(item.isShareable()).thenReturn(false);
        activity.onPrepareOptionsMenu(menu);
        assertThat(menuItem).isNotVisible();
    }

    @Test
    public void testOptionsItemSelectedExternal() {
        RobolectricPackageManager packageManager = (RobolectricPackageManager)
                RuntimeEnvironment.application.getPackageManager();
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse("http://hidroh.github.io")),
                ShadowResolveInfo.newResolveInfo("label", activity.getPackageName(), WebActivity.class.getName()));
        when(item.getUrl()).thenReturn("http://hidroh.github.io");
        activity.onOptionsItemSelected(new RoboMenuItem(R.id.menu_external));
        Intent actual = shadowOf(activity).getNextStartedActivity();
        assertThat(actual).hasAction(Intent.ACTION_VIEW);
    }

    @Test
    public void testOptionsItemSelectedComment() {
        activity.onOptionsItemSelected(new RoboMenuItem(R.id.menu_comment));
        Intent actual = shadowOf(activity).getNextStartedActivity();
        Assertions.assertThat(actual.getComponent().getClassName()).isEqualTo(ItemActivity.class.getName());
    }

    @Test
    public void testProgressChanged() {
        ProgressBar progressBar = (ProgressBar) activity.findViewById(R.id.progress);
        WebView webView = (WebView) activity.findViewById(R.id.web_view);
        shadowOf(webView).getWebChromeClient().onProgressChanged(webView, 50);
        assertThat(progressBar).isVisible();
        shadowOf(webView).getWebChromeClient().onProgressChanged(webView, 100);
        assertThat(progressBar).isNotVisible();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

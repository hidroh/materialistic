package io.github.hidroh.materialistic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenu;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowResolveInfo;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.ShadowWebView;

import static junit.framework.Assert.assertNotNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowWebView.class})
@RunWith(RobolectricGradleTestRunner.class)
public class WebActivityTest {
    private WebActivity activity;
    private ActivityController<WebActivity> controller;
    private ItemManager.WebItem item;
    @Inject FavoriteManager favoriteManager;
    @Captor ArgumentCaptor<FavoriteManager.OperationCallbacks> callbacks;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(favoriteManager);
        item = mock(ItemManager.WebItem.class);
        when(item.getType()).thenReturn(ItemManager.WebItem.Type.story);
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
    public void testOptionsMenuFavorite() {
        final MenuItem menuFavorite = new RoboMenuItem(){
            @Override
            public int getItemId() {
                return R.id.menu_favorite;
            }

            @Override
            public MenuItem setTitle(int title) {
                setTitle(activity.getString(title));
                return this;
            }
        };
        Menu menu = new RoboMenu() {
            @Override
            public MenuItem findItem(int id) {
                if (id == R.id.menu_favorite) {
                    return menuFavorite;
                }
                return super.findItem(id);
            }
        };
        when(item.isShareable()).thenReturn(true);
        activity.onPrepareOptionsMenu(menu);
        verify(favoriteManager).check(any(Context.class), anyString(), callbacks.capture());
        callbacks.getValue().onCheckComplete(true);
        assertThat(menuFavorite).isVisible().hasTitle(activity.getString(R.string.unsave_story));

        activity.onOptionsItemSelected(menuFavorite);
        verify(favoriteManager).remove(any(Context.class), anyString());
        assertThat(menuFavorite).isVisible().hasTitle(activity.getString(R.string.save_story));

        activity.onOptionsItemSelected(menuFavorite);
        verify(favoriteManager).add(any(Context.class), any(ItemManager.WebItem.class));
        assertThat(menuFavorite).isVisible().hasTitle(activity.getString(R.string.unsave_story));
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

    @Test
    public void testDownloadPDF() {
        ResolveInfo resolverInfo = new ResolveInfo();
        resolverInfo.activityInfo = new ActivityInfo();
        resolverInfo.activityInfo.applicationInfo = new ApplicationInfo();
        resolverInfo.activityInfo.applicationInfo.packageName =
                ListActivity.class.getPackage().getName();
        resolverInfo.activityInfo.name = ListActivity.class.getName();
        RobolectricPackageManager rpm = (RobolectricPackageManager) RuntimeEnvironment.application.getPackageManager();
        rpm.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://example.com/file.pdf")), resolverInfo);

        WebView webView = (WebView) activity.findViewById(R.id.web_view);
        ShadowWebView shadowWebView = (ShadowWebView) ShadowExtractor.extract(webView);
        shadowWebView.getDownloadListener().onDownloadStart("http://example.com/file.pdf", "", "", "", 0l);
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

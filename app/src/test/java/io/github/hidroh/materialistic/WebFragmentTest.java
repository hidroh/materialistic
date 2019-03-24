package io.github.hidroh.materialistic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.widget.NestedScrollView;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.FileDownloader;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.WebActivity;
import io.github.hidroh.materialistic.test.shadow.ShadowNestedScrollView;
import io.github.hidroh.materialistic.test.shadow.ShadowWebView;
import okio.Okio;

import static io.github.hidroh.materialistic.WebFragment.PDF_LOADER_URL;
import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowWebView.class})
@RunWith(TestRunner.class)
public class WebFragmentTest {
    private WebActivity activity;
    private ActivityController<WebActivity> controller;
    private WebItem item;
    @Inject FavoriteManager favoriteManager;
    @Inject ReadabilityClient readabilityClient;
    private Intent intent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(favoriteManager);
        item = mock(WebItem.class);
        when(item.getType()).thenReturn(Item.STORY_TYPE);
        when(item.getUrl()).thenReturn("http://example.com");
        intent = new Intent();
        intent.putExtra(WebActivity.EXTRA_ITEM, item);
        controller = Robolectric.buildActivity(WebActivity.class, intent);
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, NetworkInfo.State.CONNECTED));
        activity = controller.get();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_ad_block), true)
                .putBoolean(activity.getString(R.string.pref_lazy_load), false)
                .apply();
        controller.create().start().resume().visible();
    }

    @Test
    public void testProgressChanged() {
        ProgressBar progressBar = activity.findViewById(R.id.progress);
        WebView webView = activity.findViewById(R.id.web_view);
        shadowOf(webView).getWebChromeClient().onProgressChanged(webView, 50);
        assertThat(progressBar).isVisible();
        shadowOf(webView).getWebChromeClient().onProgressChanged(webView, 100);
        assertThat(progressBar).isNotVisible();
    }

    @Test
    public void testDownloadContent() {
        ResolveInfo resolverInfo = new ResolveInfo();
        resolverInfo.activityInfo = new ActivityInfo();
        resolverInfo.activityInfo.applicationInfo = new ApplicationInfo();
        resolverInfo.activityInfo.applicationInfo.packageName =
                ListActivity.class.getPackage().getName();
        resolverInfo.activityInfo.name = ListActivity.class.getName();
        ShadowPackageManager rpm = shadowOf(RuntimeEnvironment.application.getPackageManager());
        final String url = "http://example.com/file.doc";
        rpm.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)), resolverInfo);

        WebView webView = activity.findViewById(R.id.web_view);
        ShadowWebView shadowWebView = Shadow.extract(webView);
        when(item.getUrl()).thenReturn(url);
        shadowWebView.getDownloadListener().onDownloadStart(url, "", "", "", 0L);
        assertThat((View) activity.findViewById(R.id.empty)).isVisible();
        activity.findViewById(R.id.download_button).performClick();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testDownloadPdf() {
        ResolveInfo resolverInfo = new ResolveInfo();
        resolverInfo.activityInfo = new ActivityInfo();
        resolverInfo.activityInfo.applicationInfo = new ApplicationInfo();
        resolverInfo.activityInfo.applicationInfo.packageName = ListActivity.class.getPackage().getName();
        resolverInfo.activityInfo.name = ListActivity.class.getName();
        ShadowPackageManager rpm = shadowOf(RuntimeEnvironment.application.getPackageManager());
        when(item.getUrl()).thenReturn("http://example.com/file.pdf");
        rpm.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl())), resolverInfo);

        WebView webView = activity.findViewById(R.id.web_view);
        ShadowWebView shadowWebView = Shadow.extract(webView);
        WebFragment fragment = (WebFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(WebFragment.class.getName());
        shadowWebView.getDownloadListener().onDownloadStart(item.getUrl(), "", "", "application/pdf", 0L);
        shadowWebView.getWebViewClient().onPageFinished(webView, PDF_LOADER_URL);
        verify(fragment.mFileDownloader).downloadFile(
            eq(item.getUrl()),
            eq("application/pdf"),
            any(FileDownloader.FileDownloaderCallback.class));
    }

    @Test
    public void testPdfAndroidJavascriptBridgeGetChunk() throws IOException {
        final String path = this.getClass().getClassLoader().getResource("file.txt").getPath();
        final File file = new File(path);
        final long size = file.length();
        final String expected = Base64.encodeToString(Okio.buffer(Okio.source(file)).readByteArray(), Base64.DEFAULT);

        final WebFragment.PdfAndroidJavascriptBridge bridge = new WebFragment.PdfAndroidJavascriptBridge(path, null);
        assertEquals(expected, bridge.getChunk(0, size));
    }

    @Test
    public void testPdfAndroidJavascriptBridgeGetSize() {
        final String path = this.getClass().getClassLoader().getResource("file.txt").getPath();
        final long expected = new File(path).length();

        final WebFragment.PdfAndroidJavascriptBridge bridge = new WebFragment.PdfAndroidJavascriptBridge(path, null);
        assertEquals(expected, bridge.getSize());
    }

    @Config(shadows = ShadowNestedScrollView.class)
    @Test
    public void testScrollToTop() {
        NestedScrollView scrollView = activity.findViewById(R.id.nested_scroll_view);
        scrollView.smoothScrollTo(0, 1);
        assertThat(customShadowOf(scrollView).getSmoothScrollY()).isEqualTo(1);
        activity.fragment.scrollToTop();
        assertThat(customShadowOf(scrollView).getSmoothScrollY()).isEqualTo(0);

    }

    @Test
    public void testFullscreenScrollToTop() {
        activity.findViewById(R.id.toolbar_web).performClick();
        assertEquals(-1, ((ShadowWebView) Shadow.extract(activity.findViewById(R.id.web_view)))
                    .getScrollY());

    }

    @Test
    public void testFullscreen() {
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true));
        assertThat((View) activity.findViewById(R.id.control_switcher)).isVisible();
        activity.recreate();
        assertThat((View) activity.findViewById(R.id.control_switcher)).isVisible();
        activity.findViewById(R.id.button_exit).performClick();
        assertThat((View) activity.findViewById(R.id.control_switcher)).isNotVisible();
    }

    @Test
    public void testSearch() {
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true));
        activity.findViewById(R.id.button_find).performClick();
        ViewSwitcher controlSwitcher = activity.findViewById(R.id.control_switcher);
        assertThat(controlSwitcher.getDisplayedChild()).isEqualTo(1);
        ShadowWebView shadowWebView = Shadow.extract(activity.findViewById(R.id.web_view));

        // no query
        EditText editText = activity.findViewById(R.id.edittext);
        shadowOf(editText).getOnEditorActionListener().onEditorAction(null, 0, null);
        assertThat((View) activity.findViewById(R.id.button_next)).isDisabled();

        // with results
        shadowWebView.setFindCount(1);
        editText.setText("abc");
        shadowOf(editText).getOnEditorActionListener().onEditorAction(null, 0, null);
        assertThat((View) activity.findViewById(R.id.button_next)).isEnabled();
        activity.findViewById(R.id.button_next).performClick();
        assertThat(shadowWebView.getFindIndex()).isEqualTo(1);
        activity.findViewById(R.id.button_clear).performClick();
        assertThat(editText).isEmpty();
        assertThat(controlSwitcher.getDisplayedChild()).isEqualTo(0);

        // with no results
        shadowWebView.setFindCount(0);
        editText.setText("abc");
        shadowOf(editText).getOnEditorActionListener().onEditorAction(null, 0, null);
        assertThat((View) activity.findViewById(R.id.button_next)).isDisabled();
        assertThat(ShadowToast.getTextOfLatestToast()).contains(activity.getString(R.string.no_matches));
    }

    @Test
    public void testRefresh() {
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true));
        ShadowWebView.lastGlobalLoadedUrl = null;
        ShadowWebView shadowWebView = Shadow.extract(activity.findViewById(R.id.web_view));
        shadowWebView.setProgress(20);
        activity.findViewById(R.id.button_refresh).performClick();
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).isNullOrEmpty();
        shadowWebView.setProgress(100);
        activity.findViewById(R.id.button_refresh).performClick();
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).isEqualTo(ShadowWebView.RELOADED);
    }

    @SuppressLint("NewApi")
    @Test
    public void testWebControls() {
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true));
        ShadowWebView shadowWebView = Shadow.extract(activity.findViewById(R.id.web_view));
        activity.findViewById(R.id.button_more).performClick();
        shadowOf(ShadowPopupMenu.getLatestPopupMenu()).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_zoom_in));
        assertThat(shadowWebView.getZoomDegree()).isEqualTo(1);
        activity.findViewById(R.id.button_more).performClick();
        shadowOf(ShadowPopupMenu.getLatestPopupMenu()).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_zoom_out));
        assertThat(shadowWebView.getZoomDegree()).isEqualTo(0);
        activity.findViewById(R.id.button_forward).performClick();
        assertThat(shadowWebView.getPageIndex()).isEqualTo(1);
        activity.findViewById(R.id.button_back).performClick();
        assertThat(shadowWebView.getPageIndex()).isEqualTo(0);
    }

    @Config(shadows = ShadowNestedScrollView.class)
    @Test
    public void testScroll() {
        ShadowNestedScrollView shadowScrollView =
                customShadowOf((NestedScrollView) activity.findViewById(R.id.nested_scroll_view));
        WebFragment fragment = (WebFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(WebFragment.class.getName());
        fragment.scrollToNext();
        assertThat(shadowScrollView.getLastScrollDirection()).isEqualTo(View.FOCUS_DOWN);
        fragment.scrollToPrevious();
        assertThat(shadowScrollView.getLastScrollDirection()).isEqualTo(View.FOCUS_UP);
        fragment.scrollToTop();
        assertThat(shadowScrollView.getSmoothScrollY()).isEqualTo(0);
    }

    @Test
    public void testFullScroll() {
        LocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true));
        ShadowWebView shadowWebView = Shadow.extract(activity.findViewById(R.id.web_view));
        WebFragment fragment = (WebFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(WebFragment.class.getName());
        fragment.scrollToTop();
        assertEquals(0, shadowWebView.getScrollY());
        fragment.scrollToNext();
        assertEquals(1, shadowWebView.getScrollY());
        fragment.scrollToPrevious();
        assertEquals(0, shadowWebView.getScrollY());
    }

    @Test
    public void testBackPressed() {
        WebView webView = activity.findViewById(R.id.web_view);
        shadowOf(webView).getWebViewClient().onPageFinished(webView, "http://example.com");
        shadowOf(webView).setCanGoBack(true);
        assertTrue(activity.fragment.onBackPressed());
        shadowOf(webView).setCanGoBack(false);
        assertFalse(activity.fragment.onBackPressed());
    }

    @Test
    public void testReadabilityToggle() {
        activity.fragment.onOptionsItemSelected(new RoboMenuItem(R.id.menu_readability));
        verify(readabilityClient).parse(any(), eq("http://example.com"), any());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

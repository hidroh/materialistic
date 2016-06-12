package io.github.hidroh.materialistic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.v4.widget.NestedScrollView;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;

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
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.ShadowNestedScrollView;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.ShadowWebView;
import io.github.hidroh.materialistic.test.WebActivity;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowWebView.class, ShadowNestedScrollView.class, ShadowSupportPreferenceManager.class})
@RunWith(RobolectricGradleTestRunner.class)
public class WebFragmentTest {
    private WebActivity activity;
    private ActivityController<WebActivity> controller;
    private WebItem item;
    @Inject FavoriteManager favoriteManager;
    @Captor ArgumentCaptor<FavoriteManager.OperationCallbacks> callbacks;
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
        controller = Robolectric.buildActivity(WebActivity.class);
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        activity = controller.get();
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_ad_block), true)
                .putBoolean(activity.getString(R.string.pref_lazy_load), false)
                .apply();
        controller.withIntent(intent).create().start().resume().visible();
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
        assertThat(activity.findViewById(R.id.empty)).isVisible();
        activity.findViewById(R.id.download_button).performClick();
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void testScrollToTop() {
        NestedScrollView scrollView = (NestedScrollView) activity.findViewById(R.id.nested_scroll_view);
        scrollView.smoothScrollTo(0, 1);
        assertEquals(1, ((ShadowNestedScrollView) ShadowExtractor.extract(scrollView))
                .getSmoothScrollY());
        activity.fragment.scrollToTop();
        assertEquals(0, ((ShadowNestedScrollView) ShadowExtractor.extract(scrollView))
                .getSmoothScrollY());

    }

    @Test
    public void testFullscreenScrollToTop() {
        activity.findViewById(R.id.toolbar_web).performClick();
        assertEquals(-1, ((ShadowWebView) ShadowExtractor.extract(activity.findViewById(R.id.web_view)))
                    .getScrollY());

    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Test
    public void testAdBlocker() {
        WebView webView = (WebView) activity.findViewById(R.id.web_view);
        WebViewClient client = shadowOf(webView).getWebViewClient();
        assertNull(client.shouldInterceptRequest(webView, "http://google.com"));
        assertNull(client.shouldInterceptRequest(webView, "http://google.com"));
        assertNotNull(client.shouldInterceptRequest(webView, "http://page2.g.doubleclick.net"));
        assertNotNull(client.shouldInterceptRequest(webView, "http://page2.g.doubleclick.net"));
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Test
    public void testAdBlockerDisabled() {
        controller.pause().stop().destroy();
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_ad_block), false)
                .apply();
        controller = Robolectric.buildActivity(WebActivity.class);
        activity = controller.get();
        controller.withIntent(intent).create().start().resume();
        WebView webView = (WebView) activity.findViewById(R.id.web_view);
        WebViewClient client = shadowOf(webView).getWebViewClient();
        assertNull(client.shouldInterceptRequest(webView, "http://google.com"));
        assertNull(client.shouldInterceptRequest(webView, "http://page2.g.doubleclick.net"));
    }

    @Test
    public void testFullscreen() {
        ShadowLocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(BaseWebFragment.ACTION_FULLSCREEN)
                        .putExtra(BaseWebFragment.EXTRA_FULLSCREEN, true));
        assertThat(activity.findViewById(R.id.control_switcher)).isVisible();
        shadowOf(activity).recreate();
        assertThat(activity.findViewById(R.id.control_switcher)).isVisible();
        activity.findViewById(R.id.button_exit).performClick();
        assertThat(activity.findViewById(R.id.control_switcher)).isNotVisible();
    }

    @Test
    public void testSearch() {
        ShadowLocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(BaseWebFragment.ACTION_FULLSCREEN)
                        .putExtra(BaseWebFragment.EXTRA_FULLSCREEN, true));
        activity.findViewById(R.id.button_find).performClick();
        ViewSwitcher controlSwitcher = (ViewSwitcher) activity.findViewById(R.id.control_switcher);
        assertThat(controlSwitcher.getDisplayedChild()).isEqualTo(1);
        ShadowWebView shadowWebView = (ShadowWebView) ShadowExtractor
                .extract(activity.findViewById(R.id.web_view));

        // no query
        EditText editText = (EditText) activity.findViewById(R.id.edittext);
        shadowOf(editText).getOnEditorActionListener().onEditorAction(null, 0, null);
        assertThat(activity.findViewById(R.id.button_next)).isNotVisible();

        // with results
        shadowWebView.setFindCount(1);
        editText.setText("abc");
        shadowOf(editText).getOnEditorActionListener().onEditorAction(null, 0, null);
        assertThat(activity.findViewById(R.id.button_next)).isVisible();
        activity.findViewById(R.id.button_next).performClick();
        assertThat(shadowWebView.getFindIndex()).isEqualTo(1);
        activity.findViewById(R.id.button_previous).performClick();
        assertThat(shadowWebView.getFindIndex()).isEqualTo(0);
        activity.findViewById(R.id.button_clear).performClick();
        assertThat(editText).isEmpty();
        assertThat(controlSwitcher.getDisplayedChild()).isEqualTo(0);

        // with no results
        shadowWebView.setFindCount(0);
        editText.setText("abc");
        shadowOf(editText).getOnEditorActionListener().onEditorAction(null, 0, null);
        assertThat(activity.findViewById(R.id.button_next)).isNotVisible();
        assertThat(ShadowToast.getTextOfLatestToast()).contains(activity.getString(R.string.no_matches));
    }

    @Test
    public void testRefresh() {
        ShadowLocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(BaseWebFragment.ACTION_FULLSCREEN)
                        .putExtra(BaseWebFragment.EXTRA_FULLSCREEN, true));
        ShadowWebView.lastGlobalLoadedUrl = null;
        ShadowWebView shadowWebView = (ShadowWebView) ShadowExtractor
                .extract(activity.findViewById(R.id.web_view));

        // should reload if fully loaded
        shadowWebView.setProgress(100);
        activity.findViewById(R.id.button_refresh).performClick();
        assertNotNull(ShadowWebView.getLastGlobalLoadedUrl());

        // should stop loading if not yet fully loaded
        ShadowWebView.lastGlobalLoadedUrl = null;
        shadowWebView.setProgress(50);
        activity.findViewById(R.id.button_refresh).performClick();
        assertNull(ShadowWebView.getLastGlobalLoadedUrl());
    }

    @SuppressLint("NewApi")
    @Test
    public void testWebControls() {
        ShadowLocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(BaseWebFragment.ACTION_FULLSCREEN)
                        .putExtra(BaseWebFragment.EXTRA_FULLSCREEN, true));
        ShadowWebView shadowWebView = (ShadowWebView) ShadowExtractor
                .extract(activity.findViewById(R.id.web_view));
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

    @Test
    public void testScroll() {
        ShadowNestedScrollView shadowScrollView = (ShadowNestedScrollView) ShadowExtractor
                .extract(activity.findViewById(R.id.nested_scroll_view));
        WebFragment fragment = (WebFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(WebFragment.class.getName());
        fragment.scrollToNext();
        assertEquals(View.FOCUS_DOWN, shadowScrollView.getLastScrollDirection());
        fragment.scrollToPrevious();
        assertEquals(View.FOCUS_UP, shadowScrollView.getLastScrollDirection());
        fragment.scrollToTop();
        assertEquals(0, shadowScrollView.getSmoothScrollY());
    }

    @Test
    public void testFullScroll() {
        ShadowLocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(BaseWebFragment.ACTION_FULLSCREEN)
                        .putExtra(BaseWebFragment.EXTRA_FULLSCREEN, true));
        ShadowWebView shadowWebView = (ShadowWebView) ShadowExtractor
                .extract(activity.findViewById(R.id.web_view));
        WebFragment fragment = (WebFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(WebFragment.class.getName());
        fragment.scrollToTop();
        assertEquals(0, shadowWebView.getScrollY());
        fragment.scrollToNext();
        assertEquals(1, shadowWebView.getScrollY());
        fragment.scrollToPrevious();
        assertEquals(0, shadowWebView.getScrollY());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

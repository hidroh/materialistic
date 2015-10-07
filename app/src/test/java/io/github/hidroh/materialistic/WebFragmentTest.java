package io.github.hidroh.materialistic;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v4.widget.NestedScrollView;
import android.webkit.WebView;
import android.widget.ProgressBar;

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
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.test.ShadowNestedScrollView;
import io.github.hidroh.materialistic.test.ShadowWebView;
import io.github.hidroh.materialistic.test.WebActivity;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

// TODO switch to API 21 once ShareActionProvider is fixed
@Config(shadows = {ShadowWebView.class, ShadowNestedScrollView.class}, sdk = 19)
@RunWith(RobolectricGradleTestRunner.class)
public class WebFragmentTest {
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
        when(item.getType()).thenReturn(ItemManager.Item.STORY_TYPE);
        Intent intent = new Intent();
        intent.putExtra(WebActivity.EXTRA_ITEM, item);
        controller = Robolectric.buildActivity(WebActivity.class);
        activity = controller.withIntent(intent).create().start().resume().get();
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
        assertThat(activity.findViewById(R.id.download_button)).isVisible();
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

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

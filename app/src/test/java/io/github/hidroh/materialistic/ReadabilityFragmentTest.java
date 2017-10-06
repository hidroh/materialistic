package io.github.hidroh.materialistic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.view.View;
import android.webkit.WebView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowPopupMenu;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.TestReadabilityActivity;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.TestWebItem;
import io.github.hidroh.materialistic.test.shadow.ShadowNestedScrollView;
import io.github.hidroh.materialistic.test.shadow.ShadowPreferenceFragmentCompat;
import io.github.hidroh.materialistic.test.shadow.ShadowWebView;

import static io.github.hidroh.materialistic.test.shadow.CustomShadows.customShadowOf;
import static junit.framework.Assert.assertEquals;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.android.support.v4.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowWebView.class, ShadowPreferenceFragmentCompat.class})
@RunWith(TestRunner.class)
public class ReadabilityFragmentTest {
    private TestReadabilityActivity activity;
    private ActivityController<TestReadabilityActivity> controller;
    @Inject ReadabilityClient readabilityClient;
    @Captor ArgumentCaptor<ReadabilityClient.Callback> callback;
    private WebFragment fragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(readabilityClient);
        controller = Robolectric.buildActivity(TestReadabilityActivity.class);
        activity = controller.create().start().resume().visible().get();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_lazy_load), false)
                .putString(activity.getString(R.string.pref_story_display),
                        activity.getString(R.string.pref_story_display_value_readability))
                .apply();
        shadowOf((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        Bundle args = new Bundle();
        WebItem item = new TestWebItem() {
            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getUrl() {
                return "http://example.com/article.html";
            }
        };
        args.putParcelable(WebFragment.EXTRA_ITEM, item);
        fragment = (WebFragment) Fragment.instantiate(activity, WebFragment.class.getName(), args);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment, "tag")
                .commit();
    }

    @Test
    public void testParseAndBind() {
        assertThat((View) activity.findViewById(R.id.progress)).isVisible();
        verify(readabilityClient).parse(eq("1"), eq("http://example.com/article.html"),
                callback.capture());
        callback.getValue().onResponse("<div>content</div>");
        WebView webView = (WebView) activity.findViewById(R.id.web_view);
        shadowOf(webView).getWebViewClient().onPageFinished(webView, "about:blank");
        assertThat(shadowOf(webView).getLastLoadDataWithBaseURL().data).contains("content");
        shadowOf(activity).recreate();
        webView = (WebView) activity.findViewById(R.id.web_view);
        shadowOf(webView).getWebViewClient().onPageFinished(webView, "about:blank");
        assertThat(shadowOf(webView).getLastLoadDataWithBaseURL().data).contains("content");
        controller.pause().stop().destroy();
    }

    @Test
    public void testParseFailed() {
        assertThat(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_font_options)).isNotVisible();
        assertThat((View) activity.findViewById(R.id.progress)).isVisible();
        verify(readabilityClient).parse(eq("1"), eq("http://example.com/article.html"),
                callback.capture());
        callback.getValue().onResponse(null);
        reset(readabilityClient);
        assertThat(ShadowToast.getTextOfLatestToast())
                .contains(activity.getString(R.string.readability_failed));
        WebView webView = (WebView) activity.findViewById(R.id.web_view);
        shadowOf(webView).getWebViewClient().onPageFinished(webView, "about:blank");
        assertThat(ShadowWebView.getLastGlobalLoadedUrl())
                .contains("http://example.com/article.html");
        assertThat(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_font_options)).isNotVisible();
        controller.pause().stop().destroy();
    }

    @Config(shadows = ShadowNestedScrollView.class)
    @Test
    public void testScrollToTop() {
        NestedScrollView scrollView = (NestedScrollView) activity.findViewById(R.id.nested_scroll_view);
        scrollView.smoothScrollTo(0, 1);
        assertThat(customShadowOf(scrollView).getSmoothScrollY()).isEqualTo(1);
        fragment.scrollToTop();
        assertThat(customShadowOf(scrollView).getSmoothScrollY()).isEqualTo(0);
        controller.pause().stop().destroy();
    }

    @Test
    public void testFontSizeMenu() {
        verify(readabilityClient).parse(eq("1"), eq("http://example.com/article.html"),
                callback.capture());
        callback.getValue().onResponse("<div>content</div>");
        fragment.onOptionsItemSelected(new RoboMenuItem(R.id.menu_font_options));
        assertThat(fragment.getFragmentManager())
                .hasFragmentWithTag(PopupSettingsFragment.class.getName());
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_readability_text_size), "3")
                .apply();
        WebView webView = (WebView) activity.findViewById(R.id.web_view);
        shadowOf(webView).getWebViewClient().onPageFinished(webView, "about:blank");
        assertThat(shadowOf(webView).getLastLoadDataWithBaseURL().data).contains("20");
        assertEquals(R.style.AppTextSize_XLarge,
                Preferences.Theme.resolvePreferredReadabilityTextSize(activity));
        controller.pause().stop().destroy();
    }

    @Test
    public void testFontMenu() {
        verify(readabilityClient).parse(eq("1"), eq("http://example.com/article.html"),
                callback.capture());
        callback.getValue().onResponse("<div>content</div>");
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_readability_font), "DroidSans.ttf")
                .apply();
        WebView webView = (WebView) activity.findViewById(R.id.web_view);
        shadowOf(webView).getWebViewClient().onPageFinished(webView, "about:blank");
        assertThat(shadowOf(webView).getLastLoadDataWithBaseURL().data).contains("DroidSans.ttf");
        assertEquals("DroidSans.ttf", Preferences.Theme.getReadabilityTypeface(activity));
        controller.pause().stop().destroy();
    }

    @Test
    public void testWebToggle() {
        fragment.onOptionsItemSelected(new RoboMenuItem(R.id.menu_readability));
        WebView webView = (WebView) activity.findViewById(R.id.web_view);
        shadowOf(webView).getWebViewClient().onPageFinished(webView, "about:blank");
        assertThat(shadowOf(webView).getLastLoadedUrl()).isEqualTo("http://example.com/article.html");
    }

    @SuppressLint("NewApi")
    @Test
    public void testFullscreenMenu() {
        verify(readabilityClient).parse(eq("1"), eq("http://example.com/article.html"),
                callback.capture());
        callback.getValue().onResponse("<div>content</div>");
        ShadowLocalBroadcastManager.getInstance(activity)
                .sendBroadcast(new Intent(WebFragment.ACTION_FULLSCREEN)
                        .putExtra(WebFragment.EXTRA_FULLSCREEN, true));
        activity.findViewById(R.id.button_more).performClick();
        shadowOf(ShadowPopupMenu.getLatestPopupMenu()).getOnMenuItemClickListener()
                .onMenuItemClick(new RoboMenuItem(R.id.menu_font_options));
        assertThat(fragment.getFragmentManager())
                .hasFragmentWithTag(PopupSettingsFragment.class.getName());
    }

    @Test
    public void testBindAfterDetached() {
        assertThat((View) activity.findViewById(R.id.progress)).isVisible();
        controller.pause().stop().destroy();
        verify(readabilityClient).parse(eq("1"), eq("http://example.com/article.html"),
                callback.capture());
        callback.getValue().onResponse("<div>content</div>");
    }
}

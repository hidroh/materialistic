package io.github.hidroh.materialistic;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.test.ShadowNestedScrollView;
import io.github.hidroh.materialistic.test.ShadowTextView;
import io.github.hidroh.materialistic.test.TestReadabilityActivity;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowNestedScrollView.class, ShadowTextView.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ReadabilityFragmentTest {
    private TestReadabilityActivity activity;
    private ActivityController<TestReadabilityActivity> controller;
    @Inject ReadabilityClient readabilityClient;
    @Captor ArgumentCaptor<ReadabilityClient.Callback> callback;
    private ReadabilityFragment fragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(readabilityClient);
        controller = Robolectric.buildActivity(TestReadabilityActivity.class);
        activity = controller.create().start().resume().visible().get();
        shadowOf((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        Bundle args = new Bundle();
        args.putString(ReadabilityFragment.EXTRA_URL, "http://example.com/article.html");
        fragment = (ReadabilityFragment) Fragment.instantiate(activity, ReadabilityFragment.class.getName(), args);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment, "tag")
                .commit();
    }

    @Test
    public void testParseAndBind() {
        assertThat(activity.findViewById(R.id.progress)).isVisible();
        verify(readabilityClient).parse(eq("http://example.com/article.html"), callback.capture());
        callback.getValue().onResponse("<div>content</div>");
        assertThat(activity.findViewById(R.id.progress)).isNotVisible();
        assertThat((TextView) activity.findViewById(R.id.content)).containsText("content");
        shadowOf(activity).recreate();
        assertThat((TextView) activity.findViewById(R.id.content)).containsText("content");
    }

    @Test
    public void testParseFailed() {
        assertThat(activity.findViewById(R.id.progress)).isVisible();
        verify(readabilityClient).parse(eq("http://example.com/article.html"), callback.capture());
        callback.getValue().onResponse(null);
        reset(readabilityClient);
        assertThat(activity.findViewById(R.id.progress)).isVisible();
        View snackbarAction = activity.findViewById(R.id.snackbar_action);
        assertThat(snackbarAction).isVisible();
        snackbarAction.performClick();
        verify(readabilityClient).parse(eq("http://example.com/article.html"),
                any(ReadabilityClient.Callback.class));
    }

    @Test
    public void testScrollToTop() {
        NestedScrollView scrollView = (NestedScrollView) activity.findViewById(R.id.nested_scroll_view);
        scrollView.smoothScrollTo(0, 1);
        assertEquals(1, ((ShadowNestedScrollView) ShadowExtractor.extract(scrollView))
                .getSmoothScrollY());
        fragment.scrollToTop();
        assertEquals(0, ((ShadowNestedScrollView) ShadowExtractor.extract(scrollView))
                .getSmoothScrollY());
    }

    @Test
    public void testFontSizeMenu() {
        assertThat((TextView) activity.findViewById(R.id.content)).hasTextSize(14); // small - default
        MenuItem menuItem = shadowOf(activity).getOptionsMenu().findItem(R.id.menu_font_size);
        assertThat(menuItem).hasSubMenu();
        assertThat(menuItem.getSubMenu()).hasSize(5);
        shadowOf(activity).clickMenuItem(R.id.menu_font_size);
        fragment.onOptionsItemSelected(menuItem.getSubMenu().getItem(4)); // extra large
        assertThat((TextView) activity.findViewById(R.id.content)).hasTextSize(20);
    }

    @Test
    public void testFontMenu() {
        assertNull(((ShadowTextView) ShadowExtractor.extract(activity
                .findViewById(R.id.content))).getTypeface()); // no custom typeface set
        MenuItem menuItem = shadowOf(activity).getOptionsMenu().findItem(R.id.menu_font);
        assertThat(menuItem).hasSubMenu();
        assertThat(menuItem.getSubMenu()).hasSize(5);
        shadowOf(activity).clickMenuItem(R.id.menu_font);
        fragment.onOptionsItemSelected(menuItem.getSubMenu().getItem(1)); // non default
        assertNotNull(((ShadowTextView) ShadowExtractor.extract(activity
                .findViewById(R.id.content))).getTypeface()); // custom typeface set
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

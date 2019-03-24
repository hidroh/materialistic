package io.github.hidroh.materialistic;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.webkit.WebView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowWebView;
import org.robolectric.android.controller.ActivityController;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.Item;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ResponseListener;
import io.github.hidroh.materialistic.test.TestItem;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.TestWebItem;
import io.github.hidroh.materialistic.test.WebActivity;
import io.github.hidroh.materialistic.test.shadow.ShadowPreferenceFragmentCompat;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("ConstantConditions")
@Config(shadows = {ShadowPreferenceFragmentCompat.class})
@RunWith(TestRunner.class)
public class WebFragmentLocalTest {
    private ActivityController<WebActivity> controller;
    private WebActivity activity;
    @Inject @Named(ActivityModule.HN) ItemManager itemManager;
    @Captor ArgumentCaptor<ResponseListener<Item>> listener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        TestApplication.applicationGraph.inject(this);
        reset(itemManager);
        controller = Robolectric.buildActivity(WebActivity.class);
        activity = controller.get();
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_lazy_load), false)
                .apply();
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, NetworkInfo.State.CONNECTED));
    }

    @Test
    public void testStory() {
        TestWebItem item = new TestWebItem() {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getUrl() {
                return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
            }

            @Override
            public String getDisplayedTitle() {
                return "Ask HN";
            }
        };
        Intent intent = new Intent();
        intent.putExtra(WebActivity.EXTRA_ITEM, item);
        controller = Robolectric.buildActivity(WebActivity.class, intent);
        controller.create().start().resume().visible();
        activity = controller.get();
        verify(itemManager).getItem(eq("1"), eq(ItemManager.MODE_DEFAULT), listener.capture());
        listener.getValue().onResponse(new TestItem() {
            @Override
            public String getText() {
                return "text";
            }
        });
        WebView webView = activity.findViewById(R.id.web_view);
        ShadowWebView shadowWebView = shadowOf(webView);
        shadowWebView.getWebViewClient().onPageFinished(webView, "about:blank");
        assertThat(shadowWebView.getLastLoadDataWithBaseURL().data).contains("text");
    }

    @Test
    public void testComment() {
        TestItem item = new TestItem() {
            @NonNull
            @Override
            public String getType() {
                return COMMENT_TYPE;
            }

            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getUrl() {
                return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
            }

            @Override
            public String getText() {
                return "comment";
            }
        };
        Intent intent = new Intent();
        intent.putExtra(WebActivity.EXTRA_ITEM, item);
        controller = Robolectric.buildActivity(WebActivity.class, intent);
        controller.create().start().resume().visible();
        activity = controller.get();
        WebView webView = activity.findViewById(R.id.web_view);
        ShadowWebView shadowWebView = shadowOf(webView);
        shadowWebView.getWebViewClient().onPageFinished(webView, "about:blank");
        assertThat(shadowWebView.getLastLoadDataWithBaseURL().data).contains("comment");
    }

    @Test
    public void testMenu() {
        TestWebItem item = new TestWebItem() {
            @NonNull
            @Override
            public String getType() {
                return STORY_TYPE;
            }

            @Override
            public String getId() {
                return "1";
            }

            @Override
            public String getUrl() {
                return String.format(HackerNewsClient.WEB_ITEM_PATH, "1");
            }

            @Override
            public String getDisplayedTitle() {
                return "Ask HN";
            }
        };
        Intent intent = new Intent();
        intent.putExtra(WebActivity.EXTRA_ITEM, item);
        controller = Robolectric.buildActivity(WebActivity.class, intent);
        controller.create().start().resume().visible();
        activity = controller.get();
        verify(itemManager).getItem(eq("1"), eq(ItemManager.MODE_DEFAULT), listener.capture());
        listener.getValue().onResponse(new TestItem() {
            @Override
            public String getText() {
                return "text";
            }
        });
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentByTag(WebFragment.class.getName());
        assertTrue(fragment.hasOptionsMenu());
        fragment.onOptionsItemSelected(new RoboMenuItem(R.id.menu_font_options));
        assertNotNull(ShadowDialog.getLatestDialog());
        PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putString(activity.getString(R.string.pref_readability_font), "DroidSans.ttf")
                .apply();
        WebView webView = activity.findViewById(R.id.web_view);
        ShadowWebView shadowWebView = shadowOf(webView);
        shadowWebView.getWebViewClient().onPageFinished(webView, "about:blank");
        assertThat(shadowWebView.getLastLoadDataWithBaseURL().data)
                .contains("text")
                .contains("DroidSans.ttf");
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

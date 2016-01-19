package io.github.hidroh.materialistic;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.WebItem;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.TestReadabilityActivity;
import io.github.hidroh.materialistic.test.TestWebItem;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowSupportPreferenceManager.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ReadabilityFragmentLazyLoadTest {
    private TestReadabilityActivity activity;
    private ActivityController<TestReadabilityActivity> controller;
    @Inject ReadabilityClient readabilityClient;
    private Fragment fragment;

    @Before
    public void setUp() {
        TestApplication.applicationGraph.inject(this);
        reset(readabilityClient);
        controller = Robolectric.buildActivity(TestReadabilityActivity.class);
        activity = controller.create().start().resume().visible().get();
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
        args.putParcelable(ReadabilityFragment.EXTRA_ITEM, item);
        fragment = Fragment.instantiate(activity, ReadabilityFragment.class.getName(), args);
        shadowOf((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(null);
    }

    @Test
    public void testLazyLoadByDefault() {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment, "tag")
                .commit();
        verify(readabilityClient, never()).parse(anyString(), anyString(), any(ReadabilityClient.Callback.class));
        reset(readabilityClient);
        fragment.setUserVisibleHint(true);
        fragment.setUserVisibleHint(false);
        verify(readabilityClient).parse(anyString(), anyString(), any(ReadabilityClient.Callback.class));
    }

    @Test
    public void testLazyLoadOnWifi() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_lazy_load), false)
                .commit();
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment, "tag")
                .commit();
        verify(readabilityClient, never()).parse(anyString(), anyString(), any(ReadabilityClient.Callback.class));
        reset(readabilityClient);
        fragment.setUserVisibleHint(true);
        fragment.setUserVisibleHint(false);
        verify(readabilityClient).parse(anyString(), anyString(), any(ReadabilityClient.Callback.class));
    }

    @Test
    public void testVisible() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_lazy_load), false)
                .commit();
        shadowOf((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        fragment.setUserVisibleHint(true);
        verify(readabilityClient, never()).parse(anyString(), anyString(), any(ReadabilityClient.Callback.class));
        reset(readabilityClient);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment, "tag")
                .commit();
        verify(readabilityClient).parse(anyString(), anyString(), any(ReadabilityClient.Callback.class));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

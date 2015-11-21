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
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.util.ActivityController;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.test.TestReadabilityActivity;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

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
        args.putString(ReadabilityFragment.EXTRA_URL, "http://example.com/article.html");
        fragment = Fragment.instantiate(activity, ReadabilityFragment.class.getName(), args);
        shadowOf((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(null);
    }

    @Test
    public void testLazyLoad() {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment, "tag")
                .commit();
        verify(readabilityClient, never()).parse(anyString(), any(ReadabilityClient.Callback.class));
        reset(readabilityClient);
        fragment.setUserVisibleHint(true);
        fragment.setUserVisibleHint(false);
        verify(readabilityClient).parse(anyString(), any(ReadabilityClient.Callback.class));
    }

    @Test
    public void testVisible() {
        shadowOf((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, true));
        fragment.setUserVisibleHint(true);
        verify(readabilityClient, never()).parse(anyString(), any(ReadabilityClient.Callback.class));
        reset(readabilityClient);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment, "tag")
                .commit();
        verify(readabilityClient).parse(anyString(), any(ReadabilityClient.Callback.class));
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}

package io.github.hidroh.materialistic.data;

import android.content.ContentValues;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import rx.schedulers.Schedulers;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
public class SessionManagerTest {
    private ShadowContentResolver resolver;
    private SessionManager manager;

    @Before
    public void setUp() {
        resolver = shadowOf(RuntimeEnvironment.application.getContentResolver());
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        resolver.insert(MaterialisticProvider.URI_VIEWED, cv);
        cv = new ContentValues();
        cv.put("itemid", "2");
        resolver.insert(MaterialisticProvider.URI_VIEWED, cv);
        manager = new SessionManager(Schedulers.immediate());
    }

    @Test
    public void testIsViewedNull() {
        assertFalse(manager.isViewed(RuntimeEnvironment.application.getContentResolver(), null)
                .toBlocking().single());
    }

    @Test
    public void testIsViewedTrue() {
        assertTrue(manager.isViewed(RuntimeEnvironment.application.getContentResolver(), "1")
                .toBlocking().single());
    }

    @Test
    public void testIsViewedFalse() {
        assertFalse(manager.isViewed(RuntimeEnvironment.application.getContentResolver(), "-1")
                .toBlocking().single());
    }

    @Test
    public void testViewNoId() {
        manager.view(RuntimeEnvironment.application, null);
        assertThat(resolver.getNotifiedUris()).isEmpty();
    }
    @Test
    public void testView() {
        manager.view(RuntimeEnvironment.application, "3");
        assertThat(resolver.getNotifiedUris()).isNotEmpty();
    }
}

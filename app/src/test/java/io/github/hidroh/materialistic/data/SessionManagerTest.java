package io.github.hidroh.materialistic.data;

import android.content.ContentValues;
import android.content.ShadowAsyncQueryHandler;
import android.support.v4.content.LocalBroadcastManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.support.v4.Shadows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowAsyncQueryHandler.class})
@RunWith(RobolectricGradleTestRunner.class)
public class SessionManagerTest {
    private ShadowContentResolver resolver;
    private SessionManager manager;
    private SessionManager.OperationCallbacks callbacks;

    @Before
    public void setUp() {
        callbacks = mock(SessionManager.OperationCallbacks.class);
        resolver = shadowOf(ShadowApplication.getInstance().getContentResolver());
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        resolver.insert(MaterialisticProvider.URI_VIEWED, cv);
        cv = new ContentValues();
        cv.put("itemid", "2");
        resolver.insert(MaterialisticProvider.URI_VIEWED, cv);
        manager = new SessionManager();
    }

    @Test
    public void testIsViewNull() {
        manager.isViewed(RuntimeEnvironment.application.getContentResolver(), null, callbacks);
        verify(callbacks, never()).onCheckComplete(anyBoolean());
    }

    @Test
    public void testIsViewTrue() {
        manager.isViewed(RuntimeEnvironment.application.getContentResolver(), "1", callbacks);
        verify(callbacks).onCheckComplete(eq(true));
    }

    @Test
    public void testIsViewFalse() {
        manager.isViewed(RuntimeEnvironment.application.getContentResolver(), "-1", callbacks);
        verify(callbacks).onCheckComplete(eq(false));
    }

    @Test
    public void testViewNoId() {
        manager.view(RuntimeEnvironment.application, null);
        assertThat(Shadows.shadowOf(LocalBroadcastManager.getInstance(RuntimeEnvironment.application))
                .getSentBroadcastIntents()).isEmpty();
    }
    @Test
    public void testView() {
        manager.view(RuntimeEnvironment.application, "3");
        assertThat(resolver.getNotifiedUris()).isNotEmpty();
    }
}

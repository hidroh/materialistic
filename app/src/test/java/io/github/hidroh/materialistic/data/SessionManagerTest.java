package io.github.hidroh.materialistic.data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import io.github.hidroh.materialistic.DataModule;
import io.github.hidroh.materialistic.test.InMemoryCache;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(JUnit4.class)
public class SessionManagerTest {
    @Inject LocalCache cache;
    private SessionManager manager;

    @Before
    public void setUp() {
        ObjectGraph objectGraph = ObjectGraph.create(new TestModule());
        objectGraph.inject(this);
        cache.setViewed("1");
        cache.setViewed("2");
        manager = new SessionManager();
        objectGraph.inject(manager);
    }

    @Test
    public void testIsViewedNull() {
        assertFalse(manager.isViewed(null)
                .toBlocking().single());
    }

    @Test
    public void testIsViewedTrue() {
        assertTrue(manager.isViewed("1")
                .toBlocking().single());
    }

    @Test
    public void testIsViewedFalse() {
        assertFalse(manager.isViewed("-1")
                .toBlocking().single());
    }

    @Module(
            injects = {
                    SessionManagerTest.class,
                    SessionManager.class
            },
            overrides = true
    )
    static class TestModule {
        @Provides @Singleton @Named(DataModule.IO_THREAD)
        public Scheduler provideIoThreadScheduler() {
            return Schedulers.immediate();
        }

        @Provides @Singleton
        public LocalCache provideLocalCache() {
            return new InMemoryCache();
        }
    }
}

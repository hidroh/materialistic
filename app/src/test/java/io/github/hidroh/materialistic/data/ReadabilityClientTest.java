package io.github.hidroh.materialistic.data;

import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import io.github.hidroh.materialistic.DataModule;
import io.github.hidroh.materialistic.test.InMemoryCache;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class ReadabilityClientTest {
    @Inject RestServiceFactory factory;
    @Inject LocalCache cache;
    private ReadabilityClient client;
    private ReadabilityClient.Callback callback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph objectGraph = ObjectGraph.create(new TestModule());
        objectGraph.inject(this);
        reset(TestRestServiceFactory.mercuryService);
        client = new ReadabilityClient.Impl(cache, factory);
        objectGraph.inject(client);
        callback = mock(ReadabilityClient.Callback.class);
    }

    @Test
    public void testWithContent() {
        ReadabilityClient.Impl.Readable readable = new GsonBuilder().create()
                .fromJson("{\"content\":\"<div>content</div>\"}", ReadabilityClient.Impl.Readable.class);
        when(TestRestServiceFactory.mercuryService.parse(any()))
                .thenReturn(Observable.just(readable));
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.mercuryService).parse(any());
        verify(callback).onResponse(eq("<div>content</div>"));
    }

    @Test
    public void testEmptyContent() {
        ReadabilityClient.Impl.Readable readable = new GsonBuilder().create()
                .fromJson("{\"content\":\"<div></div>\"}", ReadabilityClient.Impl.Readable.class);
        when(TestRestServiceFactory.mercuryService.parse(any()))
                .thenReturn(Observable.just(readable));
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.mercuryService).parse(any());
        verify(callback).onResponse(isNull());
    }

    @Test
    public void testError() {
        when(TestRestServiceFactory.mercuryService.parse(any()))
                .thenReturn(Observable.error(new IOException()));
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.mercuryService).parse(any());
        verify(callback).onResponse(isNull());
    }

    @Test
    public void testCachedContent() {
        cache.putReadability("1", "<div>content</div>");
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.mercuryService, never()).parse(any());
        verify(callback).onResponse(eq("<div>content</div>"));
    }

    @Test
    public void testEmptyCachedContent() {
        cache.putReadability("1", "<div></div>");
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.mercuryService, never()).parse(any());
        verify(callback).onResponse(isNull());
    }

    @Module(
            injects = {
                    ReadabilityClientTest.class,
                    ReadabilityClient.Impl.class
            },
            overrides = true
    )
    static class TestModule {
        @Provides
        @Singleton
        public RestServiceFactory provideRestServiceFactory() {
            return new TestRestServiceFactory();
        }

        @Provides @Singleton @Named(DataModule.MAIN_THREAD)
        public Scheduler provideMainThreadScheduler() {
            return Schedulers.immediate();
        }

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

package io.github.hidroh.materialistic.data;

import android.content.ContentValues;

import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import io.github.hidroh.materialistic.test.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import rx.Observable;
import rx.schedulers.Schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("unchecked")
@RunWith(RobolectricGradleTestRunner.class)
public class ReadabilityClientTest {
    @Inject RestServiceFactory factory;
    private ReadabilityClient client;
    private ReadabilityClient.Callback callback;
    private ShadowContentResolver resolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.mercuryService);
        client = new ReadabilityClient.Impl(RuntimeEnvironment.application, factory,
                Schedulers.immediate());
        callback = mock(ReadabilityClient.Callback.class);
        resolver = shadowOf(RuntimeEnvironment.application.getContentResolver());
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
        verify(callback).onResponse((String) isNull());
    }

    @Test
    public void testError() {
        when(TestRestServiceFactory.mercuryService.parse(any()))
                .thenReturn(Observable.error(new IOException()));
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.mercuryService).parse(any());
        verify(callback).onResponse((String) isNull());
    }

    @Test
    public void testCachedContent() {
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        cv.put("content", "<div>content</div>");
        resolver.insert(MaterialisticProvider.URI_READABILITY, cv);
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.mercuryService, never()).parse(any());
        verify(callback).onResponse(eq("<div>content</div>"));
    }

    @Test
    public void testEmptyCachedContent() {
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        cv.put("content", "<div></div>");
        resolver.insert(MaterialisticProvider.URI_READABILITY, cv);
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.mercuryService, never()).parse(any());
        verify(callback).onResponse((String) isNull());
    }

    @Module(
            injects = ReadabilityClientTest.class,
            overrides = true
    )
    static class TestModule {
        @Provides
        @Singleton
        public RestServiceFactory provideRestServiceFactory() {
            return new TestRestServiceFactory();
        }
    }
}

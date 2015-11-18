package io.github.hidroh.materialistic.data;

import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import retrofit.Callback;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
public class ReadabilityClientTest {
    @Inject RestServiceFactory factory;
    @Captor ArgumentCaptor<Callback<ReadabilityClient.Impl.Readable>> callbackCaptor;
    private ReadabilityClient client;
    private ReadabilityClient.Callback callback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.readabilityService);
        client = new ReadabilityClient.Impl(factory);
        callback = mock(ReadabilityClient.Callback.class);
    }

    @Test
    public void testWithContent() {
        client.parse("http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService)
                .parse(anyString(), callbackCaptor.capture());
        ReadabilityClient.Impl.Readable readable = new GsonBuilder().create()
                .fromJson("{\"content\":\"<div>content</div>\"}", ReadabilityClient.Impl.Readable.class);
        callbackCaptor.getValue().success(readable, null);
        verify(callback).onResponse(eq("<div>content</div>"));
    }

    @Test
    public void testEmptyContent() {
        client.parse("http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService)
                .parse(anyString(), callbackCaptor.capture());
        ReadabilityClient.Impl.Readable readable = new GsonBuilder().create()
                .fromJson("{\"content\":\"<div></div>\"}", ReadabilityClient.Impl.Readable.class);
        callbackCaptor.getValue().success(readable, null);
        verify(callback).onResponse((String) isNull());
    }

    @Test
    public void testError() {
        client.parse("http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService)
                .parse(anyString(), callbackCaptor.capture());
        callbackCaptor.getValue().failure(null);
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

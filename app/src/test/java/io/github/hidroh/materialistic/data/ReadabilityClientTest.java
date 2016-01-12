package io.github.hidroh.materialistic.data;

import android.content.ContentValues;
import android.content.ShadowAsyncQueryHandler;

import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@Config(shadows = {ShadowAsyncQueryHandler.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ReadabilityClientTest {
    @Inject RestServiceFactory factory;
    @Captor ArgumentCaptor<Callback<ReadabilityClient.Impl.Readable>> callbackCaptor;
    private ReadabilityClient client;
    private ReadabilityClient.Callback callback;
    private ShadowContentResolver resolver;
    private Call call;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.readabilityService);
        client = new ReadabilityClient.Impl(RuntimeEnvironment.application, factory);
        callback = mock(ReadabilityClient.Callback.class);
        call = mock(Call.class);
        when(TestRestServiceFactory.readabilityService.parse(anyString())).thenReturn(call);
        resolver = shadowOf(ShadowApplication.getInstance().getContentResolver());
    }

    @Test
    public void testWithContent() {
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService).parse(anyString());
        verify(call).enqueue(callbackCaptor.capture());
        ReadabilityClient.Impl.Readable readable = new GsonBuilder().create()
                .fromJson("{\"content\":\"<div>content</div>\"}", ReadabilityClient.Impl.Readable.class);
        callbackCaptor.getValue().onResponse(Response.success(readable));
        verify(callback).onResponse(eq("<div>content</div>"));
    }

    @Test
    public void testEmptyContent() {
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService).parse(anyString());
        verify(call).enqueue(callbackCaptor.capture());
        ReadabilityClient.Impl.Readable readable = new GsonBuilder().create()
                .fromJson("{\"content\":\"<div></div>\"}", ReadabilityClient.Impl.Readable.class);
        callbackCaptor.getValue().onResponse(Response.success(readable));
        verify(callback).onResponse((String) isNull());
    }

    @Test
    public void testError() {
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService).parse(anyString());
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null);
        verify(callback).onResponse((String) isNull());
    }

    @Test
    public void testCachedContent() {
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        cv.put("content", "<div>content</div>");
        resolver.insert(MaterialisticProvider.URI_READABILITY, cv);
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService, never()).parse(anyString());
        verify(call, never()).enqueue(any(Callback.class));
        verify(callback).onResponse(eq("<div>content</div>"));
    }

    @Test
    public void testEmptyCachedContent() {
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        cv.put("content", "<div></div>");
        resolver.insert(MaterialisticProvider.URI_READABILITY, cv);
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService, never()).parse(anyString());
        verify(call, never()).enqueue(any(Callback.class));
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

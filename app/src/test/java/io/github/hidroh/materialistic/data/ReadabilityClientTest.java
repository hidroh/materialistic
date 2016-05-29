package io.github.hidroh.materialistic.data;

import android.content.ContentValues;

import com.squareup.moshi.Moshi;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
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
        resolver = shadowOf(RuntimeEnvironment.application.getContentResolver());
    }

    @Test
    public void testWithContent() throws IOException {
        ReadabilityClient.Impl.Readable readable = new Moshi.Builder().build()
                .adapter(ReadabilityClient.Impl.Readable.class)
                .fromJson("{\"content\":\"<div>content</div>\"}");
        when(call.execute()).thenReturn(Response.success(readable));
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService).parse(anyString());
        verify(call).execute();
        verify(callback).onResponse(eq("<div>content</div>"));
    }

    @Test
    public void testEmptyContent() throws IOException {
        ReadabilityClient.Impl.Readable readable = new Moshi.Builder().build()
                .adapter(ReadabilityClient.Impl.Readable.class)
                .fromJson("{\"content\":\"<div></div>\"}");
        when(call.execute()).thenReturn(Response.success(readable));
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService).parse(anyString());
        verify(call).execute();
        verify(callback).onResponse((String) isNull());
    }

    @Test
    public void testError() throws IOException {
        when(call.execute()).thenThrow(IOException.class);
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService).parse(anyString());
        verify(call).execute();
        verify(callback).onResponse((String) isNull());
    }

    @Test
    public void testCachedContent() throws IOException {
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        cv.put("content", "<div>content</div>");
        resolver.insert(MaterialisticProvider.URI_READABILITY, cv);
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService, never()).parse(anyString());
        verify(call, never()).execute();
        verify(callback).onResponse(eq("<div>content</div>"));
    }

    @Test
    public void testEmptyCachedContent() throws IOException {
        ContentValues cv = new ContentValues();
        cv.put("itemid", "1");
        cv.put("content", "<div></div>");
        resolver.insert(MaterialisticProvider.URI_READABILITY, cv);
        client.parse("1", "http://example.com/article.html", callback);
        verify(TestRestServiceFactory.readabilityService, never()).parse(anyString());
        verify(call, never()).execute();
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

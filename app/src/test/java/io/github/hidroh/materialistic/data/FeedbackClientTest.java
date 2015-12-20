package io.github.hidroh.materialistic.data;

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
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class FeedbackClientTest {
    @Inject RestServiceFactory factory;
    @Captor ArgumentCaptor<Callback<Object>> callbackCaptor;
    private FeedbackClient client;
    private FeedbackClient.Callback callback;
    private Call call;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.feedbackService);
        client = new FeedbackClient.Impl(factory);
        callback = mock(FeedbackClient.Callback.class);
        call = mock(Call.class);
        when(TestRestServiceFactory.feedbackService
                .createGithubIssue(any(FeedbackClient.Impl.Issue.class)))
                .thenReturn(call);
    }

    @Test
    public void testSendSuccessful() {
        client.send("title", "body", callback);
        verify(TestRestServiceFactory.feedbackService)
                .createGithubIssue(any(FeedbackClient.Impl.Issue.class));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(Response.success(null), null);
        verify(callback).onSent(eq(true));
    }

    @Test
    public void testSendFailed() {
        client.send("title", "body", callback);
        verify(TestRestServiceFactory.feedbackService)
                .createGithubIssue(any(FeedbackClient.Impl.Issue.class));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null);
        verify(callback).onSent(eq(false));
    }

    @Module(
            injects = FeedbackClientTest.class,
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

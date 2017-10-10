package io.github.hidroh.materialistic.data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class FeedbackClientTest {
    @Inject RestServiceFactory factory;
    private FeedbackClient client;
    private FeedbackClient.Callback callback;

    @Before
    public void setUp() {
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.feedbackService);
        client = new FeedbackClient.Impl(factory, Schedulers.immediate());
        callback = mock(FeedbackClient.Callback.class);
    }

    @Test
    public void testSendSuccessful() {
        when(TestRestServiceFactory.feedbackService
                .createGithubIssue(any(FeedbackClient.Impl.Issue.class)))
                .thenReturn(Observable.just(null));
        client.send("title", "body", callback);
        verify(TestRestServiceFactory.feedbackService)
                .createGithubIssue(any(FeedbackClient.Impl.Issue.class));
        verify(callback).onSent(eq(true));
    }

    @Test
    public void testSendFailed() {
        when(TestRestServiceFactory.feedbackService
                .createGithubIssue(any(FeedbackClient.Impl.Issue.class)))
                .thenReturn(Observable.error(new IOException()));
        client.send("title", "body", callback);
        verify(TestRestServiceFactory.feedbackService)
                .createGithubIssue(any(FeedbackClient.Impl.Issue.class));
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

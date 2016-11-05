package io.github.hidroh.materialistic.data;

import java.util.concurrent.Executor;

import static org.mockito.Mockito.mock;

public class TestRestServiceFactory implements RestServiceFactory {
    public static final HackerNewsClient.RestService hnRestService =
            mock(HackerNewsClient.RestService.class);
    public static final AlgoliaClient.RestService algoliaRestService =
            mock(AlgoliaClient.RestService.class);
    public static final FeedbackClient.Impl.FeedbackService feedbackService =
            mock(FeedbackClient.Impl.FeedbackService.class);
    public static final ReadabilityClient.Impl.MercuryService mercuryService =
            mock(ReadabilityClient.Impl.MercuryService.class);

    @Override
    public RestServiceFactory rxEnabled(boolean enabled) {
        return this;
    }

    @Override
    public <T> T create(String baseUrl, Class<T> clazz) {
        return create(baseUrl, clazz, null);
    }

    @Override
    public <T> T create(String baseUrl, Class<T> clazz, Executor callbackExecutor) {
        if (clazz.isInstance(hnRestService)) {
            return (T) hnRestService;
        }
        if (clazz.isInstance(algoliaRestService)) {
            return (T) algoliaRestService;
        }
        if (clazz.isInstance(feedbackService)) {
            return (T) feedbackService;
        }
        if (clazz.isInstance(mercuryService)) {
            return (T) mercuryService;
        }
        return mock(clazz);
    }
}

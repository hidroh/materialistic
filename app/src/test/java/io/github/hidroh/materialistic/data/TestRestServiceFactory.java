package io.github.hidroh.materialistic.data;

import static org.mockito.Mockito.mock;

public class TestRestServiceFactory implements RestServiceFactory {
    public static final HackerNewsClient.RestService hnRestService =
            mock(HackerNewsClient.RestService.class);
    public static final AlgoliaClient.RestService algoliaRestService =
            mock(AlgoliaClient.RestService.class);

    @Override
    public <T> T create(String baseUrl, Class<T> clazz) {
        if (clazz.isInstance(hnRestService)) {
            return (T) hnRestService;
        }
        if (clazz.isInstance(algoliaRestService)) {
            return (T) algoliaRestService;
        }
        return mock(clazz);
    }
}

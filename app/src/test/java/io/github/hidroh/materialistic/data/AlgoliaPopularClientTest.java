package io.github.hidroh.materialistic.data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import io.github.hidroh.materialistic.test.ParameterizedRobolectricGradleTestRunner;
import retrofit.Callback;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(ParameterizedRobolectricGradleTestRunner.class)
public class AlgoliaPopularClientTest {
    private final String range;
    @Inject RestServiceFactory factory;
    private ItemManager hackerNewsClient = mock(ItemManager.class);
    private AlgoliaPopularClient client;

    public AlgoliaPopularClientTest(String range) {
        this.range = range;
    }

    @ParameterizedRobolectricGradleTestRunner.Parameters
    public static List<Object[]> provideParameters() {
        return Arrays.asList(
                new Object[]{AlgoliaPopularClient.LAST_24H},
                new Object[]{AlgoliaPopularClient.PAST_WEEK},
                new Object[]{AlgoliaPopularClient.PAST_MONTH},
                new Object[]{AlgoliaPopularClient.PAST_YEAR}
        );
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.algoliaRestService);
        client = new AlgoliaPopularClient(RuntimeEnvironment.application, factory);
        client.mHackerNewsClient = hackerNewsClient;
    }

    @Test
    public void testGetStories() {
        client.getStories(range, mock(ItemManager.ResponseListener.class));
        verify(TestRestServiceFactory.algoliaRestService)
                .searchByMinTimestamp(contains("created_at_i>"), any(Callback.class));
    }

    @Module(
            injects = AlgoliaPopularClientTest.class,
            overrides = true
    )
    static class TestModule {
        @Provides
        @Singleton
        public RestServiceFactory provideRestServiceFactory() {
            return new TestRestServiceFactory();
        }
    }}

package io.github.hidroh.materialistic.data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.DataModule;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class AlgoliaPopularClientTest {
    private final String range;
    @Inject RestServiceFactory factory;
    @Inject @Named(ActivityModule.HN) ItemManager hackerNewsClient;
    private AlgoliaPopularClient client;

    public AlgoliaPopularClientTest(String range) {
        this.range = range;
    }

    @Parameterized.Parameters
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
        reset(TestRestServiceFactory.algoliaRestService);
        ObjectGraph objectGraph = ObjectGraph.create(new TestModule());
        objectGraph.inject(this);
        client = new AlgoliaPopularClient(factory);
        objectGraph.inject(client);
    }

    @Test
    public void testGetStories() {
        when(TestRestServiceFactory.algoliaRestService.searchByMinTimestampRx(any()))
                .thenReturn(Observable.error(new IOException()));
        client.getStories(range, ItemManager.MODE_DEFAULT, mock(ResponseListener.class));
        verify(TestRestServiceFactory.algoliaRestService)
                .searchByMinTimestampRx(contains("created_at_i>"));
    }

    @Module(
            injects = {
                    AlgoliaPopularClientTest.class,
                    AlgoliaPopularClient.class
            },
            overrides = true
    )
    static class TestModule {
        @Provides @Singleton @Named(ActivityModule.HN)
        public ItemManager provideHackerNewsClient() {
            return mock(ItemManager.class);
        }

        @Provides @Singleton @Named(DataModule.MAIN_THREAD)
        public Scheduler provideMainThreadScheduler() {
            return Schedulers.immediate();
        }

        @Provides @Singleton
        public RestServiceFactory provideRestServiceFactory() {
            return new TestRestServiceFactory();
        }
    }
}

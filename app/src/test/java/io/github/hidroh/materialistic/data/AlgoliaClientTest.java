package io.github.hidroh.materialistic.data;

import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class AlgoliaClientTest {
    @Inject RestServiceFactory factory;
    @Inject @Named(ActivityModule.HN) ItemManager hackerNewsClient;
    @Captor ArgumentCaptor<Item[]> getStoriesResponse;
    private AlgoliaClient client;
    @Mock ResponseListener<Item> itemListener;
    @Mock ResponseListener<Item[]> storiesListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        reset(TestRestServiceFactory.algoliaRestService);
        AlgoliaClient.sSortByTime = true;
        ObjectGraph objectGraph = ObjectGraph.create(new TestModule());
        objectGraph.inject(this);
        client = new AlgoliaClient(factory);
        objectGraph.inject(client);
    }

    @Test
    public void testGetItem() {
        client.getItem("1", ItemManager.MODE_DEFAULT, itemListener);
        verify(hackerNewsClient).getItem(eq("1"), eq(ItemManager.MODE_DEFAULT), eq(itemListener));
    }

    @Test
    public void testGetStoriesNoListener() {
        client.getStories("filter", ItemManager.MODE_DEFAULT, null);
        verify(TestRestServiceFactory.algoliaRestService, never()).searchByDateRx(eq("filter"));
    }

    @Test
    public void testGetStoriesSuccess() {
        AlgoliaClient.AlgoliaHits hits = new GsonBuilder().create().fromJson(
                "{\"hits\":[{\"objectID\":\"1\"}]}",
                AlgoliaClient.AlgoliaHits.class);
        when(TestRestServiceFactory.algoliaRestService.searchByDateRx(eq("filter")))
                .thenReturn(Observable.just(hits));
        client.getStories("filter", ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDateRx(eq("filter"));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).hasSize(1);
    }

    @Test
    public void testGetStoriesSuccessSortByPopularity() {
        when(TestRestServiceFactory.algoliaRestService.searchRx(eq("filter")))
                .thenReturn(Observable.error(new IOException()));
        client.sSortByTime = false;
        client.getStories("filter", ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchRx(eq("filter"));
    }

    @Test
    public void testGetStoriesEmpty() {
        AlgoliaClient.AlgoliaHits hits = new GsonBuilder().create().fromJson("{\"hits\":[]}",
                AlgoliaClient.AlgoliaHits.class);
        when(TestRestServiceFactory.algoliaRestService.searchByDateRx(eq("filter")))
                .thenReturn(Observable.just(hits));
        client.getStories("filter", ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDateRx(eq("filter"));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isEmpty();
    }

    @Test
    public void testGetStoriesFailure() {
        when(TestRestServiceFactory.algoliaRestService.searchByDateRx(eq("filter")))
                .thenReturn(Observable.error(new Throwable("message")));
        client.getStories("filter", ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDateRx(eq("filter"));
        verify(storiesListener).onError(eq("message"));
    }

    @Module(
            injects = {
                    AlgoliaClientTest.class,
                    AlgoliaClient.class
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

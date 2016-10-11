package io.github.hidroh.materialistic.data;

import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import io.github.hidroh.materialistic.test.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import rx.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class AlgoliaClientTest {
    @Inject RestServiceFactory factory;
    private ItemManager hackerNewsClient = mock(ItemManager.class);
    @Captor ArgumentCaptor<Item[]> getStoriesResponse;
    private AlgoliaClient client;
    private ResponseListener<Item> itemListener;
    private ResponseListener<Item[]> storiesListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.algoliaRestService);
        client = new AlgoliaClient(RuntimeEnvironment.application, factory);
        client.mHackerNewsClient = hackerNewsClient;
        client.sSortByTime = true;
        itemListener = mock(ResponseListener.class);
        storiesListener = mock(ResponseListener.class);
    }

    @Test
    public void testGetItem() {
        client.getItem("1", ItemManager.MODE_DEFAULT, itemListener);
        verify(hackerNewsClient).getItem(eq("1"), eq(ItemManager.MODE_DEFAULT), eq(itemListener));
    }

    @Test
    public void testGetStoriesNoListener() {
        client.getStories("filter", ItemManager.MODE_DEFAULT, null);
        verify(TestRestServiceFactory.algoliaRestService, never()).searchByDate(eq("filter"));
    }

    @Test
    public void testGetStoriesSuccess() {
        AlgoliaClient.AlgoliaHits hits = new GsonBuilder().create().fromJson(
                "{\"hits\":[{\"objectID\":\"1\"}]}",
                AlgoliaClient.AlgoliaHits.class);
        when(TestRestServiceFactory.algoliaRestService.searchByDate(eq("filter")))
                .thenReturn(Observable.just(hits));
        client.getStories("filter", ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDate(eq("filter"));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).hasSize(1);
    }

    @Test
    public void testGetStoriesSuccessSortByPopularity() {
        when(TestRestServiceFactory.algoliaRestService.search(eq("filter")))
                .thenReturn(Observable.error(new IOException()));
        client.sSortByTime = false;
        client.getStories("filter", ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).search(eq("filter"));
    }

    @Test
    public void testGetStoriesEmpty() {
        AlgoliaClient.AlgoliaHits hits = new GsonBuilder().create().fromJson("{\"hits\":[]}",
                AlgoliaClient.AlgoliaHits.class);
        when(TestRestServiceFactory.algoliaRestService.searchByDate(eq("filter")))
                .thenReturn(Observable.just(hits));
        client.getStories("filter", ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDate(eq("filter"));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isEmpty();
    }

    @Test
    public void testGetStoriesFailure() {
        when(TestRestServiceFactory.algoliaRestService.searchByDate(eq("filter")))
                .thenReturn(Observable.error(new Throwable("message")));
        client.getStories("filter", ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDate(eq("filter"));
        verify(storiesListener).onError(eq("message"));
    }

    @Module(
            injects = AlgoliaClientTest.class,
            overrides = true
    )
    static class TestModule {
        @Provides @Singleton
        public RestServiceFactory provideRestServiceFactory() {
            return new TestRestServiceFactory();
        }
    }
}

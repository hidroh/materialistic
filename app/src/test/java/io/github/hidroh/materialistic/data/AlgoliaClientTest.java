package io.github.hidroh.materialistic.data;

import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class AlgoliaClientTest {
    @Inject RestServiceFactory factory;
    private ItemManager hackerNewsClient = mock(ItemManager.class);
    @Captor ArgumentCaptor<Callback<AlgoliaClient.Item>> getItemCallback;
    @Captor ArgumentCaptor<Callback<AlgoliaClient.AlgoliaHits>> getStoriesCallback;
    @Captor ArgumentCaptor<ItemManager.Item[]> getStoriesResponse;
    private AlgoliaClient client;
    private ResponseListener<ItemManager.Item> itemListener;
    private ResponseListener<ItemManager.Item[]> storiesListener;
    private Call call;

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
        call = mock(Call.class);
        when(TestRestServiceFactory.algoliaRestService.search(anyString())).thenReturn(call);
        when(TestRestServiceFactory.algoliaRestService.searchByDate(anyString())).thenReturn(call);
    }

    @Test
    public void testGetItem() {
        client.getItem("1", itemListener);
        verify(hackerNewsClient).getItem(eq("1"), eq(itemListener));
    }

    @Test
    public void testGetStoriesNoListener() {
        client.getStories("filter", null);
        verify(TestRestServiceFactory.algoliaRestService, never()).searchByDate(eq("filter"));
        verify(call, never()).enqueue(any(Callback.class));
    }

    @Test
    public void testGetStoriesSuccess() {
        client.getStories("filter", storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDate(eq("filter"));
        verify(call).enqueue(getStoriesCallback.capture());
        AlgoliaClient.AlgoliaHits hits = new GsonBuilder().create().fromJson(
                "{\"hits\":[{\"objectID\":\"1\"}]}",
                AlgoliaClient.AlgoliaHits.class);
        getStoriesCallback.getValue().onResponse(Response.success(hits), null);
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).hasSize(1);
    }

    @Test
    public void testGetStoriesSuccessSortByPopularity() {
        client.sSortByTime = false;
        client.getStories("filter", storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).search(eq("filter"));
        verify(call).enqueue(any(Callback.class));
    }

    @Test
    public void testGetStoriesEmpty() {
        client.getStories("filter", storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDate(eq("filter"));
        verify(call).enqueue(getStoriesCallback.capture());
        AlgoliaClient.AlgoliaHits hits = new GsonBuilder().create().fromJson("{\"hits\":[]}",
                AlgoliaClient.AlgoliaHits.class);
        getStoriesCallback.getValue().onResponse(Response.success(hits), null);
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isEmpty();
    }

    @Test
    public void testGetStoriesFailure() {
        client.getStories("filter", storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDate(eq("filter"));
        verify(call).enqueue(getStoriesCallback.capture());
        getStoriesCallback.getValue().onFailure(new Throwable("message"));
        verify(storiesListener).onError(eq("message"));
    }

    @Test
    public void testGetStoriesFailureNoMessage() {
        client.getStories("filter", storiesListener);
        verify(TestRestServiceFactory.algoliaRestService).searchByDate(eq("filter"));
        verify(call).enqueue(getStoriesCallback.capture());
        getStoriesCallback.getValue().onFailure(null);
        verify(storiesListener).onError(eq(""));
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

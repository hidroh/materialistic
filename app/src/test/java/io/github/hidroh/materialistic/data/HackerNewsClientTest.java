package io.github.hidroh.materialistic.data;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class HackerNewsClientTest {
    @Inject RestServiceFactory factory;
    @Captor ArgumentCaptor<Callback<HackerNewsClient.HackerNewsItem>> getItemCallback;
    @Captor ArgumentCaptor<Callback<HackerNewsClient.UserItem>> getUserCallback;
    @Captor ArgumentCaptor<Callback<int[]>> getStoriesCallback;
    @Captor ArgumentCaptor<ItemManager.Item[]> getStoriesResponse;
    private HackerNewsClient client;
    private ResponseListener<ItemManager.Item> itemListener;
    private ResponseListener<UserManager.User> userListener;
    private ResponseListener<ItemManager.Item[]> storiesListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.hnRestService);
        client = new HackerNewsClient(factory);
        itemListener = mock(ResponseListener.class);
        storiesListener = mock(ResponseListener.class);
        userListener = mock(ResponseListener.class);
    }

    @Test
    public void testGetItemNoListener() {
        client.getItem("1", null);
        verify(TestRestServiceFactory.hnRestService, never()).item(anyString(), getItemCallback.capture());
    }

    @Test
    public void testGetItemSuccess() {
        client.getItem("1", itemListener);
        verify(TestRestServiceFactory.hnRestService).item(eq("1"), getItemCallback.capture());
        HackerNewsClient.HackerNewsItem hnItem = mock(HackerNewsClient.HackerNewsItem.class);
        getItemCallback.getValue().success(hnItem, createResponse(200));
        verify(itemListener).onResponse(eq(hnItem));
    }

    @Test
    public void testGetItemFailure() {
        client.getItem("1", itemListener);
        verify(TestRestServiceFactory.hnRestService).item(eq("1"), getItemCallback.capture());
        RetrofitError retrofitError = mock(RetrofitError.class);
        when(retrofitError.getMessage()).thenReturn("message");
        getItemCallback.getValue().failure(retrofitError);
        verify(itemListener).onError(eq("message"));
    }

    @Test
    public void testGetItemFailureNoMessage() {
        client.getItem("1", itemListener);
        verify(TestRestServiceFactory.hnRestService).item(eq("1"), getItemCallback.capture());
        getItemCallback.getValue().failure(null);
        verify(itemListener).onError(eq(""));
    }

    @Test
    public void testGetStoriesNoListener() {
        client.getStories(ItemManager.TOP_FETCH_MODE, null);
        verify(TestRestServiceFactory.hnRestService, never()).topStories(getStoriesCallback.capture());
    }

    @Test
    public void testGetTopStoriesSuccess() {
        client.getStories(ItemManager.TOP_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).topStories(getStoriesCallback.capture());
        getStoriesCallback.getValue().success(new int[]{1, 2}, createResponse(200));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).hasSize(2);
    }

    @Test
    public void testGetNewStoriesNull() {
        client.getStories(ItemManager.NEW_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).newStories(getStoriesCallback.capture());
        getStoriesCallback.getValue().success(null, createResponse(200));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isEmpty();
    }

    @Test
    public void testGetAskEmpty() {
        client.getStories(ItemManager.ASK_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).askStories(getStoriesCallback.capture());
        getStoriesCallback.getValue().success(new int[]{}, createResponse(200));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isEmpty();
    }

    @Test
    public void testGetShowFailure() {
        client.getStories(ItemManager.SHOW_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).showStories(getStoriesCallback.capture());
        RetrofitError retrofitError = mock(RetrofitError.class);
        when(retrofitError.getMessage()).thenReturn("message");
        getStoriesCallback.getValue().failure(retrofitError);
        verify(storiesListener).onError(eq("message"));
    }

    @Test
    public void testGetJobsFailureNoMessage() {
        client.getStories(ItemManager.JOBS_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).jobStories(getStoriesCallback.capture());
        getStoriesCallback.getValue().failure(null);
        verify(storiesListener).onError(eq(""));
    }

    @Test
    public void testGetUserNoListener() {
        client.getUser("username", null);
        verify(TestRestServiceFactory.hnRestService, never()).user(anyString(), getUserCallback.capture());
    }

    @Test
    public void testGetUserSuccess() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"), getUserCallback.capture());
        HackerNewsClient.UserItem hnUser = mock(HackerNewsClient.UserItem.class);
        getUserCallback.getValue().success(hnUser, createResponse(200));
        verify(userListener).onResponse(eq(hnUser));
    }

    @Test
    public void testGetUserNull() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"), getUserCallback.capture());
        getUserCallback.getValue().success(null, createResponse(200));
        verify(userListener).onResponse((UserManager.User) isNull());
    }

    @Test
    public void testGetUserFailure() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"), getUserCallback.capture());
        RetrofitError retrofitError = mock(RetrofitError.class);
        when(retrofitError.getMessage()).thenReturn("message");
        getUserCallback.getValue().failure(retrofitError);
        verify(userListener).onError(eq("message"));
    }

    @Test
    public void testGetUserFailureNoMessage() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"), getUserCallback.capture());
        getUserCallback.getValue().failure(null);
        verify(userListener).onError(eq(""));
    }

    private Response createResponse(int statusCode) {
        return new Response("", statusCode, "", new ArrayList<Header>(), mock(TypedInput.class));
    }

    @Module(
            injects = HackerNewsClientTest.class,
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

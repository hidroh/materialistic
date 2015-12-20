package io.github.hidroh.materialistic.data;

import android.content.ContentResolver;

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
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class HackerNewsClientTest {
    @Inject RestServiceFactory factory;
    @Inject SessionManager sessionManager;
    private HackerNewsClient client;
    private Call call;
    @Captor ArgumentCaptor<ItemManager.Item[]> getStoriesResponse;
    @Captor ArgumentCaptor<Callback> callbackCaptor;
    private ResponseListener<ItemManager.Item> itemListener;
    private ResponseListener<UserManager.User> userListener;
    private ResponseListener<ItemManager.Item[]> storiesListener;
    @Captor ArgumentCaptor<SessionManager.OperationCallbacks> sessionCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.hnRestService);
        reset(sessionManager);
        client = new HackerNewsClient(RuntimeEnvironment.application, factory, sessionManager);
        itemListener = mock(ResponseListener.class);
        storiesListener = mock(ResponseListener.class);
        userListener = mock(ResponseListener.class);
        call = mock(Call.class);
        when(TestRestServiceFactory.hnRestService.item(anyString())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.askStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.topStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.jobStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.newStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.showStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.user(anyString())).thenReturn(call);
    }

    @Test
    public void testGetItemNoListener() {
        client.getItem("1", null);
        verify(TestRestServiceFactory.hnRestService, never()).item(anyString());
    }

    @Test
    public void testGetItemSuccess() {
        client.getItem("1", itemListener);
        verify(TestRestServiceFactory.hnRestService).item(eq("1"));
        verify(call).enqueue(callbackCaptor.capture());
        HackerNewsClient.HackerNewsItem hnItem = mock(HackerNewsClient.HackerNewsItem.class);
        callbackCaptor.getValue().onResponse(Response.success(hnItem), null);
        verify(sessionManager).isViewed(any(ContentResolver.class), eq("1"),
                sessionCallback.capture());
        sessionCallback.getValue().onCheckComplete(false);
        verify(itemListener).onResponse(eq(hnItem));
    }

    @Test
    public void testGetItemFailure() {
        client.getItem("1", itemListener);
        verify(sessionManager).isViewed(any(ContentResolver.class), eq("1"),
                sessionCallback.capture());
        sessionCallback.getValue().onCheckComplete(true);
        verify(TestRestServiceFactory.hnRestService).item(eq("1"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(new Throwable("message"));
        verify(itemListener).onError(eq("message"));
    }

    @Test
    public void testGetItemFailureNoMessage() {
        client.getItem("1", itemListener);
        verify(sessionManager).isViewed(any(ContentResolver.class), eq("1"),
                sessionCallback.capture());
        sessionCallback.getValue().onCheckComplete(true);
        verify(TestRestServiceFactory.hnRestService).item(eq("1"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null);
        verify(itemListener).onError(eq(""));
    }

    @Test
    public void testGetStoriesNoListener() {
        client.getStories(ItemManager.TOP_FETCH_MODE, null);
        verify(TestRestServiceFactory.hnRestService, never()).topStories();
    }

    @Test
    public void testGetTopStoriesSuccess() {
        client.getStories(ItemManager.TOP_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).topStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(Response.success(new int[]{1, 2}), null);
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).hasSize(2);
    }

    @Test
    public void testGetNewStoriesNull() {
        client.getStories(ItemManager.NEW_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).newStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(Response.success(null), null);
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isEmpty();
    }

    @Test
    public void testGetAskEmpty() {
        client.getStories(ItemManager.ASK_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).askStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(Response.success(new int[]{}), null);
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isEmpty();
    }

    @Test
    public void testGetShowFailure() {
        client.getStories(ItemManager.SHOW_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).showStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(new Throwable("message"));
        verify(storiesListener).onError(eq("message"));
    }

    @Test
    public void testGetJobsFailureNoMessage() {
        client.getStories(ItemManager.JOBS_FETCH_MODE, storiesListener);
        verify(TestRestServiceFactory.hnRestService).jobStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null);
        verify(storiesListener).onError(eq(""));
    }

    @Test
    public void testGetUserNoListener() {
        client.getUser("username", null);
        verify(TestRestServiceFactory.hnRestService, never()).user(anyString());
    }

    @Test
    public void testGetUserSuccess() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"));
        verify(call).enqueue(callbackCaptor.capture());
        HackerNewsClient.UserItem hnUser = mock(HackerNewsClient.UserItem.class);
        callbackCaptor.getValue().onResponse(Response.success(hnUser), null);
        verify(userListener).onResponse(eq(hnUser));
    }

    @Test
    public void testGetUserNull() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(Response.success(null), null);
        verify(userListener).onResponse((UserManager.User) isNull());
    }

    @Test
    public void testGetUserFailure() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(new Throwable("message"));
        verify(userListener).onError(eq("message"));
    }

    @Test
    public void testGetUserFailureNoMessage() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null);
        verify(userListener).onError(eq(""));
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

        @Provides
        @Singleton
        public SessionManager provideSessionManager() {
            return mock(SessionManager.class);
        }
    }
}

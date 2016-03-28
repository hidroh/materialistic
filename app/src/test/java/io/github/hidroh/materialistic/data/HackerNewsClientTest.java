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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    @Inject FavoriteManager favoriteManager;
    private HackerNewsClient client;
    private Call call;
    @Captor ArgumentCaptor<Item[]> getStoriesResponse;
    @Captor ArgumentCaptor<Callback> callbackCaptor;
    private ResponseListener<Item> itemListener;
    private ResponseListener<UserManager.User> userListener;
    private ResponseListener<Item[]> storiesListener;
    @Captor ArgumentCaptor<SessionManager.OperationCallbacks> sessionCallback;
    @Captor ArgumentCaptor<FavoriteManager.OperationCallbacks> favoriteCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph.create(new TestModule()).inject(this);
        reset(TestRestServiceFactory.hnRestService);
        reset(sessionManager);
        reset(favoriteManager);
        client = new HackerNewsClient(RuntimeEnvironment.application, factory, sessionManager,
                favoriteManager);
        itemListener = mock(ResponseListener.class);
        storiesListener = mock(ResponseListener.class);
        userListener = mock(ResponseListener.class);
        call = mock(Call.class);
        when(TestRestServiceFactory.hnRestService.item(anyString())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkItem(anyString())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.askStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.topStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.jobStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.newStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.showStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkAskStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkTopStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkJobStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkNewStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkShowStories()).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.user(anyString())).thenReturn(call);
    }

    @Test
    public void testGetItemNoListener() {
        client.getItem("1", ItemManager.MODE_DEFAULT, null);
        verify(TestRestServiceFactory.hnRestService, never()).item(anyString());
    }

    @Test
    public void testGetItemSuccess() {
        client.getItem("1", ItemManager.MODE_DEFAULT, itemListener);
        verify(TestRestServiceFactory.hnRestService).item(eq("1"));
        verify(call).enqueue(callbackCaptor.capture());
        HackerNewsItem hnItem = mock(HackerNewsItem.class);
        callbackCaptor.getValue().onResponse(null, Response.success(hnItem));
        verify(sessionManager).isViewed(any(ContentResolver.class), eq("1"),
                sessionCallback.capture());
        sessionCallback.getValue().onCheckViewedComplete(false);
        verify(favoriteManager).check(any(ContentResolver.class), eq("1"),
                favoriteCallback.capture());
        favoriteCallback.getValue().onCheckComplete(false);
        verify(itemListener).onResponse(eq(hnItem));
    }

    @Test
    public void testGetItemForceNetwork() {
        client.getItem("1", ItemManager.MODE_NETWORK, itemListener);
        verify(TestRestServiceFactory.hnRestService).networkItem(eq("1"));
    }

    @Test
    public void testGetItemForceCache() throws IOException {
        HackerNewsItem hnItem = mock(HackerNewsItem.class);
        when(call.execute()).thenReturn(Response.success(hnItem));
        client.getItem("1", ItemManager.MODE_CACHE, itemListener);
        verify(TestRestServiceFactory.hnRestService).cachedItem(eq("1"));
        verify(call).execute();
        verify(sessionManager).isViewed(any(ContentResolver.class), eq("1"),
                sessionCallback.capture());
        sessionCallback.getValue().onCheckViewedComplete(false);
        verify(favoriteManager).check(any(ContentResolver.class), eq("1"),
                favoriteCallback.capture());
        favoriteCallback.getValue().onCheckComplete(false);
        verify(itemListener).onResponse(eq(hnItem));
    }

    @Test
    public void testGetItemForceCacheUnsatisfiable() throws IOException {
        when(call.execute()).thenThrow(IOException.class);
        client.getItem("1", ItemManager.MODE_CACHE, itemListener);
        verify(TestRestServiceFactory.hnRestService).cachedItem(eq("1"));
        verify(call).execute();
        verify(TestRestServiceFactory.hnRestService).item(eq("1"));
    }

    @Test
    public void testGetItemFailure() {
        client.getItem("1", ItemManager.MODE_DEFAULT, itemListener);
        verify(favoriteManager).check(any(ContentResolver.class), eq("1"),
                favoriteCallback.capture());
        favoriteCallback.getValue().onCheckComplete(true);
        verify(sessionManager).isViewed(any(ContentResolver.class), eq("1"),
                sessionCallback.capture());
        sessionCallback.getValue().onCheckViewedComplete(true);
        verify(TestRestServiceFactory.hnRestService).item(eq("1"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null, new Throwable("message"));
        verify(itemListener).onError(eq("message"));
    }

    @Test
    public void testGetItemFailureNoMessage() {
        client.getItem("1", ItemManager.MODE_DEFAULT, itemListener);
        verify(sessionManager).isViewed(any(ContentResolver.class), eq("1"),
                sessionCallback.capture());
        sessionCallback.getValue().onCheckViewedComplete(true);
        verify(favoriteManager).check(any(ContentResolver.class), eq("1"),
                favoriteCallback.capture());
        favoriteCallback.getValue().onCheckComplete(true);
        verify(TestRestServiceFactory.hnRestService).item(eq("1"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null, null);
        verify(itemListener).onError(eq(""));
    }

    @Test
    public void testGetStoriesNoListener() {
        client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_DEFAULT, null);
        verify(TestRestServiceFactory.hnRestService, never()).topStories();
    }

    @Test
    public void testGetTopStoriesSuccess() {
        client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.hnRestService).topStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(null, Response.success(new int[]{1, 2}));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).hasSize(2);
    }

    @Test
    public void testGetNewStoriesNull() {
        client.getStories(ItemManager.NEW_FETCH_MODE, ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.hnRestService).newStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(null, Response.success(null));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isNullOrEmpty();
    }

    @Test
    public void testGetAskEmpty() {
        client.getStories(ItemManager.ASK_FETCH_MODE, ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.hnRestService).askStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(null, Response.success(new int[]{}));
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isEmpty();
    }

    @Test
    public void testGetShowFailure() {
        client.getStories(ItemManager.SHOW_FETCH_MODE, ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.hnRestService).showStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null, new Throwable("message"));
        verify(storiesListener).onError(eq("message"));
    }

    @Test
    public void testGetJobsFailureNoMessage() {
        client.getStories(ItemManager.JOBS_FETCH_MODE, ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.hnRestService).jobStories();
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null, null);
        verify(storiesListener).onError(eq(""));
    }

    @Test
    public void testGetStoriesForceNetwork() {
        client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkTopStories();

        client.getStories(ItemManager.NEW_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkNewStories();

        client.getStories(ItemManager.ASK_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkAskStories();

        client.getStories(ItemManager.JOBS_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkJobStories();

        client.getStories(ItemManager.SHOW_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkShowStories();
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
        UserItem hnUser = mock(UserItem.class);
        callbackCaptor.getValue().onResponse(null, Response.success(hnUser));
        verify(userListener).onResponse(eq(hnUser));
    }

    @Test
    public void testGetUserNull() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(null, Response.success(null));
        verify(userListener).onResponse((UserManager.User) isNull());
    }

    @Test
    public void testGetUserFailure() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null, new Throwable("message"));
        verify(userListener).onError(eq("message"));
    }

    @Test
    public void testGetUserFailureNoMessage() {
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).user(eq("username"));
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null, null);
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

        @Provides
        @Singleton
        public FavoriteManager provideFavoriteManager() {
            return mock(FavoriteManager.class);
        }
    }
}

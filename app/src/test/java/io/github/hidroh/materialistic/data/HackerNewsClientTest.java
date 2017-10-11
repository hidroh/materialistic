package io.github.hidroh.materialistic.data;

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
import io.github.hidroh.materialistic.DataModule;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class HackerNewsClientTest {
    @Inject RestServiceFactory factory;
    @Inject SessionManager sessionManager;
    @Inject FavoriteManager favoriteManager;
    private HackerNewsClient client;
    @Captor ArgumentCaptor<Item[]> getStoriesResponse;
    @Mock ResponseListener<Item> itemListener;
    @Mock ResponseListener<UserManager.User> userListener;
    @Mock ResponseListener<Item[]> storiesListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectGraph objectGraph = ObjectGraph.create(new TestModule());
        objectGraph.inject(this);
        reset(TestRestServiceFactory.hnRestService);
        reset(sessionManager);
        reset(favoriteManager);
        client = new HackerNewsClient(factory, sessionManager, favoriteManager);
        objectGraph.inject(client);
    }

    @Test
    public void testGetItemNoListener() {
        client.getItem("1", ItemManager.MODE_DEFAULT, null);
        verify(TestRestServiceFactory.hnRestService, never()).itemRx(any());
    }

    @Test
    public void testGetItemSuccess() {
        HackerNewsItem hnItem = mock(HackerNewsItem.class);
        when(TestRestServiceFactory.hnRestService.itemRx(eq("1")))
                .thenReturn(Observable.just(hnItem));
        when(sessionManager.isViewed(eq("1")))
                .thenReturn(Observable.just(false));
        when(favoriteManager.check(eq("1")))
                .thenReturn(Observable.just(false));
        client.getItem("1", ItemManager.MODE_DEFAULT, itemListener);
        verify(TestRestServiceFactory.hnRestService).itemRx(eq("1"));
        verify(sessionManager).isViewed(eq("1"));
        verify(favoriteManager).check(eq("1"));
        verify(itemListener).onResponse(eq(hnItem));
    }

    @Test
    public void testGetItemForceNetwork() {
        client.getItem("1", ItemManager.MODE_NETWORK, itemListener);
        verify(TestRestServiceFactory.hnRestService).networkItemRx(eq("1"));
    }

    @Test
    public void testGetItemForceCache() {
        HackerNewsItem hnItem = mock(HackerNewsItem.class);
        when(TestRestServiceFactory.hnRestService.cachedItemRx(eq("1")))
                .thenReturn(Observable.just(hnItem));
        when(sessionManager.isViewed(eq("1")))
                .thenReturn(Observable.just(false));
        when(favoriteManager.check(eq("1")))
                .thenReturn(Observable.just(false));
        client.getItem("1", ItemManager.MODE_CACHE, itemListener);
        verify(TestRestServiceFactory.hnRestService).cachedItemRx(eq("1"));
        verify(sessionManager).isViewed(eq("1"));
        verify(favoriteManager).check(eq("1"));
        verify(itemListener).onResponse(eq(hnItem));
    }

    @Test
    public void testGetItemForceCacheUnsatisfiable() {
        when(TestRestServiceFactory.hnRestService.cachedItemRx(eq("1")))
                .thenReturn(Observable.error(new IOException()));
        when(TestRestServiceFactory.hnRestService.itemRx(eq("1")))
                .thenReturn(Observable.just(mock(HackerNewsItem.class)));
        client.getItem("1", ItemManager.MODE_CACHE, itemListener);
        verify(TestRestServiceFactory.hnRestService).cachedItemRx(eq("1"));
        verify(TestRestServiceFactory.hnRestService).itemRx(eq("1"));
    }

    @Test
    public void testGetItemFailure() {
        when(TestRestServiceFactory.hnRestService.itemRx(eq("1")))
                .thenReturn(Observable.error(new Throwable("message")));
        when(sessionManager.isViewed(eq("1")))
                .thenReturn(Observable.just(true));
        when(favoriteManager.check(eq("1")))
                .thenReturn(Observable.just(true));
        client.getItem("1", ItemManager.MODE_DEFAULT, itemListener);
        verify(favoriteManager).check(eq("1"));
        verify(sessionManager).isViewed(eq("1"));
        verify(TestRestServiceFactory.hnRestService).itemRx(eq("1"));
        verify(itemListener).onError(eq("message"));
    }

    @Test
    public void testGetItemFailureNoMessage() {
        when(TestRestServiceFactory.hnRestService.itemRx(eq("1")))
                .thenReturn(Observable.error(new Throwable("")));
        when(sessionManager.isViewed(eq("1")))
                .thenReturn(Observable.just(true));
        when(favoriteManager.check(eq("1")))
                .thenReturn(Observable.just(true));
        client.getItem("1", ItemManager.MODE_DEFAULT, itemListener);
        verify(sessionManager).isViewed(eq("1"));
        verify(favoriteManager).check(eq("1"));
        verify(TestRestServiceFactory.hnRestService).itemRx(eq("1"));
        verify(itemListener).onError(eq(""));
    }

    @Test
    public void testGetStoriesNoListener() {
        client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_DEFAULT, null);
        verify(TestRestServiceFactory.hnRestService, never()).topStoriesRx();
    }

    @Test
    public void testGetTopStoriesSuccess() {
        when(TestRestServiceFactory.hnRestService.topStoriesRx())
                .thenReturn(Observable.just(new int[]{1, 2}));
        client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.hnRestService).topStoriesRx();
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).hasSize(2);
    }

    @Test
    public void testGetNewStoriesNull() {
        when(TestRestServiceFactory.hnRestService.newStoriesRx())
                .thenReturn(Observable.just(null));
        client.getStories(ItemManager.NEW_FETCH_MODE, ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.hnRestService).newStoriesRx();
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isNullOrEmpty();
    }

    @Test
    public void testGetAskEmpty() {
        when(TestRestServiceFactory.hnRestService.askStoriesRx())
                .thenReturn(Observable.just(new int[0]));
        client.getStories(ItemManager.ASK_FETCH_MODE, ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.hnRestService).askStoriesRx();
        verify(storiesListener).onResponse(getStoriesResponse.capture());
        assertThat(getStoriesResponse.getValue()).isEmpty();
    }

    @Test
    public void testGetShowFailure() {
        when(TestRestServiceFactory.hnRestService.showStoriesRx())
                .thenReturn(Observable.error(new Throwable("message")));
        client.getStories(ItemManager.SHOW_FETCH_MODE, ItemManager.MODE_DEFAULT, storiesListener);
        verify(TestRestServiceFactory.hnRestService).showStoriesRx();
        verify(storiesListener).onError(eq("message"));
    }

    @Test
    public void testGetStoriesForceNetwork() {
        when(TestRestServiceFactory.hnRestService.networkTopStoriesRx())
                .thenReturn(Observable.just(null));
        client.getStories(ItemManager.TOP_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkTopStoriesRx();

        when(TestRestServiceFactory.hnRestService.networkNewStoriesRx())
                .thenReturn(Observable.just(null));
        client.getStories(ItemManager.NEW_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkNewStoriesRx();

        when(TestRestServiceFactory.hnRestService.networkAskStoriesRx())
                .thenReturn(Observable.just(null));
        client.getStories(ItemManager.ASK_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkAskStoriesRx();

        when(TestRestServiceFactory.hnRestService.networkJobStoriesRx())
                .thenReturn(Observable.just(null));
        client.getStories(ItemManager.JOBS_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkJobStoriesRx();

        when(TestRestServiceFactory.hnRestService.networkShowStoriesRx())
                .thenReturn(Observable.just(null));
        client.getStories(ItemManager.SHOW_FETCH_MODE, ItemManager.MODE_NETWORK, storiesListener);
        verify(TestRestServiceFactory.hnRestService).networkShowStoriesRx();
    }

    @Test
    public void testGetUserNoListener() {
        client.getUser("username", null);
        verify(TestRestServiceFactory.hnRestService, never()).userRx(any());
    }

    @Test
    public void testGetUserSuccess() {
        UserItem hnUser = mock(UserItem.class);
        when(TestRestServiceFactory.hnRestService.userRx(eq("username")))
                .thenReturn(Observable.just(hnUser));
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).userRx(eq("username"));
        verify(userListener).onResponse(eq(hnUser));
    }

    @Test
    public void testGetUserNull() {
        when(TestRestServiceFactory.hnRestService.userRx(eq("username")))
                .thenReturn(Observable.just(null));
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).userRx(eq("username"));
        verify(userListener).onResponse(isNull());
    }

    @Test
    public void testGetUserFailure() {
        when(TestRestServiceFactory.hnRestService.userRx(eq("username")))
                .thenReturn(Observable.error(new Throwable("message")));
        client.getUser("username", userListener);
        verify(TestRestServiceFactory.hnRestService).userRx(eq("username"));
        verify(userListener).onError(eq("message"));
    }

    @Module(
            injects = {
                    HackerNewsClientTest.class,
                    HackerNewsClient.class
            },
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

        @Provides @Singleton @Named(DataModule.IO_THREAD)
        public Scheduler provideIoScheduler() {
            return Schedulers.immediate();
        }

        @Provides @Singleton @Named(DataModule.MAIN_THREAD)
        public Scheduler provideMainThreadScheduler() {
            return Schedulers.immediate();
        }
    }
}

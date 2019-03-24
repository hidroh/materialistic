/*
 * Copyright (c) 2016 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic.data;

import android.accounts.Account;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;

import java.io.IOException;

import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.test.InMemoryCache;
import io.github.hidroh.materialistic.test.TestRunner;
import io.github.hidroh.materialistic.test.shadow.ShadowWebView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("unchecked")
@Config(shadows = {ShadowWebView.class}, sdk = 18)
@RunWith(TestRunner.class)
public class ItemSyncAdapterTest {
    private TestItemSyncAdapter adapter;
    private SharedPreferences syncPreferences;
    private @Captor ArgumentCaptor<Callback<HackerNewsItem>> callbackCapture;
    private ReadabilityClient readabilityClient = mock(ReadabilityClient.class);
    private ServiceController<ItemSyncService> serviceController;
    private ItemSyncService service;
    private @Captor ArgumentCaptor<ReadabilityClient.Callback> readabilityCallbackCaptor;
    private SyncScheduler syncScheduler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        reset(TestRestServiceFactory.hnRestService);
        reset(readabilityClient);
        serviceController = Robolectric.buildService(ItemSyncService.class);
        service = serviceController.create().get();
        setNetworkType(ConnectivityManager.TYPE_WIFI);
        PreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_saved_item_sync), true)
                .putBoolean(service.getString(R.string.pref_offline_comments), true)
                .apply();
        adapter = new TestItemSyncAdapter(service, new TestRestServiceFactory(), readabilityClient);
        syncPreferences = service.getSharedPreferences(
                service.getPackageName() +
                        SyncDelegate.SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
        syncScheduler = new SyncScheduler();
    }

    @Test
    public void testSyncDisabled() {
        PreferenceManager.getDefaultSharedPreferences(service)
                .edit().clear().apply();
        syncScheduler.scheduleSync(service, "1");
        assertNull(ShadowContentResolver.getStatus(createSyncAccount(),
                SyncContentProvider.PROVIDER_AUTHORITY));
    }

    @Test
    public void testSyncEnabledCached() throws IOException {
        HackerNewsItem hnItem = mock(HackerNewsItem.class);
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(hnItem));
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);

        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);

        // cache hit, should not try network or defer
        verify(TestRestServiceFactory.hnRestService).cachedItem(any());
        verify(TestRestServiceFactory.hnRestService, never()).networkItem(any());
        assertThat(syncPreferences.getAll()).isEmpty();
    }

    @Test
    public void testSyncEnabledNonWifi() throws IOException {
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenThrow(IOException.class);
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);

        setNetworkType(ConnectivityManager.TYPE_MOBILE);
        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);

        // should defer
        assertThat(syncPreferences.getAll()).isNotEmpty();
    }

    @Test
    public void testSyncEnabledAnyConnection() throws IOException {
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenThrow(IOException.class);
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkItem(any())).thenReturn(call);

        PreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putString(service.getString(R.string.pref_offline_data),
                        service.getString(R.string.offline_data_default))
                .apply();
        setNetworkType(ConnectivityManager.TYPE_MOBILE);
        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);

        // should try cache, then network
        verify(TestRestServiceFactory.hnRestService).cachedItem(any());
        verify(TestRestServiceFactory.hnRestService).networkItem(any());
        assertThat(syncPreferences.getAll()).isEmpty();
    }

    @Test
    public void testSyncEnabledWifi() throws IOException {
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenThrow(IOException.class);
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkItem(any())).thenReturn(call);

        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);

        // should try cache before network
        verify(TestRestServiceFactory.hnRestService).cachedItem(any());
        verify(TestRestServiceFactory.hnRestService).networkItem(any());
        assertThat(syncPreferences.getAll()).isEmpty();

        // on network response should try children
        verify(call).enqueue(callbackCapture.capture());
        HackerNewsItem item = mock(HackerNewsItem.class);
        when(item.getKids()).thenReturn(new long[]{2L, 3L});
        callbackCapture.getValue().onResponse(null, Response.success(item));
        verify(TestRestServiceFactory.hnRestService, times(3)).cachedItem(any());
    }

    @Test
    public void testSyncChildrenDisabled() throws IOException {
        HackerNewsItem item = mock(HackerNewsItem.class);
        when(item.getKids()).thenReturn(new long[]{2L, 3L});
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(item));
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);

        PreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_comments), false)
                .apply();
        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);

        // should not sync children
        verify(TestRestServiceFactory.hnRestService).cachedItem(any());
    }

    @Test
    public void testSyncDeferred() throws IOException {
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenThrow(IOException.class);
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkItem(any())).thenReturn(call);

        syncPreferences.edit().putBoolean("1", true).putBoolean("2", true).apply();
        syncScheduler.scheduleSync(service, null);
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);
        ShadowContentResolver.Status syncStatus = ShadowContentResolver.getStatus(
                new Account("Materialistic", BuildConfig.APPLICATION_ID),
                SyncContentProvider.PROVIDER_AUTHORITY);
        assertThat(syncStatus.syncRequests).isEqualTo(3); // original + 2 deferred
    }

    @Test
    public void testSyncReadabilityDisabled() throws IOException {
        HackerNewsItem item = new TestHnItem(1L) {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getRawUrl() {
                return "http://example.com";
            }
        };
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(item));
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);

        PreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_readability), false)
                .apply();
        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);

        verify(TestRestServiceFactory.hnRestService).cachedItem(any());
        verify(readabilityClient, never()).parse(any(), any(), any());
    }

    @Test
    public void testSyncReadability() throws IOException {
        HackerNewsItem item = new TestHnItem(1L) {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getRawUrl() {
                return "http://example.com";
            }
        };
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(item));
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);

        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);

        verify(TestRestServiceFactory.hnRestService).cachedItem(any());
        verify(readabilityClient).parse(any(), eq("http://example.com"), any());
    }

    @Test
    public void testSyncReadabilityNoWifi() throws IOException {
        HackerNewsItem item = new TestHnItem(1L) {
            @Override
            public boolean isStoryType() {
                return true;
            }
        };
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(item));
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);

        setNetworkType(ConnectivityManager.TYPE_MOBILE);
        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);

        verify(readabilityClient, never()).parse(any(), any(), any());
    }

    @Test
    public void testSyncReadabilityNotStory() throws IOException {
        HackerNewsItem item = new TestHnItem(1L) {
            @Override
            public boolean isStoryType() {
                return false;
            }
        };
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(item));
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);

        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);

        verify(TestRestServiceFactory.hnRestService).cachedItem(any());
        verify(readabilityClient, never()).parse(any(), any(), any());
    }

    @Test
    public void testSyncWebCacheEmptyUrl() {
        new FavoriteManager(new InMemoryCache(), Schedulers.immediate(),
                mock(MaterialisticDatabase.SavedStoriesDao.class))
                .add(service, new Favorite("1", null, "title", System.currentTimeMillis()));
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).isNullOrEmpty();
    }

    @Test
    public void testSyncWebCache() throws IOException {
        ShadowWebView.lastGlobalLoadedUrl = null;
        PreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_article), true)
                .apply();

        HackerNewsItem item = new TestHnItem(1L) {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getUrl() {
                return "http://example.com";
            }
        };
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(item));
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);

        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).contains("http://example.com");
    }

    @Test
    public void testSyncWebCacheDisabled() throws IOException {
        ShadowWebView.lastGlobalLoadedUrl = null;
        PreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_article), false)
                .apply();
        HackerNewsItem item = new TestHnItem(1L) {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getRawUrl() {
                return "http://example.com";
            }
        };
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(item));
        when(TestRestServiceFactory.hnRestService.cachedItem(any())).thenReturn(call);

        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).isNullOrEmpty();
    }

    @Test
    public void testNotification() throws IOException {
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(new TestHnItem(1L) {
            @Override
            public boolean isStoryType() {
                return true;
            }

            @Override
            public String getRawUrl() {
                return "http://example.com";
            }

            @Override
            public long[] getKids() {
                return new long[]{2L, 3L};
            }
        }));
        when(TestRestServiceFactory.hnRestService.cachedItem(eq("1"))).thenReturn(call);
        Call<HackerNewsItem> kid1Call = mock(Call.class);
        when(kid1Call.execute()).thenReturn(Response.success(new TestHnItem(2L) {
            @Override
            public boolean isStoryType() {
                return false;
            }
        }));
        when(TestRestServiceFactory.hnRestService.cachedItem(eq("2"))).thenReturn(kid1Call);
        Call<HackerNewsItem> kid2Call = mock(Call.class);
        when(kid2Call.execute()).thenThrow(IOException.class);
        when(TestRestServiceFactory.hnRestService.cachedItem(eq("3"))).thenReturn(kid2Call);
        when(TestRestServiceFactory.hnRestService.networkItem(eq("3"))).thenReturn(kid2Call);

        PreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_notification), true)
                .apply();
        syncScheduler.scheduleSync(service, "1");
        adapter.onPerformSync(mock(Account.class), getLastSyncExtras(), null, null, null);
        verify(readabilityClient).parse(any(), eq("http://example.com"),
                readabilityCallbackCaptor.capture());
        readabilityCallbackCaptor.getValue().onResponse("");

        ShadowNotificationManager notificationManager = shadowOf((NotificationManager) service
                .getSystemService(Context.NOTIFICATION_SERVICE));
        ShadowNotification shadowNotification = shadowOf(notificationManager.getNotification(1));
        assertThat(shadowNotification.getProgress()).isEqualTo(3); // self + kid 1 + readability
        assertThat(shadowNotification.getMax()).isEqualTo(104); // self + 2 kids + readability + web

        shadowOf(adapter.syncDelegate.mWebView).getWebChromeClient()
                .onProgressChanged(adapter.syncDelegate.mWebView, 100);

        verify(kid2Call).enqueue(callbackCapture.capture());
        callbackCapture.getValue().onFailure(null, null);
        assertThat(notificationManager.getAllNotifications()).isEmpty();
    }

    @Test
    public void testBindService() {
        assertNotNull(service.onBind(null));
    }

    @Test
    public void testWifiChange() {
        setNetworkType(ConnectivityManager.TYPE_MOBILE);
        new ItemSyncWifiReceiver()
                .onReceive(service, new Intent(ConnectivityManager.CONNECTIVITY_ACTION));
        assertThat(ShadowContentResolver.getStatus(createSyncAccount(),
                SyncContentProvider.PROVIDER_AUTHORITY, true).syncRequests).isEqualTo(0);

        setNetworkType(ConnectivityManager.TYPE_WIFI);
        new ItemSyncWifiReceiver().onReceive(service, new Intent());
        assertThat(ShadowContentResolver.getStatus(createSyncAccount(),
                SyncContentProvider.PROVIDER_AUTHORITY, true).syncRequests).isEqualTo(0);

        setNetworkType(ConnectivityManager.TYPE_WIFI);
        new ItemSyncWifiReceiver()
                .onReceive(service, new Intent(ConnectivityManager.CONNECTIVITY_ACTION));
        assertThat(ShadowContentResolver.getStatus(createSyncAccount(),
                SyncContentProvider.PROVIDER_AUTHORITY, true).syncRequests).isGreaterThan(0);
    }

    @After
    public void tearDown() {
        serviceController.destroy();
    }

    private void setNetworkType(int type) {
        shadowOf((ConnectivityManager) service.getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null, type, 0, true, NetworkInfo.State.CONNECTED));
    }

    private Bundle getLastSyncExtras() {
        return ShadowContentResolver.getStatus(createSyncAccount(),
                SyncContentProvider.PROVIDER_AUTHORITY).syncExtras;
    }

    @NonNull
    private Account createSyncAccount() {
        return new Account("Materialistic", BuildConfig.APPLICATION_ID);
    }

    private static class TestItemSyncAdapter extends ItemSyncAdapter {

        SyncDelegate syncDelegate;

        TestItemSyncAdapter(Context context, RestServiceFactory factory, ReadabilityClient readabilityClient) {
            super(context, factory, readabilityClient);
        }

        @NonNull
        @Override
        SyncDelegate createSyncDelegate() {
            syncDelegate = super.createSyncDelegate();
            return syncDelegate;
        }
    }
}

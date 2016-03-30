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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.util.ServiceController;

import java.io.IOException;

import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.ShadowWebView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("unchecked")
@Config(shadows = {ShadowSupportPreferenceManager.class, ShadowWebView.class})
@RunWith(RobolectricGradleTestRunner.class)
public class ItemSyncAdapterTest {
    private ItemSyncAdapter adapter;
    private SharedPreferences syncPreferences;
    private @Captor ArgumentCaptor<Callback<HackerNewsItem>> callbackCapture;
    private ServiceController<ItemSyncService> serviceController;
    private ItemSyncService service;
    private ReadabilityClient readabilityClient = mock(ReadabilityClient.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        reset(TestRestServiceFactory.hnRestService);
        reset(readabilityClient);
        serviceController = Robolectric.buildService(ItemSyncService.class);
        service = serviceController.attach().create().get();
        setNetworkType(ConnectivityManager.TYPE_WIFI);
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_saved_item_sync), true)
                .putBoolean(service.getString(R.string.pref_offline_comments), true)
                .commit();
        adapter = new ItemSyncAdapter(service, new TestRestServiceFactory(), readabilityClient);
        syncPreferences = service.getSharedPreferences(
                service.getPackageName() +
                        ItemSyncAdapter.SYNC_PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    @Test
    public void testSyncDisabled() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(service)
                .edit().clear().commit();
        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        // should not try cache or network or defer
        verify(TestRestServiceFactory.hnRestService, never()).cachedItem(anyString());
        verify(TestRestServiceFactory.hnRestService, never()).networkItem(anyString());
        assertThat(syncPreferences.getAll()).isEmpty();
    }

    @Test
    public void testSyncEnabledCached() throws IOException {
        HackerNewsItem hnItem = mock(HackerNewsItem.class);
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(hnItem));
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);

        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        // cache hit, should not try network or defer
        verify(TestRestServiceFactory.hnRestService).cachedItem(anyString());
        verify(TestRestServiceFactory.hnRestService, never()).networkItem(anyString());
        assertThat(syncPreferences.getAll()).isEmpty();
    }

    @Test
    public void testSyncEnabledNonWifi() throws IOException {
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenThrow(IOException.class);
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);

        setNetworkType(ConnectivityManager.TYPE_MOBILE);
        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        // should try cache, then defer
        verify(TestRestServiceFactory.hnRestService).cachedItem(anyString());
        verify(TestRestServiceFactory.hnRestService, never()).networkItem(anyString());
        assertThat(syncPreferences.getAll()).isNotEmpty();
    }

    @Test
    public void testSyncEnabledAnyConnection() throws IOException {
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenThrow(IOException.class);
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkItem(anyString())).thenReturn(call);

        ShadowSupportPreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putString(service.getString(R.string.pref_offline_data),
                        service.getString(R.string.offline_data_default))
                .commit();
        setNetworkType(ConnectivityManager.TYPE_MOBILE);
        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        // should try cache, then network
        verify(TestRestServiceFactory.hnRestService).cachedItem(anyString());
        verify(TestRestServiceFactory.hnRestService).networkItem(anyString());
        assertThat(syncPreferences.getAll()).isEmpty();
    }

    @Test
    public void testSyncEnabledWifi() throws IOException {
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenThrow(IOException.class);
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkItem(anyString())).thenReturn(call);

        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        // should try cache before network
        verify(TestRestServiceFactory.hnRestService).cachedItem(anyString());
        verify(TestRestServiceFactory.hnRestService).networkItem(anyString());
        assertThat(syncPreferences.getAll()).isEmpty();

        // on network response should try children
        verify(call).enqueue(callbackCapture.capture());
        HackerNewsItem item = mock(HackerNewsItem.class);
        when(item.getKids()).thenReturn(new long[]{2L, 3L});
        callbackCapture.getValue().onResponse(null, Response.success(item));
        verify(TestRestServiceFactory.hnRestService, times(3)).cachedItem(anyString());
    }

    @Test
    public void testSyncChildrenDisabled() throws IOException {
        HackerNewsItem item = mock(HackerNewsItem.class);
        when(item.getKids()).thenReturn(new long[]{2L, 3L});
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(item));
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);

        ShadowSupportPreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_comments), false)
                .commit();
        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        // should not sync children
        verify(TestRestServiceFactory.hnRestService).cachedItem(anyString());
    }

    @Test
    public void testSyncDeferred() throws IOException {
        Call<HackerNewsItem> call = mock(Call.class);
        when(call.execute()).thenThrow(IOException.class);
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);
        when(TestRestServiceFactory.hnRestService.networkItem(anyString())).thenReturn(call);

        syncPreferences.edit().putBoolean("1", true).putBoolean("2", true).apply();
        adapter.onPerformSync(mock(Account.class), new Bundle(), null, null, null);
        verify(TestRestServiceFactory.hnRestService, times(2)).cachedItem(anyString());
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
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);

        ShadowSupportPreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_readability), false)
                .commit();
        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        verify(TestRestServiceFactory.hnRestService).cachedItem(anyString());
        verify(readabilityClient, never()).parse(anyString(), anyString(),
                any(ReadabilityClient.Callback.class));
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
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);

        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        verify(TestRestServiceFactory.hnRestService).cachedItem(anyString());
        verify(readabilityClient).parse(anyString(), eq("http://example.com"),
                any(ReadabilityClient.Callback.class));
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
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);

        setNetworkType(ConnectivityManager.TYPE_MOBILE);
        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        verify(TestRestServiceFactory.hnRestService).cachedItem(anyString());
        verify(readabilityClient, never()).parse(anyString(), anyString(),
                any(ReadabilityClient.Callback.class));
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
        when(TestRestServiceFactory.hnRestService.cachedItem(anyString())).thenReturn(call);

        Bundle bundle = new Bundle();
        bundle.putString(ItemSyncAdapter.EXTRA_ID, "1");
        adapter.onPerformSync(mock(Account.class), bundle, null, null, null);

        verify(TestRestServiceFactory.hnRestService).cachedItem(anyString());
        verify(readabilityClient, never()).parse(anyString(), anyString(),
                any(ReadabilityClient.Callback.class));
    }

    @Test
    public void testSyncWebCacheEmptyUrl() {
        ItemSyncAdapter.saveWebCache(RuntimeEnvironment.application, null);
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).isNullOrEmpty();
    }

    @Test
    public void testSyncWebCache() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_article), true)
                .commit();
        ItemSyncAdapter.saveWebCache(RuntimeEnvironment.application, "http://example.com");
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).contains("http://example.com");
    }

    @Test
    public void testSyncWebCacheNonWifi() {
        setNetworkType(ConnectivityManager.TYPE_MOBILE);
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_article), true)
                .commit();
        ItemSyncAdapter.saveWebCache(RuntimeEnvironment.application, "http://example.com");
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).isNullOrEmpty();
    }

    @Test
    public void testSyncWebCacheDisabled() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(service)
                .edit()
                .putBoolean(service.getString(R.string.pref_offline_article), false)
                .commit();
        ItemSyncAdapter.saveWebCache(RuntimeEnvironment.application, "http://example.com");
        assertThat(ShadowWebView.getLastGlobalLoadedUrl()).isNullOrEmpty();
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
        assertFalse(ShadowContentResolver.isSyncActive(Application.createSyncAccount(),
                MaterialisticProvider.PROVIDER_AUTHORITY));

        setNetworkType(ConnectivityManager.TYPE_WIFI);
        new ItemSyncWifiReceiver().onReceive(service, new Intent());
        assertFalse(ShadowContentResolver.isSyncActive(Application.createSyncAccount(),
                MaterialisticProvider.PROVIDER_AUTHORITY));

        setNetworkType(ConnectivityManager.TYPE_WIFI);
        new ItemSyncWifiReceiver()
                .onReceive(service, new Intent(ConnectivityManager.CONNECTIVITY_ACTION));
        assertTrue(ShadowContentResolver.isSyncActive(Application.createSyncAccount(),
                MaterialisticProvider.PROVIDER_AUTHORITY));
    }

    private void setNetworkType(int type) {
        shadowOf((ConnectivityManager) service.getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null, type, 0, true, true));
    }

    @After
    public void tearDown() {
        serviceController.destroy();
    }
}

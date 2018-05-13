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

package io.github.hidroh.materialistic;

import android.content.Context;
import android.net.TrafficStats;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;
import javax.net.SocketFactory;

import dagger.Module;
import dagger.Provides;
import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.FileDownloader;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.RestServiceFactory;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

@Module(library = true, complete = false)
class NetworkModule {
    private static final String TAG_OK_HTTP = "OkHttp";
    private static final long CACHE_SIZE = 20 * 1024 * 1024; // 20 MB

    @Provides @Singleton
    public RestServiceFactory provideRestServiceFactory(Call.Factory callFactory) {
        return new RestServiceFactory.Impl(callFactory);
    }

    @Provides @Singleton
    public Call.Factory provideCallFactory(Context context) {
        return new OkHttpClient.Builder()
                .socketFactory(new SocketFactory() {
                    private SocketFactory mDefaultFactory = SocketFactory.getDefault();

                    @Override
                    public Socket createSocket() throws IOException {
                        Socket socket = mDefaultFactory.createSocket();
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }

                    @Override
                    public Socket createSocket(String host, int port) throws IOException {
                        Socket socket = mDefaultFactory.createSocket(host, port);
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }

                    @Override
                    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
                        Socket socket = mDefaultFactory.createSocket(host, port, localHost, localPort);
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }

                    @Override
                    public Socket createSocket(InetAddress host, int port) throws IOException {
                        Socket socket = mDefaultFactory.createSocket(host, port);
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }

                    @Override
                    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
                        Socket socket = mDefaultFactory.createSocket(address, port, localAddress, localPort);
                        TrafficStats.setThreadStatsTag(1);
                        return socket;
                    }
                })
                .cache(new Cache(context.getApplicationContext().getCacheDir(), CACHE_SIZE))
                .addNetworkInterceptor(new CacheOverrideNetworkInterceptor())
                .addInterceptor(new ConnectionAwareInterceptor(context))
                .addInterceptor(new LoggingInterceptor())
                .followRedirects(false)
                .build();
    }

    @Provides @Singleton
    public FileDownloader provideFileDownloader(Context context, Call.Factory callFactory) {
        return new FileDownloader(context, callFactory);
    }

    static class ConnectionAwareInterceptor implements Interceptor {

        static final Map<String, String> CACHE_ENABLED_HOSTS = new HashMap<>();
        static {
            CACHE_ENABLED_HOSTS.put(HackerNewsClient.HOST,
                    RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M);
            CACHE_ENABLED_HOSTS.put(AlgoliaClient.HOST,
                    RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M);
            CACHE_ENABLED_HOSTS.put(ReadabilityClient.HOST,
                    RestServiceFactory.CACHE_CONTROL_MAX_AGE_24H);
        }
        private final Context mContext;

        ConnectionAwareInterceptor(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            boolean forceCache = CACHE_ENABLED_HOSTS.containsKey(request.url().host()) &&
                    !AppUtils.hasConnection(mContext);
            return chain.proceed(forceCache ?
                    request.newBuilder()
                            .cacheControl(CacheControl.FORCE_CACHE)
                            .build() :
                    request);
        }
    }

    static class CacheOverrideNetworkInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);
            if (!ConnectionAwareInterceptor.CACHE_ENABLED_HOSTS
                    .containsKey(request.url().host())) {
                return response;
            } else {
                return response.newBuilder()
                        .header("Cache-Control",
                                ConnectionAwareInterceptor.CACHE_ENABLED_HOSTS
                                        .get(request.url().host()))
                        .build();
            }
        }
    }

    static class LoggingInterceptor implements Interceptor {
        private final Interceptor debugInterceptor = new HttpLoggingInterceptor(
                message -> Log.d(TAG_OK_HTTP, message))
                .setLevel(BuildConfig.DEBUG ?
                        HttpLoggingInterceptor.Level.BODY :
                        HttpLoggingInterceptor.Level.NONE);

        @Override
        public Response intercept(Chain chain) throws IOException {
            return debugInterceptor.intercept(chain);
        }
    }
}

package io.github.hidroh.materialistic.accounts;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
public class UserServicesClientTest {
    private UserServices userServices;
    @Captor ArgumentCaptor<Callback> callbackCaptor;
    private Call call;
    private Response.Builder responseBuilder = new Response.Builder()
            .protocol(Protocol.HTTP_2)
            .request(new Request.Builder().url("http://example.com").build());

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        call = mock(Call.class);
        OkHttpClient client = mock(OkHttpClient.class);
        when(client.newCall(any(Request.class))).thenReturn(call);
        userServices = new UserServicesClient(client);
    }

    @Test
    public void testSuccess() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.login("username", "password", callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(responseBuilder
                .code(HttpURLConnection.HTTP_MOVED_TEMP).build());
        verify(callback).onDone(eq(true));
    }

    @Test
    public void testFailed() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.login("username", "password", callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(responseBuilder
                .code(HttpURLConnection.HTTP_OK).build());
        verify(callback).onDone(eq(false));
    }

    @Test
    public void testError() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.login("username", "password", callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null, null);
        verify(callback).onDone(eq(false));
    }
}

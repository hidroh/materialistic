package io.github.hidroh.materialistic.accounts;

import android.accounts.Account;

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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAccountManager;

import java.io.IOException;
import java.net.HttpURLConnection;

import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.Preferences;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private Account account;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        call = mock(Call.class);
        OkHttpClient client = mock(OkHttpClient.class);
        when(client.newCall(any(Request.class))).thenReturn(call);
        userServices = new UserServicesClient(client);
        Preferences.setUsername(RuntimeEnvironment.application, "username");
        account = new Account("username", BuildConfig.APPLICATION_ID);
        ShadowAccountManager.get(RuntimeEnvironment.application)
                .addAccountExplicitly(account, "password", null);
    }

    @Test
    public void testLoginSuccess() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.login("username", "password", false, callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(responseBuilder
                .code(HttpURLConnection.HTTP_MOVED_TEMP).build());
        verify(callback).onDone(eq(true));
    }

    @Test
    public void testRegisterFailed() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.login("username", "password", true, callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(responseBuilder
                .code(HttpURLConnection.HTTP_OK).build());
        verify(callback).onDone(eq(false));
    }

    @Test
    public void testLoginError() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.login("username", "password", false, callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null, null);
        verify(callback).onError();
    }

    @Test
    public void testVoteSuccess() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.voteUp(RuntimeEnvironment.application, "1", callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(responseBuilder
                .code(HttpURLConnection.HTTP_MOVED_TEMP).build());
        verify(callback).onDone(eq(true));
    }

    @Test
    public void testVoteFailed() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.voteUp(RuntimeEnvironment.application, "1", callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(responseBuilder
                .code(HttpURLConnection.HTTP_OK).build());
        verify(callback).onDone(eq(false));
    }

    @Test
    public void testVoteError() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.voteUp(RuntimeEnvironment.application, "1", callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(null, null);
        verify(callback).onError();
    }

    @Test
    public void testVoteNoMatchingAccount() throws IOException {
        Preferences.setUsername(RuntimeEnvironment.application, "another");
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.voteUp(RuntimeEnvironment.application, "1", callback);
        verify(call, never()).enqueue(any(Callback.class));
        verify(callback).onDone(eq(false));
    }

    @Test
    public void testCommentSuccess() throws IOException {
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.reply(RuntimeEnvironment.application, "1", "reply", callback);
        verify(call).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(responseBuilder
                .code(HttpURLConnection.HTTP_MOVED_TEMP).build());
        verify(callback).onDone(eq(true));
    }

    @Test
    public void testCommentNotLoggedIn() throws IOException {
        Preferences.setUsername(RuntimeEnvironment.application, null);
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.reply(RuntimeEnvironment.application, "1", "reply", callback);
        verify(call, never()).enqueue(any(Callback.class));
        verify(callback).onDone(eq(false));
    }

    @Test
    public void testVoteNoAccount() throws IOException {
        ShadowAccountManager.get(RuntimeEnvironment.application)
                .removeAccount(account, null, null);
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.voteUp(RuntimeEnvironment.application, "1", callback);
        verify(call, never()).enqueue(any(Callback.class));
        verify(callback).onDone(eq(false));
    }

}

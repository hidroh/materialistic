package io.github.hidroh.materialistic.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.net.HttpURLConnection;

import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.Preferences;
import io.github.hidroh.materialistic.test.TestRunner;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import rx.schedulers.Schedulers;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(TestRunner.class)
public class UserServicesClientTest {
    private UserServices userServices;
    private Call call;
    private Response.Builder responseBuilder = createResponseBuilder();
    private Account account;
    @Captor ArgumentCaptor<Throwable> throwableCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        call = mock(Call.class);
        Call.Factory callFactory = mock(Call.Factory.class);
        when(callFactory.newCall(any(Request.class))).thenReturn(call);
        userServices = new UserServicesClient(callFactory, Schedulers.immediate());
        Preferences.setUsername(RuntimeEnvironment.application, "username");
        account = new Account("username", BuildConfig.APPLICATION_ID);
        AccountManager.get(RuntimeEnvironment.application)
                .addAccountExplicitly(account, "password", null);
    }

    @Test
    public void testLoginSuccess() throws IOException {
        when(call.execute()).thenReturn(responseBuilder
                .message("")
                .code(HttpURLConnection.HTTP_MOVED_TEMP)
                .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.login("username", "password", false, callback);
        verify(callback).onDone(eq(true));
    }

    @Test
    public void testRegisterFailed() throws IOException {
        when(call.execute()).thenReturn(responseBuilder
                .body(ResponseBody.create(MediaType.parse("text/html"), "<body>Message<br/></body>"))
                .message("")
                .code(HttpURLConnection.HTTP_OK)
                .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.login("username", "password", true, callback);
        verify(callback).onError(throwableCaptor.capture());
        //noinspection ThrowableResultOfMethodCallIgnored
        assertThat(throwableCaptor.getValue().getMessage()).contains("Message");
    }

    @Test
    public void testLoginError() throws IOException {
        when(call.execute()).thenThrow(new IOException());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.login("username", "password", false, callback);
        verify(callback).onError(any(Throwable.class));
    }

    @Test
    public void testVoteSuccess() throws IOException {
        when(call.execute()).thenReturn(responseBuilder
                .message("")
                .code(HttpURLConnection.HTTP_MOVED_TEMP)
                .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        assertTrue(userServices.voteUp(RuntimeEnvironment.application, "1", callback));
        verify(callback).onDone(eq(true));
    }

    @Test
    public void testVoteFailed() throws IOException {
        when(call.execute()).thenReturn(responseBuilder
                .message("")
                .code(HttpURLConnection.HTTP_OK)
                .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        assertTrue(userServices.voteUp(RuntimeEnvironment.application, "1", callback));
        verify(callback).onDone(eq(false));
    }

    @Test
    public void testVoteError() throws IOException {
        when(call.execute()).thenThrow(new IOException());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        assertTrue(userServices.voteUp(RuntimeEnvironment.application, "1", callback));
        verify(callback).onError(any(Throwable.class));
    }

    @Test
    public void testVoteNoMatchingAccount() {
        Preferences.setUsername(RuntimeEnvironment.application, "another");
        UserServices.Callback callback = mock(UserServices.Callback.class);
        assertFalse(userServices.voteUp(RuntimeEnvironment.application, "1", callback));
        verify(call, never()).enqueue(any(Callback.class));
    }

    @Test
    public void testCommentSuccess() throws IOException {
        when(call.execute()).thenReturn(responseBuilder
                .message("")
                .code(HttpURLConnection.HTTP_MOVED_TEMP)
                .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.reply(RuntimeEnvironment.application, "1", "reply", callback);
        verify(callback).onDone(eq(true));
    }

    @Test
    public void testCommentNotLoggedIn() {
        Preferences.setUsername(RuntimeEnvironment.application, null);
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.reply(RuntimeEnvironment.application, "1", "reply", callback);
        verify(call, never()).enqueue(any(Callback.class));
        verify(callback).onDone(eq(false));
    }

    @Test
    public void testVoteNoAccount() {
        AccountManager.get(RuntimeEnvironment.application)
                .removeAccount(account, null, null);
        UserServices.Callback callback = mock(UserServices.Callback.class);
        assertFalse(userServices.voteUp(RuntimeEnvironment.application, "1", callback));
        verify(call, never()).enqueue(any(Callback.class));
    }

    @Test
    public void testSubmitNoAccount() {
        AccountManager.get(RuntimeEnvironment.application)
                .removeAccount(account, null, null);
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "content", true, callback);
        verify(call, never()).enqueue(any(Callback.class));
        verify(callback).onDone(eq(false));
    }

    @Test
    public void testSubmitSuccess() throws IOException {
        when(call.execute())
                .thenReturn(createResponseBuilder()
                        .body(ResponseBody.create(MediaType.parse("text/html"),
                                "<input \"name\"=\"fnid\" value=\"unique\">"))
                        .message("")
                        .code(HttpURLConnection.HTTP_OK)
                        .build())
                .thenReturn(createResponseBuilder()
                        .message("")
                        .code(HttpURLConnection.HTTP_MOVED_TEMP)
                        .header("location", "newest")
                        .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "content", false, callback);
        verify(call, times(2)).execute();
        verify(callback).onDone(eq(true));
    }

    @Test
    public void testSubmitDuplicate() throws IOException {
        when(call.execute())
                .thenReturn(createResponseBuilder()
                        .body(ResponseBody.create(MediaType.parse("text/html"),
                                "<input \"name\"=\"fnid\" value=\"unique\">"))
                        .message("")
                        .code(HttpURLConnection.HTTP_OK)
                        .build())
                .thenReturn(createResponseBuilder()
                        .message("")
                        .code(HttpURLConnection.HTTP_MOVED_TEMP)
                        .header("location", "item?id=1234")
                        .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "url", true, callback);
        verify(call, times(2)).execute();
        verify(callback).onError(isA(UserServices.Exception.class));
    }

    @Test
    public void testSubmitError() throws IOException {
        when(call.execute())
                .thenReturn(createResponseBuilder()
                        .body(ResponseBody.create(MediaType.parse("text/html"),
                                "<input \"name\"=\"fnid\" value=\"unique\">"))
                        .message("")
                        .code(HttpURLConnection.HTTP_OK)
                        .build())
                .thenReturn(createResponseBuilder()
                        .message("")
                        .code(HttpURLConnection.HTTP_OK)
                        .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "url", true, callback);
        verify(call, times(2)).execute();
        verify(callback).onError(any(Throwable.class));
    }

    @Test
    public void testSubmitNetworkError() throws IOException {
        when(call.execute())
                .thenReturn(createResponseBuilder()
                        .body(ResponseBody.create(MediaType.parse("text/html"),
                                "<input \"name\"=\"fnid\" value=\"unique\">"))
                        .message("")
                        .code(HttpURLConnection.HTTP_OK)
                        .build())
                .thenThrow(new IOException());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "url", true, callback);
        verify(call, times(2)).execute();
        verify(callback).onError(any(Throwable.class));
    }

    @Test
    public void testSubmitParsingNoInput() throws IOException {
        when(call.execute()).thenReturn(createResponseBuilder()
                .body(ResponseBody.create(MediaType.parse("text/html"), ""))
                .message("")
                .code(HttpURLConnection.HTTP_OK)
                .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "url", true, callback);
        verify(callback).onError(any(Throwable.class));
    }

    @Test
    public void testSubmitParsingNoFnid() throws IOException {
        when(call.execute()).thenReturn(createResponseBuilder()
                .body(ResponseBody.create(MediaType.parse("text/html"),
                        "<input \"name\"=\"hiddenfield\" value=\"unique\">"))
                .message("")
                .code(HttpURLConnection.HTTP_OK)
                .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "url", true, callback);
        verify(callback).onError(any(Throwable.class));
    }

    @Test
    public void testSubmitParsingNoFnidValue() throws IOException {
        when(call.execute()).thenReturn(createResponseBuilder()
                .body(ResponseBody.create(MediaType.parse("text/html"),
                        "<input \"name\"=\"fnid\">"))
                .message("")
                .code(HttpURLConnection.HTTP_OK)
                .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "url", true, callback);
        verify(callback).onError(any(Throwable.class));
    }

    @Test
    public void testSubmitLoginFailed() throws IOException {
        when(call.execute()).thenReturn(createResponseBuilder()
                .message("")
                .code(HttpURLConnection.HTTP_MOVED_TEMP)
                .build());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "url", true, callback);
        verify(callback).onError(any(Throwable.class));
    }

    @Test
    public void testSubmitLoginError() throws IOException {
        when(call.execute()).thenThrow(new IOException());
        UserServices.Callback callback = mock(UserServices.Callback.class);
        userServices.submit(RuntimeEnvironment.application, "title", "url", true, callback);
        verify(callback).onError(any(Throwable.class));
    }

    private Response.Builder createResponseBuilder() {
        return new Response.Builder()
                .protocol(Protocol.HTTP_2)
                .request(new Request.Builder().url("http://example.com")
                        .build());
    }
}

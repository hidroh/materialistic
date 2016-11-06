package io.github.hidroh.materialistic.accounts;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Intent;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import io.github.hidroh.materialistic.test.TestRunner;
import org.robolectric.RuntimeEnvironment;

import io.github.hidroh.materialistic.BuildConfig;
import io.github.hidroh.materialistic.LoginActivity;

import static org.assertj.android.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

@RunWith(TestRunner.class)
public class AccountAuthenticatorTest {
    private AccountAuthenticator authenticator;

    @Before
    public void setUp() {
        authenticator = new AccountAuthenticator(RuntimeEnvironment.application);
    }

    @Test
    public void testAddAccount() throws NetworkErrorException {
        Bundle actual = authenticator.addAccount(mock(AccountAuthenticatorResponse.class),
                BuildConfig.APPLICATION_ID, null, null, null);
        assertThat(actual).hasKey(AccountManager.KEY_INTENT);
        Intent actualIntent = actual.getParcelable(AccountManager.KEY_INTENT);
        assertThat(actualIntent)
                .hasComponent(RuntimeEnvironment.application, LoginActivity.class)
                .hasExtra(LoginActivity.EXTRA_ADD_ACCOUNT, true);
    }

    @Test
    public void testUnimplemented() throws NetworkErrorException {
        assertNull(authenticator.editProperties(null, null));
        assertNull(authenticator.confirmCredentials(null, null, null));
        assertNull(authenticator.getAuthToken(null, null, null, null));
        assertNull(authenticator.getAuthTokenLabel(null));
        assertNull(authenticator.updateCredentials(null, null, null, null));
        assertNull(authenticator.hasFeatures(null, null, null));
    }
}

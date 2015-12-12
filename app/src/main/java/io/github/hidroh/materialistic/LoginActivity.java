package io.github.hidroh.materialistic;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;

public class LoginActivity extends AccountAuthenticatorActivity {
    @Inject UserServices mUserServices;
    @Inject AccountManager mAccountManager;
    private View mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        final TextInputLayout usernameLayout = (TextInputLayout)
                findViewById(R.id.textinput_username);
        final TextInputLayout passwordLayout = (TextInputLayout)
                findViewById(R.id.textinput_password);
        final EditText username = (EditText) findViewById(R.id.edittext_username);
        final EditText password = (EditText) findViewById(R.id.edittext_password);
        mLoginButton = findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usernameLayout.setErrorEnabled(false);
                passwordLayout.setErrorEnabled(false);
                if (username.length() == 0) {
                    usernameLayout.setError(getString(R.string.username_required));
                }
                if (password.length() == 0) {
                    passwordLayout.setError(getString(R.string.password_required));
                }
                if (username.length() == 0 || password.length() == 0) {
                    return;
                }
                mLoginButton.setEnabled(false);
                login(username.getText().toString(), password.getText().toString());
            }
        });
    }

    @Override
    protected boolean isDialogTheme() {
        return true;
    }

    private void login(final String username, final String password) {
        mUserServices.login(username, password,
                new UserServices.Callback() {
                    @Override
                    public void onDone(boolean successful) {
                        mLoginButton.setEnabled(true);
                        if (successful) {
                            addAccount(username, password);
                            Toast.makeText(LoginActivity.this,
                                    getString(R.string.welcome, username), Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    R.string.login_failed, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onError() {
                        Toast.makeText(LoginActivity.this,
                                R.string.login_failed, Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void addAccount(String username, String password) {
        Account account = new Account(username, BuildConfig.APPLICATION_ID);
        mAccountManager.addAccountExplicitly(account, password, null);
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, username);
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, BuildConfig.APPLICATION_ID);
        setAccountAuthenticatorResult(bundle);
        Preferences.setUsername(this, username);
        finish();
    }
}

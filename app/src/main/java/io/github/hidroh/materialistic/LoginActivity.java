/*
 * Copyright (c) 2015 Ha Duy Trung
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import com.google.android.material.textfield.TextInputLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;

public class LoginActivity extends AccountAuthenticatorActivity {
    public static final String EXTRA_ADD_ACCOUNT = LoginActivity.class.getName() + ".EXTRA_ADD_ACCOUNT";
    @Inject UserServices mUserServices;
    @Inject AccountManager mAccountManager;
    private View mLoginButton;
    private View mRegisterButton;
    private TextInputLayout mUsernameLayout;
    private TextInputLayout mPasswordLayout;
    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private String mUsername;
    private String mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String username = Preferences.getUsername(this);
        boolean addAccount = getIntent().getBooleanExtra(EXTRA_ADD_ACCOUNT, false);
        setContentView(R.layout.activity_login);
        mUsernameLayout = (TextInputLayout) findViewById(R.id.textinput_username);
        mPasswordLayout = (TextInputLayout) findViewById(R.id.textinput_password);
        mUsernameEditText = (EditText) findViewById(R.id.edittext_username);
        mLoginButton = findViewById(R.id.login_button);
        mRegisterButton = findViewById(R.id.register_button);
        if (!addAccount && !TextUtils.isEmpty(username)) {
            setTitle(R.string.re_enter_password);
            mUsernameEditText.setText(username);
            mRegisterButton.setVisibility(View.GONE);
        }
        mPasswordEditText = (EditText) findViewById(R.id.edittext_password);
        mLoginButton.setOnClickListener(v -> {
            if (!validate()) {
                return;
            }
            mLoginButton.setEnabled(false);
            mRegisterButton.setEnabled(false);
            login(mUsernameEditText.getText().toString(),
                    mPasswordEditText.getText().toString(),
                    false);
        });
        mRegisterButton.setOnClickListener(v -> {
            if (!validate()) {
                return;
            }
            mLoginButton.setEnabled(false);
            mRegisterButton.setEnabled(false);
            login(mUsernameEditText.getText().toString().trim(),
                    mPasswordEditText.getText().toString().trim(),
                    true);
        });
    }

    @Override
    protected boolean isDialogTheme() {
        return true;
    }

    private boolean validate() {
        mUsernameLayout.setErrorEnabled(false);
        mPasswordLayout.setErrorEnabled(false);
        if (mUsernameEditText.length() == 0) {
            mUsernameLayout.setError(getString(R.string.username_required));
        }
        if (mPasswordEditText.length() == 0) {
            mPasswordLayout.setError(getString(R.string.password_required));
        }
        return mUsernameEditText.length() > 0 && mPasswordEditText.length() > 0;
    }

    private void login(String username, String password, boolean createAccount) {
        mUsername = username;
        mPassword = password;
        mUserServices.login(username, password, createAccount, new LoginCallback(this));
    }

    void onLoggedIn(boolean successful, String errorMessage) {
        mLoginButton.setEnabled(true);
        mRegisterButton.setEnabled(true);
        if (successful) {
            addAccount(mUsername, mPassword);
            Toast.makeText(this, getString(R.string.welcome, mUsername), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, TextUtils.isEmpty(errorMessage) ?
                    getString(R.string.login_failed) :
                    errorMessage,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void addAccount(String username, String password) {
        Account account = new Account(username, BuildConfig.APPLICATION_ID);
        mAccountManager.addAccountExplicitly(account, password, null);
        mAccountManager.setPassword(account, password); // for re-login with updated password
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, username);
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, BuildConfig.APPLICATION_ID);
        setAccountAuthenticatorResult(bundle);
        Preferences.setUsername(this, username);
        finish();
    }

    static class LoginCallback extends UserServices.Callback {
        private final WeakReference<LoginActivity> mLoginActivity;

        LoginCallback(LoginActivity loginActivity) {
            mLoginActivity = new WeakReference<>(loginActivity);
        }

        @Override
        public void onDone(boolean successful) {
            if (mLoginActivity.get() != null && !mLoginActivity.get().isActivityDestroyed()) {
                mLoginActivity.get().onLoggedIn(successful, null);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (mLoginActivity.get() != null && !mLoginActivity.get().isActivityDestroyed()) {
                mLoginActivity.get().onLoggedIn(false, throwable != null ? throwable.getMessage() : null);
            }
        }
    }
}

package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.EditText;

public class LoginActivity extends InjectableActivity {
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
        final View loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
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
                loginButton.setEnabled(false);
                // TODO
            }
        });
    }

    @Override
    protected boolean isDialogTheme() {
        return true;
    }
}

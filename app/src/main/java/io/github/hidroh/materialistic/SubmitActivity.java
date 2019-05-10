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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.annotation.Synthetic;

public class SubmitActivity extends InjectableActivity {
    private static final String HN_GUIDELINES_URL = "https://news.ycombinator.com/newsguidelines.html";
    private static final String STATE_SUBJECT = "state:subject";
    private static final String STATE_TEXT = "state:text";
    // matching title url without any trailing text
    private static final String REGEX_FUZZY_URL = "(.*)((http|https)://[^\\s]*)$";
    @Inject UserServices mUserServices;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    @Synthetic TextView mTitleEditText;
    private TextView mContentEditText;
    private TextInputLayout mTitleLayout;
    private TextInputLayout mContentLayout;
    private boolean mSending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setStatusBarColor(getWindow(), ContextCompat.getColor(this, R.color.blackT12));
        setContentView(R.layout.activity_submit);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        mTitleLayout = (TextInputLayout) findViewById(R.id.textinput_title);
        mContentLayout = (TextInputLayout) findViewById(R.id.textinput_content);
        mTitleEditText = (TextView) findViewById(R.id.edittext_title);
        mContentEditText = (TextView) findViewById(R.id.edittext_content);
        String text, subject;
        if (savedInstanceState == null) {
            subject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
            text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        } else {
            subject = savedInstanceState.getString(STATE_SUBJECT);
            text = savedInstanceState.getString(STATE_TEXT);
        }
        mTitleEditText.setText(subject);
        mContentEditText.setText(text);
        if (TextUtils.isEmpty(subject)) {
            if (isUrl(text)) {
                WebView webView = new WebView(this);
                webView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onReceivedTitle(WebView view, String title) {
                        if (mTitleEditText.length() == 0) {
                            mTitleEditText.setText(title);
                        }
                    }
                });
                webView.loadUrl(text);
            } else if (!TextUtils.isEmpty(text)) {
                extractUrl(text);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_submit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_send).setEnabled(!mSending);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.menu_send) {
            if (!validate()) {
                return true;
            }
            final boolean isUrl = isUrl(mContentEditText.getText().toString());
            mAlertDialogBuilder
                    .init(SubmitActivity.this)
                    .setMessage(isUrl ? R.string.confirm_submit_url :
                            R.string.confirm_submit_question)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> submit(isUrl))
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
                    .show();
            return true;
        }
        if (item.getItemId() == R.id.menu_guidelines) {
            WebView webView = new WebView(this);
            webView.loadUrl(HN_GUIDELINES_URL);
            mAlertDialogBuilder
                    .init(this)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SUBJECT, mTitleEditText.getText().toString());
        outState.putString(STATE_TEXT, mContentEditText.getText().toString());
    }

    @Override
    public void onBackPressed() {
        mAlertDialogBuilder
                .init(this)
                .setMessage(mSending ? R.string.confirm_no_waiting : R.string.confirm_no_submit)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> SubmitActivity.super.onBackPressed())
                .show();
    }

    private boolean validate() {
        mTitleLayout.setErrorEnabled(false);
        mContentLayout.setErrorEnabled(false);
        if (mTitleEditText.length() == 0) {
            mTitleLayout.setError(getString(R.string.title_required));
        }
        if (mContentEditText.length() == 0) {
            mContentLayout.setError(getString(R.string.url_text_required));
        }
        return mTitleEditText.length() > 0 && mContentEditText.length() > 0;
    }

    private void submit(boolean isUrl) {
        toggleControls(true);
        Toast.makeText(this, R.string.sending, Toast.LENGTH_SHORT).show();
        mUserServices.submit(this, mTitleEditText.getText().toString(),
                mContentEditText.getText().toString(), isUrl, new SubmitCallback(this));
    }

    @Synthetic
    void onSubmitted(Boolean successful) {
        if (successful == null) {
            toggleControls(false);
            Toast.makeText(this, R.string.submit_failed, Toast.LENGTH_SHORT).show();
        } else if (successful) {
            Toast.makeText(this, R.string.submit_successful, Toast.LENGTH_SHORT).show();
            if (!isFinishing()) {
                Intent intent = new Intent(this, NewActivity.class);
                intent.putExtra(NewActivity.EXTRA_REFRESH, true);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent); // TODO should go to profile instead?
                finish();
            }
        } else if (!isFinishing()) {
            AppUtils.showLogin(this, mAlertDialogBuilder);
        }
    }

    @Synthetic
    void onError(int message, Uri data) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if (data != null) {
            startActivity(new Intent(Intent.ACTION_VIEW).setData(data));
        }
    }

    private boolean isUrl(String text) {
        try {
            new URL(text); // try parsing
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    private void extractUrl(String text) {
        Matcher matcher = Pattern.compile(REGEX_FUZZY_URL).matcher(text);
        if (matcher.find() && matcher.groupCount() >= 3) { // group 1: title, group 2: url, group 3: scheme
            mTitleEditText.setText(trimTitle(matcher.group(1).trim()));
            mContentEditText.setText(matcher.group(2));
        }
    }

    @NonNull
    private String trimTitle(String title) {
        int lastIndex = title.length() - 1;
        while (lastIndex >= 0) {
            char c = title.charAt(lastIndex);
            if (c == ' ' || c == ':' || c == '-') {
                lastIndex--;
            } else {
                break;
            }
        }
        return lastIndex >= 0 ? title.substring(0, lastIndex + 1) : "";
    }

    private void toggleControls(boolean sending) {
        if (isFinishing()) {
            return;
        }
        mSending = sending;
        mTitleEditText.setEnabled(!sending);
        mContentEditText.setEnabled(!sending);
        supportInvalidateOptionsMenu();
    }

    static class SubmitCallback extends UserServices.Callback {
        private final WeakReference<SubmitActivity> mSubmitActivity;

        @Synthetic
        SubmitCallback(SubmitActivity submitActivity) {
            mSubmitActivity = new WeakReference<>(submitActivity);
        }

        @Override
        public void onDone(boolean successful) {
            if (mSubmitActivity.get() != null && !mSubmitActivity.get().isActivityDestroyed()) {
                mSubmitActivity.get().onSubmitted(successful);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (mSubmitActivity.get() != null && !mSubmitActivity.get().isActivityDestroyed()) {
                if (throwable instanceof UserServices.Exception) {
                    UserServices.Exception e = (UserServices.Exception) throwable;
                    mSubmitActivity.get().onError(e.message, e.data);
                } else {
                    mSubmitActivity.get().onSubmitted(null);
                }
            }
        }
    }
}

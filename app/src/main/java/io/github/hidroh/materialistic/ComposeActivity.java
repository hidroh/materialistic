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

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;

public class ComposeActivity extends InjectableActivity {
    public static final String EXTRA_PARENT_ID = ComposeActivity.class.getName() + ".EXTRA_PARENT_ID";
    public static final String EXTRA_PARENT_TEXT = ComposeActivity.class.getName() + ".EXTRA_PARENT_TEXT";
    private static final String HN_FORMAT_DOC_URL = "https://news.ycombinator.com/formatdoc";
    private static final String FORMAT_QUOTE = "> %s\n\n";
    private static final String PARAGRAPH_QUOTE = "\n\n> ";
    private static final String PARAGRAPH_BREAK_REGEX = "[\\n]{2,}";
    @Inject UserServices mUserServices;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    private EditText mEditText;
    private String mParentText;
    private String mQuoteText;
    private String mParentId;
    private boolean mSending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParentId = getIntent().getStringExtra(EXTRA_PARENT_ID);
        if (TextUtils.isEmpty(mParentId)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_compose);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        mEditText = (EditText) findViewById(R.id.edittext_body);
        findViewById(R.id.empty).setOnClickListener(v -> mEditText.requestFocus());
        findViewById(R.id.empty).setOnLongClickListener(v -> {
            mEditText.requestFocus();
            return mEditText.performLongClick();
        });
        TextView guidelines = (TextView) findViewById(R.id.guidelines);
        guidelines.setText(Html.fromHtml(getString(R.string.formatting_guidelines)));
        guidelines.setOnClickListener(v -> {
            WebView webView = new WebView(ComposeActivity.this);
            webView.loadUrl(HN_FORMAT_DOC_URL);
            mAlertDialogBuilder
                    .init(ComposeActivity.this)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
        mParentText = getIntent().getStringExtra(EXTRA_PARENT_TEXT);
        final TextView toggle = (TextView) findViewById(R.id.toggle);
        if (TextUtils.isEmpty(mParentText)) {
            toggle.setVisibility(View.GONE);
        } else {
            final TextView textView = (TextView) findViewById(R.id.text);
            AppUtils.setTextWithLinks(textView, mParentText);
            toggle.setOnClickListener(v -> {
                if (textView.getVisibility() == View.VISIBLE) {
                    toggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                            R.drawable.ic_expand_more_white_24dp, 0);
                    textView.setVisibility(View.GONE);

                } else {
                    toggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                            R.drawable.ic_expand_less_white_24dp, 0);
                    textView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_quote).setVisible(!mSending && !TextUtils.isEmpty(mParentText));
        menu.findItem(R.id.menu_send).setEnabled(!mSending);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_send) {
            if (mEditText.length() == 0) {
                Toast.makeText(this, R.string.comment_required, Toast.LENGTH_SHORT).show();
                return false;
            } else {
                send();
                return true;
            }
        }
        if (item.getItemId() == R.id.menu_quote) {
            mEditText.getEditableText().insert(0, createQuote());
        }
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mEditText.length() == 0) {
            super.onBackPressed();
            return;
        }
        mAlertDialogBuilder
                .init(this)
                .setMessage(mSending ? R.string.confirm_no_waiting : R.string.confirm_discard)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        ComposeActivity.super.onBackPressed())
                .show();
    }

    private void send() {
        toggleControls(true);
        Toast.makeText(this, R.string.sending, Toast.LENGTH_SHORT).show();
        mUserServices.reply(this, mParentId, mEditText.getText().toString(),
                new ComposeCallback(this));
    }

    private void onSent(Boolean successful) {
        if (successful == null) {
            Toast.makeText(this, R.string.comment_failed, Toast.LENGTH_SHORT).show();
            toggleControls(false);
        } else if (successful) {
            Toast.makeText(this, R.string.comment_successful, Toast.LENGTH_SHORT).show();
            if (!isFinishing()) {
                finish();
                // TODO refresh parent
            }
        } else {
            if (!isFinishing()) {
                AppUtils.showLogin(this, mAlertDialogBuilder);
            }
            toggleControls(false);
        }
    }

    private String createQuote() {
        if (mQuoteText == null) {
            mQuoteText = String.format(FORMAT_QUOTE, Html.fromHtml(mParentText)
                    .toString()
                    .trim()
                    .replaceAll(PARAGRAPH_BREAK_REGEX, PARAGRAPH_QUOTE));
        }
        return mQuoteText;
    }

    private void toggleControls(boolean sending) {
        if (isFinishing()) {
            return;
        }
        mSending = sending;
        mEditText.setEnabled(!sending);
        supportInvalidateOptionsMenu();
    }

    private static class ComposeCallback extends UserServices.Callback {
        private final WeakReference<ComposeActivity> mComposeActivity;

        public ComposeCallback(ComposeActivity composeActivity) {
            mComposeActivity = new WeakReference<>(composeActivity);
        }

        @Override
        public void onDone(boolean successful) {
            if (mComposeActivity.get() != null && !mComposeActivity.get().isActivityDestroyed()) {
                mComposeActivity.get().onSent(successful);
            }
        }

        @Override
        public void onError(int message, Uri data) {
            if (mComposeActivity.get() != null && !mComposeActivity.get().isActivityDestroyed()) {
                mComposeActivity.get().onSent(null);
            }
        }
    }
}

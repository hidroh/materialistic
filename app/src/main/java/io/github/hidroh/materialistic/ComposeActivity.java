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

import android.content.Context;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
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
import io.github.hidroh.materialistic.annotation.Synthetic;

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
        AppUtils.setStatusBarColor(getWindow(), ContextCompat.getColor(this, R.color.blackT12));
        setContentView(R.layout.activity_compose);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_HOME_AS_UP);
        mEditText = (EditText) findViewById(R.id.edittext_body);
        if (savedInstanceState == null) {
            mEditText.setText(Preferences.getDraft(this, mParentId));
        }
        findViewById(R.id.empty).setOnClickListener(v -> mEditText.requestFocus());
        findViewById(R.id.empty).setOnLongClickListener(v -> {
            mEditText.requestFocus();
            return mEditText.performLongClick();
        });
        mParentText = getIntent().getStringExtra(EXTRA_PARENT_TEXT);
        if (!TextUtils.isEmpty(mParentText)) {
            findViewById(R.id.quote).setVisibility(View.VISIBLE);
            final TextView toggle = (TextView) findViewById(R.id.toggle);
            final TextView textView = (TextView) findViewById(R.id.text);
            AppUtils.setTextWithLinks(textView, AppUtils.fromHtml(mParentText));
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
        menu.findItem(R.id.menu_save_draft).setEnabled(!mSending);
        menu.findItem(R.id.menu_discard_draft).setEnabled(!mSending);
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
        if (item.getItemId() == R.id.menu_save_draft) {
            Preferences.saveDraft(this, mParentId, mEditText.getText().toString());
            return true;
        }
        if (item.getItemId() == R.id.menu_discard_draft) {
            Preferences.deleteDraft(this, mParentId);
            return true;
        }
        if (item.getItemId() == R.id.menu_guidelines) {
            WebView webView = new WebView(ComposeActivity.this);
            webView.loadUrl(HN_FORMAT_DOC_URL);
            mAlertDialogBuilder
                    .init(ComposeActivity.this)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mEditText.length() == 0 || mSending ||
                TextUtils.equals(Preferences.getDraft(this, mParentId), mEditText.getText().toString())) {
            super.onBackPressed();
            return;
        }
        mAlertDialogBuilder
                .init(this)
                .setMessage(R.string.confirm_save_draft)
                .setNegativeButton(android.R.string.no, (dialog, which) ->
                        ComposeActivity.super.onBackPressed())
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        Preferences.saveDraft(this, mParentId, mEditText.getText().toString());
                        ComposeActivity.super.onBackPressed();
                })
                .show();
    }

    private void send() {
        String content = mEditText.getText().toString();
        Preferences.saveDraft(this, mParentId, content);
        toggleControls(true);
        Toast.makeText(this, R.string.sending, Toast.LENGTH_SHORT).show();
        mUserServices.reply(this, mParentId, content, new ComposeCallback(this, mParentId));
    }

    @Synthetic
    void onSent(Boolean successful) {
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
            mQuoteText = String.format(FORMAT_QUOTE, AppUtils.fromHtml(mParentText)
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

    static class ComposeCallback extends UserServices.Callback {
        private final WeakReference<ComposeActivity> mComposeActivity;
        private final Context mAppContext;
        private final String mParentId;

        @Synthetic
        ComposeCallback(ComposeActivity composeActivity, String parentId) {
            mComposeActivity = new WeakReference<>(composeActivity);
            mAppContext = composeActivity.getApplicationContext();
            mParentId = parentId;
        }

        @Override
        public void onDone(boolean successful) {
            Preferences.deleteDraft(mAppContext, mParentId);
            if (mComposeActivity.get() != null && !mComposeActivity.get().isActivityDestroyed()) {
                mComposeActivity.get().onSent(successful);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (mComposeActivity.get() != null && !mComposeActivity.get().isActivityDestroyed()) {
                mComposeActivity.get().onSent(null);
            }
        }
    }
}

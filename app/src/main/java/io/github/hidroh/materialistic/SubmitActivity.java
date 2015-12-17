package io.github.hidroh.materialistic;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

import io.github.hidroh.materialistic.accounts.UserServices;

public class SubmitActivity extends InjectableActivity {
    private static final String HN_GUIDELINES_URL = "https://news.ycombinator.com/newsguidelines.html";
    @Inject UserServices mUserServices;
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    private TextView mTitleEditText;
    private TextView mContentEditText;
    private TextInputLayout mTitleLayout;
    private TextInputLayout mContentLayout;
    private boolean mSending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        mTitleLayout = (TextInputLayout) findViewById(R.id.textinput_title);
        mContentLayout = (TextInputLayout) findViewById(R.id.textinput_content);
        mTitleEditText = (TextView) findViewById(R.id.edittext_title);
        mContentEditText = (TextView) findViewById(R.id.edittext_content);
        mTitleEditText.setText(getIntent().getStringExtra(Intent.EXTRA_SUBJECT));
        mContentEditText.setText(getIntent().getStringExtra(Intent.EXTRA_TEXT));
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
            final boolean isUrl = isUrl();
            mAlertDialogBuilder
                    .init(SubmitActivity.this)
                    .setMessage(isUrl ? R.string.confirm_submit_url :
                            R.string.confirm_submit_question)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            submit(isUrl);
                        }
                    })
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
    public void onBackPressed() {
        mAlertDialogBuilder
                .init(this)
                .setMessage(mSending ? R.string.confirm_no_waiting : R.string.confirm_no_submit)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SubmitActivity.super.onBackPressed();
                    }
                })
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
                mContentEditText.getText().toString(), isUrl, new UserServices.Callback() {
                    @Override
                    public void onDone(boolean successful) {
                        toggleControls(false);
                        if (successful) {
                            Toast.makeText(SubmitActivity.this, R.string.submit_successful,
                                    Toast.LENGTH_SHORT)
                                    .show();
                            if (!isFinishing()) {
                                Intent intent = new Intent(SubmitActivity.this, NewActivity.class);
                                intent.putExtra(NewActivity.EXTRA_REFRESH, true);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivity(intent); // TODO should go to profile instead?
                                finish();
                            }
                        } else if (!isFinishing()) {
                            AppUtils.showLogin(SubmitActivity.this, mAlertDialogBuilder);
                        }

                    }

                    @Override
                    public void onError() {
                        toggleControls(false);
                        Toast.makeText(SubmitActivity.this,
                                R.string.submit_failed, Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private boolean isUrl() {
        try {
            new URL(mContentEditText.getText().toString()); // try parsing
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
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
}

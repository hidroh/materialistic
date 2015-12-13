package io.github.hidroh.materialistic;

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

import javax.inject.Inject;

public class ComposeActivity extends InjectableActivity {
    public static final String EXTRA_PARENT_ID = ComposeActivity.class.getName() + ".EXTRA_PARENT_ID";
    public static final String EXTRA_PARENT_TEXT = ComposeActivity.class.getName() + ".EXTRA_PARENT_TEXT";
    private static final String HN_FORMAT_DOC_URL = "https://news.ycombinator.com/formatdoc";
    private static final String FORMAT_QUOTE = "> %s\n\n";
    private static final String PARAGRAPH_QUOTE = "\n\n> ";
    private static final String PARAGRAPH_BREAK_REGEX = "[\\n]{2,}";
    @Inject AlertDialogBuilder mAlertDialogBuilder;
    private EditText mEditText;
    private String mParentText;
    private String mQuoteText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        mEditText = (EditText) findViewById(R.id.edittext_body);
        findViewById(R.id.empty).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.requestFocus();
            }
        });
        findViewById(R.id.empty).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mEditText.requestFocus();
                return mEditText.performLongClick();
            }
        });
        TextView guidelines = (TextView) findViewById(R.id.guidelines);
        guidelines.setText(Html.fromHtml(getString(R.string.formatting_guidelines)));
        guidelines.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = new WebView(ComposeActivity.this);
                webView.loadUrl(HN_FORMAT_DOC_URL);
                mAlertDialogBuilder
                        .init(ComposeActivity.this)
                        .setView(webView)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });
        mParentText = getIntent().getStringExtra(EXTRA_PARENT_TEXT);
        final TextView toggle = (TextView) findViewById(R.id.toggle);
        if (TextUtils.isEmpty(mParentText)) {
            toggle.setVisibility(View.GONE);
        } else {
            final TextView textView = (TextView) findViewById(R.id.text);
            AppUtils.setTextWithLinks(textView, mParentText);
            toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (textView.getVisibility() == View.VISIBLE) {
                        toggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.ic_expand_more_grey600_24dp, 0);
                        textView.setVisibility(View.GONE);

                    } else {
                        toggle.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.ic_expand_less_grey600_24dp, 0);
                        textView.setVisibility(View.VISIBLE);
                    }
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
        menu.findItem(R.id.menu_quote).setVisible(!TextUtils.isEmpty(mParentText));
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

    private void send() {
        // TODO
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
}

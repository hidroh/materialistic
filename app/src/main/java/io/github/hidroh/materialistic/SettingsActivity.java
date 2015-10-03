package io.github.hidroh.materialistic;

import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;

import io.github.hidroh.materialistic.data.SearchRecentSuggestionsProvider;

public class SettingsActivity extends BaseActivity {
    @Inject AlertDialogBuilder mAlertDialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clear_recent) {
            mAlertDialogBuilder
                    .setMessage(R.string.clear_search_history_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new SearchRecentSuggestions(SettingsActivity.this,
                                    SearchRecentSuggestionsProvider.PROVIDER_AUTHORITY,
                                    SearchRecentSuggestionsProvider.MODE)
                                    .clearHistory();
                        }
                    })
                    .create()
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean isSearchable() {
        return false;
    }
}

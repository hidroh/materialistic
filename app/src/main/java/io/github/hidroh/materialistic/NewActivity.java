package io.github.hidroh.materialistic;

import android.content.Intent;
import android.support.annotation.NonNull;

import io.github.hidroh.materialistic.data.ItemManager;

public class NewActivity extends BaseStoriesActivity {
    public static final String EXTRA_REFRESH = NewActivity.class.getName() + ".EXTRA_REFRESH";

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(EXTRA_REFRESH, false)) {
            // triggered by new submission from user, refresh list
            ListFragment listFragment = ((ListFragment) getSupportFragmentManager()
                    .findFragmentByTag(LIST_FRAGMENT_TAG));
            if (listFragment != null) {
                listFragment.filter(getFetchMode());
            }
        }
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_new);
    }

    @NonNull
    @Override
    protected String getFetchMode() {
        return ItemManager.NEW_FETCH_MODE;
    }

}

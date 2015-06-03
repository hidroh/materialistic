package io.github.hidroh.materialistic;

import android.support.annotation.NonNull;

import io.github.hidroh.materialistic.data.ItemManager;

public class JobsActivity extends BaseStoriesActivity {

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.title_activity_jobs);
    }

    @NonNull
    @Override
    protected String getFetchMode() {
        return ItemManager.JOBS_FETCH_MODE;
    }

}

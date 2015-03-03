package io.github.hidroh.materialistic;

import android.os.Bundle;

public class ActionBarSettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    @Override
    protected boolean isSearchable() {
        return false;
    }
}

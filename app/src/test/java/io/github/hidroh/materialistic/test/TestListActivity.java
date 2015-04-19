package io.github.hidroh.materialistic.test;

import android.view.Menu;

import static org.robolectric.Shadows.shadowOf;

public class TestListActivity extends io.github.hidroh.materialistic.ListActivity {
    @Override
    public void supportInvalidateOptionsMenu() {
        Menu optionsMenu = shadowOf(this).getOptionsMenu();
        if (optionsMenu != null) {
            onCreateOptionsMenu(optionsMenu);
            onPrepareOptionsMenu(optionsMenu);
        }
    }
}

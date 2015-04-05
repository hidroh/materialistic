package io.github.hidroh.materialistic.test;

public class TestListActivity extends io.github.hidroh.materialistic.ListActivity {
    @Override
    protected boolean isSearchable() {
        return false; // TODO remove once Robolectric supports this
    }
}

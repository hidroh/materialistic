package io.github.hidroh.materialistic;

import android.view.MenuItem;

public abstract class BaseItemActivity extends InjectableActivity {
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

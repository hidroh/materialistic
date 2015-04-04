package io.github.hidroh.materialistic;

import android.os.Bundle;
import android.view.MenuItem;

import dagger.ObjectGraph;

public abstract class BaseItemActivity extends TrackableActivity implements Injectable {
    private ObjectGraph mActivityGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityGraph = ((Application) getApplication()).getApplicationGraph()
                .plus(new ActivityModule(this));
        mActivityGraph.inject(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        mActivityGraph = null;
        super.onDestroy();
    }

    @Override
    public void inject(Object object) {
        mActivityGraph.inject(object);
    }
}

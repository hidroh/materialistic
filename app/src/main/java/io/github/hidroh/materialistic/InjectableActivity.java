package io.github.hidroh.materialistic;

import android.os.Bundle;

import dagger.ObjectGraph;

public abstract class InjectableActivity extends ThemedActivity implements Injectable {
    private ObjectGraph mActivityGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityGraph = ((Application) getApplication()).getApplicationGraph()
                .plus(new ActivityModule(this));
        mActivityGraph.inject(this);
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

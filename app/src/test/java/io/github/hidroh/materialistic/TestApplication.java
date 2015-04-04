package io.github.hidroh.materialistic;

import org.robolectric.TestLifecycleApplication;
import org.robolectric.shadows.ShadowApplication;

import java.lang.reflect.Method;

import dagger.ObjectGraph;

public class TestApplication extends Application implements TestLifecycleApplication {
    public static ObjectGraph applicationGraph = ObjectGraph.create(new TestActivityModule());

    @Override
    public ObjectGraph getApplicationGraph() {
        return applicationGraph;
    }

    @Override
    public void beforeTest(Method method) {
        ShadowApplication.getInstance().declareActionUnbindable("com.google.android.gms.analytics.service.START");
    }

    @Override
    public void prepareTest(Object o) {

    }

    @Override
    public void afterTest(Method method) {

    }
}

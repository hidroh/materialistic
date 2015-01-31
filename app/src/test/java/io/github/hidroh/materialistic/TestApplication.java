package io.github.hidroh.materialistic;

import org.robolectric.Robolectric;
import org.robolectric.TestLifecycleApplication;

import java.lang.reflect.Method;

public class TestApplication extends android.app.Application implements TestLifecycleApplication {
    @Override
    public void beforeTest(Method method) {
        Robolectric.getShadowApplication().declareActionUnbindable("com.google.android.gms.analytics.service.START");
    }

    @Override
    public void prepareTest(Object o) {

    }

    @Override
    public void afterTest(Method method) {

    }
}

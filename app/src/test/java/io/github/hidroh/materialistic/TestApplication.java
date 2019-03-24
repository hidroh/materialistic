package io.github.hidroh.materialistic;

import android.content.Intent;
import android.graphics.Typeface;
import androidx.collection.ArrayMap;
import android.view.View;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestLifecycleApplication;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowResolveInfo;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import dagger.ObjectGraph;
import io.github.hidroh.materialistic.test.InMemoryDatabase;
import io.github.hidroh.materialistic.test.shadow.ShadowSnackbar;

import static org.mockito.Mockito.mock;
import static org.robolectric.Shadows.shadowOf;

public class TestApplication extends Application implements TestLifecycleApplication {
    public static ObjectGraph applicationGraph = ObjectGraph.create(new TestActivityModule());
    static {
        ArrayMap<String, Typeface> typefaces = ReflectionHelpers.getField(FontCache.getInstance(), "mTypefaceMap");
        typefaces.put("DroidSans.ttf", mock(Typeface.class));
        typefaces.put("DroidSerif.ttf", mock(Typeface.class));
        typefaces.put("LibreBaskerville-Regular.ttf", mock(Typeface.class));
        typefaces.put("RobotoSlab-Regular.ttf", mock(Typeface.class));
    }

    public static void addResolver(Intent intent) {
        ShadowPackageManager packageManager = shadowOf(RuntimeEnvironment.application.getPackageManager());
        packageManager.addResolveInfoForIntent( intent,
                ShadowResolveInfo.newResolveInfo("label", "com.android.chrome", "DefaultActivity"));
    }

    @Override
    public ObjectGraph getApplicationGraph() {
        return applicationGraph;
    }

    @Override
    public void beforeTest(Method method) {
        Preferences.sReleaseNotesSeen = true;
        ShadowApplication.getInstance().declareActionUnbindable("com.google.android.gms.analytics.service.START");
    }

    @Override
    public void prepareTest(Object o) {

    }

    @Override
    public void afterTest(Method method) {
        ShadowSnackbar.reset();
        try {
            InMemoryDatabase.reset();
            resetWindowManager();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    // TODO https://github.com/robolectric/robolectric/issues/2068
    private void resetWindowManager() {
        Class clazz = ReflectionHelpers.loadClass(getClass().getClassLoader(), "android.view.WindowManagerGlobal");
        Object instance = ReflectionHelpers.callStaticMethod(clazz, "getInstance");

        // We essentially duplicate what's in {@link WindowManagerGlobal#closeAll} with what's below.
        // The closeAll method has a bit of a bug where it's iterating through the "roots" but
        // bases the number of objects to iterate through by the number of "views." This can result in
        // an {@link java.lang.IndexOutOfBoundsException} being thrown.
        Object lock = ReflectionHelpers.getField(instance, "mLock");

        ArrayList<Object> roots = ReflectionHelpers.getField(instance, "mRoots");
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (lock) {
            for (int i = 0; i < roots.size(); i++) {
                ReflectionHelpers.callInstanceMethod(instance, "removeViewLocked",
                        ReflectionHelpers.ClassParameter.from(int.class, i),
                        ReflectionHelpers.ClassParameter.from(boolean.class, false));
            }
        }

        // Views will still be held by this array. We need to clear it out to ensure
        // everything is released.
        Collection<View> dyingViews = ReflectionHelpers.getField(instance, "mDyingViews");
        dyingViews.clear();

    }
}

package io.github.hidroh.materialistic;

import android.graphics.Typeface;

import dagger.ObjectGraph;

public class Application extends android.app.Application {
    public static Typeface TYPE_FACE = null;
    private ObjectGraph mApplicationGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        mApplicationGraph = ObjectGraph.create();
        Preferences.migrate(this);
        TYPE_FACE = FontCache.getInstance().get(this, Preferences.Theme.getTypeface(this));
    }

    public ObjectGraph getApplicationGraph() {
        return mApplicationGraph;
    }
}

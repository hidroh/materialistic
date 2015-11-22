package io.github.hidroh.materialistic;

import android.graphics.Typeface;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;

import dagger.ObjectGraph;

public class Application extends android.app.Application {
    public static Typeface TYPE_FACE = null;
    private ObjectGraph mApplicationGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            GoogleAnalytics.getInstance(this).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            GoogleAnalytics.getInstance(this).setDryRun(true);
        }
        GoogleAnalytics.getInstance(this).newTracker(R.xml.ga_config);
        mApplicationGraph = ObjectGraph.create();
        Preferences.migrate(this);
        TYPE_FACE = AppUtils.createTypeface(this, Preferences.Theme.getTypeface(this));
    }

    public ObjectGraph getApplicationGraph() {
        return mApplicationGraph;
    }
}

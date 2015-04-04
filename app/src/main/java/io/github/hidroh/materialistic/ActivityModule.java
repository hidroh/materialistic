package io.github.hidroh.materialistic;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {
                ItemActivity.class,
                WebActivity.class,
                ItemFragment.class
        },
        library = true
)
public class ActivityModule {
    private final Context mContext;

    public ActivityModule(Context context) {
        mContext = context;
    }

    @Provides @Singleton
    public Context provideContext() {
        return mContext;
    }
}

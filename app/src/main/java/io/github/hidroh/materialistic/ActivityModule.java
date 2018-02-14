/*
 * Copyright (c) 2015 Ha Duy Trung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.hidroh.materialistic;

import android.accounts.AccountManager;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.github.hidroh.materialistic.appwidget.WidgetService;
import io.github.hidroh.materialistic.data.ItemSyncJobService;
import io.github.hidroh.materialistic.data.ItemSyncService;

@Module(
        injects = {
                ItemSyncService.class,
                WidgetService.class,
                ItemSyncJobService.class
        },
        library = true,
        includes = DataModule.class
)
public class ActivityModule {
    public static final String ALGOLIA = "algolia";
    public static final String POPULAR = "popular";
    public static final String HN = "hn";

    private final Context mContext;

    public ActivityModule(Context context) {
        mContext = context;
    }

    @Provides @Singleton
    public Context provideContext() {
        return mContext;
    }

    @Provides @Singleton
    public AccountManager provideAccountManager(Context context) {
        return AccountManager.get(context);
    }
}

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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Typeface;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import dagger.ObjectGraph;

public class Application extends android.app.Application {

    private static final String SYNC_ACCOUNT_NAME = "sync";
    private static final String SYNC_ACCOUNT_TYPE = BuildConfig.APPLICATION_ID + ".sync";

    public static Typeface TYPE_FACE = null;
    private RefWatcher mRefWatcher;
    private ObjectGraph mApplicationGraph;

    public static RefWatcher getRefWatcher(Context context) {
        Application application = (Application) context.getApplicationContext();
        return application.mRefWatcher;
    }

    public static Account createSyncAccount() {
        return new Account(SYNC_ACCOUNT_NAME, SYNC_ACCOUNT_TYPE);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRefWatcher = LeakCanary.install(this);
        mApplicationGraph = ObjectGraph.create();
        Preferences.migrate(this);
        TYPE_FACE = FontCache.getInstance().get(this, Preferences.Theme.getTypeface(this));
        AccountManager.get(this).addAccountExplicitly(createSyncAccount(), null, null);
        AppUtils.registerAccountsUpdatedListener(this);
    }

    public ObjectGraph getApplicationGraph() {
        return mApplicationGraph;
    }
}

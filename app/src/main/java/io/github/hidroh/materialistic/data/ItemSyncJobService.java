/*
 * Copyright (c) 2016 Ha Duy Trung
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

package io.github.hidroh.materialistic.data;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.github.hidroh.materialistic.ActivityModule;
import io.github.hidroh.materialistic.Application;
import io.github.hidroh.materialistic.Preferences;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ItemSyncJobService extends JobService {
    static final String EXTRA_ID = "extra:id";
    @Inject RestServiceFactory mFactory;
    @Inject ReadabilityClient mReadabilityClient;
    private final Map<String, SyncDelegate> mSyncDelegates = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication())
                .getApplicationGraph()
                .plus(new ActivityModule(this))
                .inject(this);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        String id = jobParameters.getExtras().getString(EXTRA_ID),
                jobId = String.valueOf(jobParameters.getJobId());
        SyncDelegate syncDelegate = createSyncDelegate();
        mSyncDelegates.put(jobId, syncDelegate);
        syncDelegate.subscribe(token -> {
            if (TextUtils.equals(jobId, token)) {
                jobFinished(jobParameters, false);
                mSyncDelegates.remove(jobId);
            }
        });
        syncDelegate.performSync(new SyncDelegate.JobBuilder(id)
                .setConnectionEnabled(true)
                .setReadabilityEnabled(Preferences.Offline.isReadabilityEnabled(this))
                .setArticleEnabled(Preferences.Offline.isArticleEnabled(this))
                .setCommentsEnabled(Preferences.Offline.isCommentsEnabled(this))
                .setNotificationEnabled(Preferences.Offline.isNotificationEnabled(this))
                .build());
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        String key = String.valueOf(jobParameters.getJobId());
        if (mSyncDelegates.containsKey(key)) {
            mSyncDelegates.remove(key).stopSync();
        }
        return true;
    }

    @VisibleForTesting
    @NonNull
    SyncDelegate createSyncDelegate() {
        return new SyncDelegate(this, mFactory, mReadabilityClient);
    }
}

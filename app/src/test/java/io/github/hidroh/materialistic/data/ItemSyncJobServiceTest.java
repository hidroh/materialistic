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
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowJobScheduler;
import org.robolectric.shadows.ShadowNetworkInfo;

import java.util.List;

import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.test.TestRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@RunWith(TestRunner.class)
public class ItemSyncJobServiceTest {
    private ServiceController<TestItemSyncJobService> controller;
    private TestItemSyncJobService service;
    @Captor ArgumentCaptor<SyncDelegate.ProgressListener> listenerCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        controller = Robolectric.buildService(TestItemSyncJobService.class);
        service = controller.create().get();
    }

    @Test
    public void testScheduledJob() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putBoolean(RuntimeEnvironment.application.getString(R.string.pref_saved_item_sync), true)
                .apply();
        shadowOf((ConnectivityManager) RuntimeEnvironment.application
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null,
                        ConnectivityManager.TYPE_WIFI, 0, true, NetworkInfo.State.CONNECTED));
        new SyncScheduler().scheduleSync(RuntimeEnvironment.application, "1");
        List<JobInfo> pendingJobs = ((ShadowJobScheduler.ShadowJobSchedulerImpl)
                shadowOf((JobScheduler) RuntimeEnvironment.application
                .getSystemService(Context.JOB_SCHEDULER_SERVICE))).getAllPendingJobs();
        assertThat(pendingJobs).isNotEmpty();
        JobInfo actual = pendingJobs.get(0);
        assertThat(actual.getService().getClassName())
                .isEqualTo(ItemSyncJobService.class.getName());
    }

    @Test
    public void testStartJob() {
        JobParameters jobParameters = mock(JobParameters.class);
        when(jobParameters.getExtras())
                .thenReturn(new SyncDelegate.JobBuilder(service, "1").build().toPersistableBundle());
        when(jobParameters.getJobId()).thenReturn(2);
        service.onStartJob(jobParameters);
        verify(service.syncDelegate).subscribe(listenerCaptor.capture());
        verify(service.syncDelegate).performSync(any(SyncDelegate.Job.class));
        listenerCaptor.getValue().onDone("2");
    }

    @After
    public void tearDown() {
        controller.destroy();
    }

    public static class TestItemSyncJobService extends ItemSyncJobService {
        SyncDelegate syncDelegate = mock(SyncDelegate.class);

        @NonNull
        @Override
        SyncDelegate createSyncDelegate() {
            return syncDelegate;
        }
    }
}

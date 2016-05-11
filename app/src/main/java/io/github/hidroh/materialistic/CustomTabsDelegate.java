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

package io.github.hidroh.materialistic;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.text.TextUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class CustomTabsDelegate {
    private static final String STABLE_PACKAGE = "com.android.chrome";
    private static final String BETA_PACKAGE = "com.chrome.beta";
    private static final String DEV_PACKAGE = "com.chrome.dev";
    private static final String LOCAL_PACKAGE = "com.google.android.apps.chrome";
    private static final String ACTION_CUSTOM_TABS_CONNECTION =
            "android.support.customtabs.action.CustomTabsService";
    private static String sPackageNameToUse;
    private CustomTabsSession mCustomTabsSession;
    private CustomTabsClient mClient;
    private CustomTabsServiceConnection mConnection;

    /**
     * Binds the Activity to the Custom Tabs Service.
     * @param activity the activity to be binded to the service.
     */
    public void bindCustomTabsService(Activity activity) {
        if (mClient != null) {
            return;
        }
        if (TextUtils.isEmpty(getPackageNameToUse(activity))) {
            return;
        }
        mConnection = new ServiceConnection(this);
        CustomTabsClient.bindCustomTabsService(activity, sPackageNameToUse, mConnection);
    }

    /**
     * Unbinds the Activity from the Custom Tabs Service.
     * @param activity the activity that is connected to the service.
     */
    public void unbindCustomTabsService(Activity activity) {
        if (mConnection == null) {
            return;
        }
        activity.unbindService(mConnection);
        mClient = null;
        mCustomTabsSession = null;
        mConnection = null;
    }

    /**
     * @see {@link CustomTabsSession#mayLaunchUrl(Uri, Bundle, List)}.
     * @return true if call to mayLaunchUrl was accepted.
     */
    public boolean mayLaunchUrl(Uri uri, Bundle extras, List<Bundle> otherLikelyBundles) {
        if (mClient == null) {
            return false;
        }
        CustomTabsSession session = getSession();
        return session != null && session.mayLaunchUrl(uri, extras, otherLikelyBundles);
    }

    /**
     * Creates or retrieves an exiting CustomTabsSession.
     *
     * @return a CustomTabsSession.
     */
    public CustomTabsSession getSession() {
        if (mClient == null) {
            mCustomTabsSession = null;
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mClient.newSession(null);
        }
        return mCustomTabsSession;
    }

    void onServiceConnected(CustomTabsClient client) {
        mClient = client;
        mClient.warmup(0L);
    }

    void onServiceDisconnected() {
        mClient = null;
        mCustomTabsSession = null;
    }

    private static String getPackageNameToUse(Context context) {
        if (sPackageNameToUse != null) {
            return sPackageNameToUse;
        }
        // packagesSupportingCustomTabs contains all apps that can handle both VIEW intents
        // and service calls.
        List<String> packagesSupportingCustomTabs = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(
                new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com")), 0);
        //noinspection Convert2streamapi
        for (ResolveInfo info : resolvedActivityList) {
            if (pm.resolveService(new Intent()
                    .setAction(ACTION_CUSTOM_TABS_CONNECTION)
                    .setPackage(info.activityInfo.packageName), 0) != null) {
                packagesSupportingCustomTabs.add(info.activityInfo.packageName);
            }
        }
        if (packagesSupportingCustomTabs.contains(STABLE_PACKAGE)) {
            sPackageNameToUse = STABLE_PACKAGE;
        } else if (packagesSupportingCustomTabs.contains(BETA_PACKAGE)) {
            sPackageNameToUse = BETA_PACKAGE;
        } else if (packagesSupportingCustomTabs.contains(DEV_PACKAGE)) {
            sPackageNameToUse = DEV_PACKAGE;
        } else if (packagesSupportingCustomTabs.contains(LOCAL_PACKAGE)) {
            sPackageNameToUse = LOCAL_PACKAGE;
        }
        return sPackageNameToUse;
    }

    static class ServiceConnection extends CustomTabsServiceConnection {
        private WeakReference<CustomTabsDelegate> mDelegate;

        public ServiceConnection(CustomTabsDelegate delegate) {
            mDelegate = new WeakReference<>(delegate);
        }

        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            CustomTabsDelegate delegate = mDelegate.get();
            if (delegate != null) {
                delegate.onServiceConnected(client);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            CustomTabsDelegate delegate = mDelegate.get();
            if (delegate != null) {
                delegate.onServiceDisconnected();
            }
        }
    }
}

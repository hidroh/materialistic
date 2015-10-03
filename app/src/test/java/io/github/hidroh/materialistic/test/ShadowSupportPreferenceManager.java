package io.github.hidroh.materialistic.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowApplication;

@Implements(PreferenceManager.class)
public class ShadowSupportPreferenceManager {

    @Implementation
    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return ShadowApplication.getInstance().getSharedPreferences("__default__", Context.MODE_PRIVATE);
    }
}

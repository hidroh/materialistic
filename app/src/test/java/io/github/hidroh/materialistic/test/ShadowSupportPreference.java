package io.github.hidroh.materialistic.test;

import android.support.v7.preference.Preference;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

@Implements(Preference.class)
public class ShadowSupportPreference {
    @RealObject Preference realObject;
    private String persistedString;

    @Implementation
    public void persistString(String value) {
        persistedString = value;
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putString(realObject.getKey(), value)
                .apply();
    }

    public String getShadowPersistedString() {
        return persistedString;
    }
}

package io.github.hidroh.materialistic.test;

import android.support.v7.preference.Preference;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(Preference.class)
public class ShadowSupportPreference {
    private String persistedString;

    @Implementation
    public void persistString(String value) {
        persistedString = value;
    }

    public String getShadowPersistedString() {
        return persistedString;
    }
}

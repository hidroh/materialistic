package io.github.hidroh.materialistic;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.content.IntentCompat;

import io.github.hidroh.materialistic.data.AlgoliaClient;

public class SettingsActivity extends PreferenceActivity {

    private SharedPreferences.OnSharedPreferenceChangeListener mListener;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Preferences.sync(getPreferenceManager());
        mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Preferences.sync(getPreferenceManager(), key);
                if (key.equals(getString(R.string.pref_theme)) ||
                        key.equals(getString(R.string.pref_text_size))) {
                    finish();
                    final Intent intent = IntentCompat.makeMainActivity(new ComponentName(
                            SettingsActivity.this, ListActivity.class));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else if (key.equals(getString(R.string.pref_search_sort))) {
                    AlgoliaClient.sSortByTime = Preferences.isSortByRecent(SettingsActivity.this);
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mListener);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mListener);
        super.onPause();
    }
}

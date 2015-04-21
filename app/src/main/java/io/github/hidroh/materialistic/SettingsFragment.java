package io.github.hidroh.materialistic;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.content.IntentCompat;

import io.github.hidroh.materialistic.data.AlgoliaClient;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment {

    private SharedPreferences.OnSharedPreferenceChangeListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        Preferences.sync(getPreferenceManager());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Preferences.sync(getPreferenceManager(), key);
                if (key.equals(getActivity().getString(R.string.pref_theme)) ||
                        key.equals(getActivity().getString(R.string.pref_text_size))) {
                    getActivity().finish();
                    final Intent intent = getActivity().getIntent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                    getActivity().startActivity(intent);
                } else if (key.equals(getActivity().getString(R.string.pref_search_sort))) {
                    AlgoliaClient.sSortByTime = Preferences.isSortByRecent(getActivity());
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

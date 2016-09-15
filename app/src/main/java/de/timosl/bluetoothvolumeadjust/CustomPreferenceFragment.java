package de.timosl.bluetoothvolumeadjust;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * A {@link PreferenceFragment} that displays settings as a Fragment to the user.
 */
public class CustomPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preference file
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register ourselves as a preference change listener
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister ourselves as a preference change listener
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        // If the ENABLE_DEBUGGING key was changed and is now disabled, clear the debug log
        // to prevent privacy leaks
        if (Preferences.KEY_ENABLE_DEBUGGING.equals(key) && !Preferences.getEnableDebugging(getActivity())) {
            L.clearLog();
        }
    }
}

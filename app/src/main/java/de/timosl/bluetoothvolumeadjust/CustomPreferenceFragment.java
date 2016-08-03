package de.timosl.bluetoothvolumeadjust;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * A {@link PreferenceFragment} that displays settings as a Fragment to the user.
 */
public class CustomPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preference file
        addPreferencesFromResource(R.xml.preferences);
    }
}

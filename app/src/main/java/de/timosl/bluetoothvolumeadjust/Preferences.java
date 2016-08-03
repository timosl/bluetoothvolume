package de.timosl.bluetoothvolumeadjust;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Getter and Setter methods for accessing the preferences set by the user.
 */
public class Preferences {

    /**
     * Key for the volume indicator setting.
     */
    private static final String KEY_SHOW_INDICATOR = "pref_show_indicator";

    /**
     * Returns if the systems volume indicator should be shown during volume changes or
     * if it should be hidden.
     * @param context The applications {@link Context}
     * @return Returns 'true' only, if the systems volume indicator should be shown,
     * 'false' if it should be hidden.
     */
    public static boolean getShowIndicatorEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_SHOW_INDICATOR,true);
    }
}

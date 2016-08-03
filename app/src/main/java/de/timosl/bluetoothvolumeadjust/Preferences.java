package de.timosl.bluetoothvolumeadjust;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceFragment;
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
     * Key for the volume reset on disconnect setting.
     */
    private static final String KEY_RESET_VOLUME_ON_DISCONNECT = "pref_reset_volume_on_disconnect";

    /**
     * Key for the last media volume setting.
     */
    private static final String KEY_LAST_MEDIA_VOLUME = "pref_last_media_volume";

    /**
     * Returns if the systems volume indicator should be shown during volume changes or
     * if it should be hidden.
     * @param context The applications {@link Context}
     * @return Returns 'true' only, if the systems volume indicator should be shown,
     * 'false' if it should be hidden
     */
    public static boolean getShowIndicatorEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_SHOW_INDICATOR,true);
    }

    /**
     * Returns if the volume should be set to its previous value after a device disconnects.
     * @param context The applications {@link Context}
     * @return Returns 'true' only, if the volume should be reset and 'false' if no action
     * should be taken after a device disconnects
     */
    public static boolean getResetVolumeOnDisconnect(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_RESET_VOLUME_ON_DISCONNECT,false);
    }

    /**
     * Sets the last media volume.
     * @param context The applications {@link Context}
     * @param volume The volume to store for restoring it later
     */
    public static void setLastMediaVolume(Context context, int volume) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_LAST_MEDIA_VOLUME,volume);
        editor.commit();
    }

    /**
     * Returns the last media volume.
     * @param context The applications {@link Context}
     * @return The last media volume before
     */
    public static int getLastMediaVolume(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_LAST_MEDIA_VOLUME,-1);
    }
}

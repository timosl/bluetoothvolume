package de.timosl.bluetoothvolumeadjust;

import android.app.Application;

import java.util.Set;

/**
 * Application delegate for this app. Used for initialization
 * for app-wide components.
 */
public class ApplicationDelegate extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initializes the logging feature
        L.init(this);

        // Log some general app information
        logAppInfo();
    }


    /**
     * Logs some genreal application info for debugging.
     */
    private void logAppInfo() {
        // Get the devices that we currently have managed
        Set<String> devices = DeviceManagment.getDevices(this);

        // Log the app version
        L.i(String.format("(AppDelegate) Application created. Version: %s (%d)",BuildConfig.VERSION_NAME,BuildConfig.VERSION_CODE));

        // Log all managed devices
        for(String deviceAddress: devices) {
            L.i(String.format("(AppDelegate) Registered device %s (%s) at volume %f",DeviceManagment.getDeviceByAddress(deviceAddress).getName(),deviceAddress,DeviceManagment.getDeviceVolume(this,deviceAddress)));
        }

        // Log preferences
        L.i(String.format("(AppDelegate) Restoring volume is set to: %b",Preferences.getResetVolumeOnDisconnect(this)));
    }
}

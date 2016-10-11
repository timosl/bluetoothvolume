package de.timosl.bluetoothvolumeadjust;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

import java.util.Set;

import de.timosl.bluetoothvolumeadjust.util.DeviceManagment;
import de.timosl.bluetoothvolumeadjust.util.L;
import de.timosl.bluetoothvolumeadjust.util.Preferences;

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
     * Logs some general application info for debugging.
     */
    private void logAppInfo() {
        // Get the devices that we currently have managed
        Set<String> devices = DeviceManagment.getDevices(this);

        if(BluetoothAdapter.getDefaultAdapter() == null) {
            L.w("(AppDelegate) Bluetooth is disabled or the adapter is unavailable.");
        } else {
            // Log the app version
            L.i(String.format("(AppDelegate) Application created. Version: %s (%d)",BuildConfig.VERSION_NAME,BuildConfig.VERSION_CODE));

            // Log all managed devices
            for(String deviceAddress: devices) {
                String deviceName = DeviceManagment.getDeviceByAddress(deviceAddress) != null ? DeviceManagment.getDeviceByAddress(deviceAddress).getName() : "<NULL_DEVICE>";
                float deviceVolume = DeviceManagment.getDeviceVolume(this,deviceAddress);

                L.i(String.format("(AppDelegate) Registered device %s (%s) at volume %f",deviceName,deviceAddress,deviceVolume));
            }
        }

        // Log preferences
        L.i(String.format("(AppDelegate) Restoring volume is set to: %b", Preferences.getResetVolumeOnDisconnect(this)));
    }
}

package de.timosl.bluetoothvolumeadjust.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stores the list of managed Bluetooth devices and the volume specified for
 * each of them.
 */
public class DeviceManagment {

    /**
     * The key for the list of devices MAC-Addresses.
     */
    private static final String KEY_DEVICES = "devices";

    /**
     * The prefix for the volume preference set for each device.
     */
    private static final String PREFIX_DEVICES = "bl_device_";

    /**
     * Returns a {@link Set} of the MAC-Addresses for each Bluetooth device
     * with a custom volume specified for them.
     * @param context The application context
     * @return A {@link Set} containing the MAC-Addresses of Bluetooth devices with
     * custom volumes specified for them
     */
    public static Set<String> getDevices(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet(KEY_DEVICES,new HashSet<String>());
    }

    /**
     * Sets the {@link Set} of Bluetooth devices. This must be the complete list, since the old
     * set will be overriden.
     * @param context The application context
     * @param devices The {@link Set} of Bluetooth devices
     */
    private static void setDevices(Context context, Set<String> devices) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putStringSet(KEY_DEVICES, devices);
        editor.commit();
    }

    /**
     * Removes a Bluetooth device and the associated volume.
     * @param context The application context
     * @param device The MAC-Address of the Bluetooth device
     */
    public static void removeDevice(Context context, String device) {
        // Remove the device from the list of all devices
        Set<String> devices = getDevices(context);
        devices.remove(device);
        setDevices(context, devices);

        // Remove the key containing the volume for this device
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(PREFIX_DEVICES + device);
        editor.commit();
    }

    /**
     * Adds the Bluetooth device with the given MAC-Address and store the given volume
     * for it.
     * @param context The application context
     * @param device The MAC-Address of the Bluetooth device
     * @param volume The volume of the device in a range from 0.0 to 1.0
     */
    public static void addDevice(Context context, String device, float volume) {
        // Check the range of the volume
        if(volume < 0f || volume > 1f) {
            throw new IllegalArgumentException("The volume has to be between 0.0 and 1.0 (Found: "+volume+")");
        }

        // Add the device to the list of all managed devices
        Set<String> devices = getDevices(context);
        devices.add(device);
        setDevices(context, devices);

        // Add the volume for this device with a key made
        // from the prefix and the devices MAC-Address
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putFloat(PREFIX_DEVICES + device, volume);
        editor.commit();
    }

    /**
     * Returns the volume for the given device.
     * @param context The application context
     * @param device The MAC-Address of the Bluetooth device
     * @return The volume for this device in a range from 0.0 to 1.0, or -1 if
     * the given device has no volume specified
     */
    public static float getDeviceVolume(Context context, String device) {
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(PREFIX_DEVICES+device,-1f);
    }

    public static BluetoothDevice getDeviceByAddress(String address) {
        // Get the list of all devices bonded with this device
        final List<BluetoothDevice> bondedDevices = new ArrayList<>(BluetoothAdapter.getDefaultAdapter().getBondedDevices());

        // Iterate over all of them to add them to our list
        for(BluetoothDevice device: bondedDevices) {
            // Only add the ones we are actually managing
            if(device.getAddress().equals(address)) {
                return device;
            }
        }
        return null;
    }
}

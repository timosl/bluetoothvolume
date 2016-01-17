package de.timosl.bluetoothvolumeadjust;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

/**
 * A {@link BroadcastReceiver} that will listen for the 'android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED'
 * action. If such an {@link Intent} is received, the volume of the {@link AudioManager#STREAM_MUSIC} media
 * stream will be adjusted according to the settings configured for the device that is now connected.
 */
public class BluetoothIntentReceiver extends BroadcastReceiver {

    /**
     * The time in milliseconds after which we abort waiting for music to start on
     * the Bluetooth device.
     */
    private static final long MUSIC_TIMEOUT = 20000L;

    @Override
    public void onReceive(final Context context, Intent intent) {
        // Get the device and its state from the Intent
        final int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,-1);
        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        // Check if the Intent was properly filled
        if(state == -1 || device == null) {
            Log.e(MainActivity.TAG, "The received intent was not valid");
            return;
        }

        // Only adjust volume if a Bluetooth device is now CONNECTED
        if(state == BluetoothProfile.STATE_CONNECTED) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            // If we're already playing music on the Bluetooth device, we can adjust the volume right away
            if(audioManager.isBluetoothA2dpOn() && audioManager.isMusicActive()) {
                adjustAudio(context,device);
            }
            // If there is no music playing right now, we will wait till the music player launches
            // itself and starts playing (or the user does this manually)
            else {
                // Store the time so we can abort after a certain interval
                long musicWaitBegin = System.currentTimeMillis();

                // Stay in this loop until music is being played
                while(!audioManager.isMusicActive()) {
                    try {
                        // Don't completely waste CPU cycles
                        Thread.sleep(100L);

                        // Abort if we waited too long already
                        if(System.currentTimeMillis() - musicWaitBegin > MUSIC_TIMEOUT) {
                            Log.w(MainActivity.TAG,"There was no music playing after "+(System.currentTimeMillis() - musicWaitBegin)+"ms, not adjusting volume");
                            return;
                        }
                    } catch (InterruptedException e) {}
                }
                Log.d(MainActivity.TAG,"Waited "+(System.currentTimeMillis()-musicWaitBegin)+"ms");

                // If we ended up here, there should be music playing on the Bluetooth device,
                // so we can finally adjust the volume
                adjustAudio(context,device);
            }
        }
    }


    /**
     * Adjusts the volume for the currently connected device.
     * @param context The applications {@link Context}
     * @param device The {@link BluetoothDevice} to change the volume for
     */
    private void adjustAudio(Context context, BluetoothDevice device) {
        // Get the volume for this device
        float volume = DeviceManagment.getDeviceVolume(context,device.getAddress());

        // Check if a volume has been set for this device
        if(volume != -1f) {
            Log.d(MainActivity.TAG,"Adjusting volume for "+device.getName());

            // Apply the specified volume as a multiplier to the maximum volume for the
            // music stream.
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int newVolume = (int) (maxVolume * volume);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,newVolume,AudioManager.FLAG_SHOW_UI);
        } else {
            Log.w(MainActivity.TAG,"There is no volume set for this device: "+device.getName());
        }
    }
}

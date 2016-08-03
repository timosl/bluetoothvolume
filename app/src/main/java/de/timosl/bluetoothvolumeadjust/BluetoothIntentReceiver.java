package de.timosl.bluetoothvolumeadjust;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.preference.Preference;
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

        // Perform the necessary actions when a device is CONNECTING
        if(state == BluetoothProfile.STATE_CONNECTING) {
            onDeviceConnecting(context);
        }

        // Perform the necessary actions when a device is CONNECTED
        if(state == BluetoothProfile.STATE_CONNECTED) {
            onDeviceConnected(context, device);
        }

        // Perform the necessary actions when a device is DISCONNECTED
        if(state == BluetoothProfile.STATE_DISCONNECTED) {
            onDeviceDisconnected(context);
        }
    }

    /**
     * Called when a registered device is being connected. (This usually
     * means the media stream has not yet switched to Bluetooth)
     * @param context The applications {@link Context}
     */
    private void onDeviceConnecting(Context context) {
        // Get a reference to the audio manager
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Store the current media volume so we can reset it later (if needed)
        int currentMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        Preferences.setLastMediaVolume(context,currentMediaVolume);
    }

    /**
     * Called when a registered device is now connected.
     * @param context The applications {@link Context}
     * @param device The {@link BluetoothDevice} that has connected
     */
    private void onDeviceConnected(Context context, BluetoothDevice device) {
        // Get a reference to the audio manager
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // If we're already playing music on the Bluetooth device, we can adjust the volume right away
        if(audioManager.isBluetoothA2dpOn() && audioManager.isMusicActive()) {
            adjustAudio(context,device);
        }

        // If there is currently no music playing, adjusting the volume of the Music-Channel
        // will not affect the volume for the bluetooth device. Android handles music volume over
        // bluetooth differently and will only allow changes when music is actively being played
        // over bluetooth.
        else {
            // Store the time so we can abort after a certain interval
            long musicWaitBegin = System.currentTimeMillis();

            // If no music is currently playing, a silent music track
            // will be played
            if(!audioManager.isMusicActive()) {
                Log.i(MainActivity.TAG,"No music is currently being played. Playing a silent track to enable proper volume adjustment");

                try {
                    // Create and start a MediaPlayer playing a silent music track
                    MediaPlayer silencePlayer = MediaPlayer.create(context, R.raw.silence);
                    silencePlayer.start();

                    // Release the resources of the MediaPlayer after the
                    // track has been played
                    silencePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();
                        }
                    });
                } catch(Exception exception) {
                    Log.e(MainActivity.TAG,"There was an error playing the silent track: "+exception);
                }
            }

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

    /**
     * Called when a registered device is now disconnected.
     * @param context The applications {@link Context}
     */
    private void onDeviceDisconnected(Context context) {
        // Get a reference to the audio manager
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Reset the media volume if the user enabled the corresponding setting
        if(Preferences.getResetVolumeOnDisconnect(context)) {
            // Check the user preference if the volume indicator should be displayed
            int showIndicatorFlag = Preferences.getShowIndicatorEnabled(context) ? AudioManager.FLAG_SHOW_UI : 0;

            // Get the previous volume
            int previousVolume = Preferences.getLastMediaVolume(context);

            // Ensure a previous volume was stored
            if(previousVolume != -1) {
                // Set the volume to its previous level
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,previousVolume,showIndicatorFlag);

                Log.d(MainActivity.TAG,"Volume reset to previous value");
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

            // Check the user preference if the volume indicator should be displayed
            int showIndicatorFlag = Preferences.getShowIndicatorEnabled(context) ? AudioManager.FLAG_SHOW_UI : 0;

            // Apply the specified volume as a multiplier to the maximum volume for the
            // music stream.
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int newVolume = (int) (maxVolume * volume);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,newVolume,showIndicatorFlag);
        } else {
            Log.w(MainActivity.TAG,"There is no volume set for this device: "+device.getName());
        }
    }
}

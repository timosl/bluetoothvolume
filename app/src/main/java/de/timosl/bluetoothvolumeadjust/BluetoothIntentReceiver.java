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
            L.w("(BluetoothIntentReceiver) The received intent was not valid. Received: "+intent);
            return;
        }

        // Perform the necessary actions when a device is CONNECTING
        if(state == BluetoothProfile.STATE_CONNECTING) {
            L.i(String.format("(BluetoothIntentReceiver) The device %s (%s) is now CONNECTING",device.getName(),device.getAddress()));
            onDeviceConnecting(context);
        }

        // Perform the necessary actions when a device is CONNECTED
        if(state == BluetoothProfile.STATE_CONNECTED) {
            L.i(String.format("(BluetoothIntentReceiver) The device %s (%s) is now CONNECTED",device.getName(),device.getAddress()));
            onDeviceConnected(context, device);
        }

        // Perform the necessary actions when a device is DISCONNECTED
        if(state == BluetoothProfile.STATE_DISCONNECTED) {
            L.i(String.format("(BluetoothIntentReceiver) The device %s (%s) is now DISCONNECTING",device.getName(),device.getAddress()));
            onDeviceDisconnected(context, device);
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
        int maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Preferences.setLastMediaVolume(context,currentMediaVolume);

        L.i(String.format("(BluetoothIntentReceiver) Storing current media volume: %d out of %d",currentMediaVolume, maxMediaVolume));
    }

    /**
     * Called when a registered device is now connected.
     * @param context The applications {@link Context}
     * @param device The {@link BluetoothDevice} that has connected
     */
    private void onDeviceConnected(Context context, BluetoothDevice device) {
        // Get a reference to the audio manager
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Retrieve the volume the user has set for this device
        float volumePercentage = DeviceManagment.getDeviceVolume(context,device.getAddress());

        // Do not change the volume if there is no value set for this device
        if(volumePercentage == -1f) {
            L.w(String.format("(BluetoothIntentReceiver) No volume set for device %s (%s). Is this device managed?",device.getName(),device.getAddress()));
            return;
        }

        // Convert the users value to a format the AudioManager can use
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int newVolume = (int) (maxVolume * volumePercentage);

        // If we're already playing music on the Bluetooth device, we can adjust the volume right away
        if(audioManager.isBluetoothA2dpOn() && audioManager.isMusicActive()) {
            adjustAudio(context,newVolume);
        }

        // If there is currently no music playing, adjusting the volume of the Music-Channel
        // will not affect the volume for the bluetooth device. Android handles music volume over
        // bluetooth differently and will only allow changes when music is actively being played
        // over bluetooth.
        else {
            playSilenceAndAdjustVolume(context, newVolume);
        }
    }

    /**
     * Called when a registered device is now disconnected.
     * @param context The applications {@link Context}
     */
    private void onDeviceDisconnected(Context context, BluetoothDevice device) {
        // Get a reference to the audio manager
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Do not change the volume if we don't manage the device that is now disconnected
        if(DeviceManagment.getDeviceVolume(context,device.getAddress()) == -1f) {
            L.i(String.format("(BluetoothIntentReceiver) The device %s (%s) is not managed by us, not resetting volume",device.getName(),device.getAddress()));
            return;
        }

        // Reset the media volume if the user enabled the corresponding setting
        if(Preferences.getResetVolumeOnDisconnect(context)) {

            // Get the previous volume
            int previousVolume = Preferences.getLastMediaVolume(context);
            L.i(String.format("(BluetoothIntentReceiver) Device disconnected, restoring volume back to %d on user request",previousVolume));

            // Check if there is already music playing on the device. If yes, we can change
            // the volume right away.
            if(audioManager.isMusicActive()) {
                adjustAudio(context,previousVolume);
            }

            // If there is no music playing, we just play a silent track to ensure the correct
            // audio stream will be changed.
            else {
                playSilenceAndAdjustVolume(context,previousVolume);
            }
        } else {
            L.i(String.format("(BluetoothIntentReceiver) Not restoring volume on user request"));
        }
    }

    /**
     * Adjusts the volume of the music audio stream with the given value.
     * @param context The applications {@link Context}
     * @param volume The volume to set. Using '-1' will not adjust the volume.
     */
    private void adjustAudio(Context context, int volume) {
        // Check if a volume has been set
        if(volume == -1f) {
            L.w(String.format("(BluetoothIntentReceiver) No valid volume passed to adjustAudio() (%d given)",volume));
            return;
        }

        // Check the user preference if the volume indicator should be displayed
        int showIndicatorFlag = Preferences.getShowIndicatorEnabled(context) ? AudioManager.FLAG_SHOW_UI : 0;

        // Apply the specified volume as a multiplier to the maximum volume for the
        // music stream.
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,showIndicatorFlag);
    }

    /**
     * Adjusts the volume of the music audio stream with the given value while playing
     * a silent audio track. This ensures that the correct audio stream will be changed.
     * This could otherwise be a problem with some devices.
     * @param context The applications {@link Context}
     * @param volume The volume to set. Using '-1' will not adjust the volume.
     */
    private void playSilenceAndAdjustVolume(Context context, int volume) {
        // Check if a volume has been set
        if(volume == -1) {
            L.w(String.format("(BluetoothIntentReceiver) No valid volume passed to playSilenceAndAdjustVolume() (%d given)",volume));
            return;
        }

        // Get a reference to the audio manager
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Store the time so we can abort after a certain interval
        long musicWaitBegin = System.currentTimeMillis();

        // If no music is currently playing, a silent music track
        // will be played
        if(!audioManager.isMusicActive()) {
            L.i("(BluetoothIntentReceiver) No music is currently being played. Playing a silent track to enable proper volume adjustment");

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
                        L.i("(BluetoothIntentReceiver) Media playback released");
                    }
                });
            } catch(Exception exception) {
                L.w("(BluetoothIntentReceiver) There was an error playing the silent track: "+exception);
            }
        }

        // Stay in this loop until music is being played
        while(!audioManager.isMusicActive()) {
            try {
                // Don't completely waste CPU cycles
                Thread.sleep(100L);

                // Abort if we waited too long already
                if(System.currentTimeMillis() - musicWaitBegin > MUSIC_TIMEOUT) {
                    L.w("There was no music playing after "+(System.currentTimeMillis() - musicWaitBegin)+"ms, not adjusting volume");
                    return;
                }
            } catch (InterruptedException e) {
                L.w("(BluetoothIntentReceiver) Interrupted while waiting for media playback");
            }
        }
        L.i("(BluetoothIntentReceiver) Waited "+(System.currentTimeMillis()-musicWaitBegin)+"ms and setting volume to "+volume);

        // If we ended up here, there should be music playing on the Bluetooth device,
        // so we can finally adjust the volume
        adjustAudio(context,volume);
    }
}

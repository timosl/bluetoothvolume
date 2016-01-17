package de.timosl.bluetoothvolumeadjust;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A custom {@link DialogFragment} that allows the user to
 * add a new {@link BluetoothDevice}.
 */
public class NewDeviceDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate the view for this dialog
        final View dialog_root = getActivity().getLayoutInflater().inflate(R.layout.dialog_newdevice,null);

        // Get a reference to the Spinner and the SeekBar
        final Spinner dialog_spinner = (Spinner) dialog_root.findViewById(R.id.dialog_newdevice_list);
        final SeekBar dialog_seekbar = (SeekBar) dialog_root.findViewById(R.id.dialog_newdevice_seekbar);
        final TextView dialog_seekbar_label = (TextView) dialog_root.findViewById(R.id.dialog_newdevice_seekbar_label);

        // Tell the Seekbar to update the label when the user drags on it
        dialog_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dialog_seekbar_label.setText(""+ progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // The Seekbar and its label start at 0%
        dialog_seekbar_label.setText("0%");
        dialog_seekbar.setProgress(0);

        // We only want to display all bonded devices we're not already managing
        final List<BluetoothDevice> devices = getUnusedDevices();

        // This ArrayAdapter will contain the names of the BluetoothDevices we will show to the user
        final ArrayAdapter<String> dialog_listAdapter = new ArrayAdapter<>(getActivity(),android.R.layout.select_dialog_item);

        // Add the names of the devices to the adapter
        // and set the Spinner to it
        for(BluetoothDevice device: devices) {
            dialog_listAdapter.add(device.getName());
        }
        dialog_spinner.setAdapter(dialog_listAdapter);

        // Build a new dialog based on the Views we created previously
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialog_root)
                .setTitle(R.string.dialog_newdevice_title)
                .setIcon(R.drawable.bluetooth)
                .setPositiveButton(R.string.dialog_newdevice_add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Do nothing if the user has not properly selected a device
                        if (dialog_spinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
                            return;
                        }

                        // Get the BluetoothDevice that was selected by the user
                        BluetoothDevice selectedDevice = devices.get(dialog_spinner.getSelectedItemPosition());

                        // Get the volume and convert it to a range from 0.0 to 1.0
                        float volume = ((float) dialog_seekbar.getProgress()) / 100f;

                        // This is the volume as an integer from 0 to 100. We use this
                        // later to display it in the Snackbar
                        int volumePercentage = (int) (volume * 100);

                        // Add the device to our global list
                        DeviceManagment.addDevice(getActivity(), selectedDevice.getAddress(), volume);

                        // Tell the MainActivity to update its list
                        // of devices if possible
                        if (getActivity() instanceof MainActivity) {
                            MainActivity act = (MainActivity) getActivity();
                            act.refreshDevices();
                        }

                        // Display a Snackbar notification showing the device name and the volume
                        // Since the Fragment is not inside the View tree of MainActivity, we have to
                        // pass a View that does explicitly. The RecyclerView has an ID already, so we
                        // can just use that
                        Snackbar.make(getActivity().findViewById(R.id.activity_main_deviceList), String.format(getString(R.string.snackbar_added_item), selectedDevice.getName(), volumePercentage), Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.dialog_newdevice_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // We don't have to do anything here, but just passing 'null'
                        // as the OnClickListener would cause crashes
                    }
                });

        // Create the Dialog and return it
        return builder.create();
    }

    /**
     * Gets a list of {@link BluetoothDevice}s that are bonded with this
     * device, but not already managed by this app.
     * @return A list of {@link BluetoothDevice}s that are bonded with this
     * device, but not already managed by this app.
     */
    private List<BluetoothDevice> getUnusedDevices() {
        // If there is no Bluetooth adapter available, we
        // cannot query it for devices (obviously)
        // This case shouldn't occur here, but we have to make sure anyway
        if(BluetoothAdapter.getDefaultAdapter() == null) {
            return new ArrayList<>();
        }

        // The devices we are bonded with (containing devices we already manage in this app)
        final List<BluetoothDevice> devices = new ArrayList<>(BluetoothAdapter.getDefaultAdapter().getBondedDevices());

        // The devices we are already managing
        final Set<String> usedDevices = DeviceManagment.getDevices(getActivity());

        // The list of devices we want to return
        final List<BluetoothDevice> unusedDevices = new ArrayList<>();

        // Iterate over all the bonded devices and add only
        // the ones to our list we are not already managing
        for(BluetoothDevice device: devices) {
            if(!usedDevices.contains(device.getAddress())) {
                unusedDevices.add(device);
            }
        }

        return unusedDevices;
    }
}

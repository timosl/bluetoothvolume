package de.timosl.bluetoothvolumeadjust.ui;

import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.timosl.bluetoothvolumeadjust.util.DeviceManagment;
import de.timosl.bluetoothvolumeadjust.util.L;
import de.timosl.bluetoothvolumeadjust.util.Preferences;
import de.timosl.bluetoothvolumeadjust.R;

/**
 * The main {@link AppCompatActivity} that the user
 * will see most of the time. Here the user can add new
 * devices to manage and remove existing ones.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * The {@link RecyclerView} that will display the devices
     * managed by the app.
     */
    private RecyclerView deviceList;

    /**
     * The {@link DeviceListAdapter} that will contain the data
     * for the {@link #deviceList}.
     */
    private DeviceListAdapter deviceListAdapter;

    /**
     * The {@link FloatingActionButton} that allows a user to
     * add a new device.
     */
    private FloatingActionButton newDeviceFAB;

    /**
     * The debug option how it was set the last time this
     * activity was created. Used for determining whether
     * to show the debug menu entry or not.
     */
    private boolean displayDebugOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the Toolbar from the support library
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add the RecyclerView instance
        deviceList = (RecyclerView) findViewById(R.id.activity_main_deviceList);
        deviceList.setHasFixedSize(true); // Allows for some optimizations in the RV

        // Add the NewDeviceFAB instance
        newDeviceFAB = (FloatingActionButton) findViewById(R.id.activity_main_newDeviceFAB);

        // Set the LayoutManager for the RV. The StaggerdGridLayoutManager
        // is an all-rounder that gets the job done
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        deviceList.setLayoutManager(layoutManager);

        // Set our adapter to the RV
        deviceListAdapter = new DeviceListAdapter(getApplication());
        deviceList.setAdapter(deviceListAdapter);

        // Get the initial list of devices
        deviceListAdapter.updateDevices();

        // This callback will handle swipes (no drags) on items inside the RV
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // We need to cast the ViewHolder to our custom class
                DeviceListAdapter.ViewHolder mViewHolder = (DeviceListAdapter.ViewHolder) viewHolder;

                // Get the BluetoothDevice for the ViewHolder that was swiped
                final BluetoothDevice device = DeviceManagment.getDeviceByAddress(mViewHolder.deviceAddress);

                // Get the values for the device, so we can use them in case
                // the user wants to undo the deletion
                final String deviceAddress = device.getAddress();
                final String deviceName = device.getName();
                final float deviceVolume = DeviceManagment.getDeviceVolume(getApplication(),deviceAddress);

                // Remove the device that was swiped from the list
                // and tell our adapter about it
                DeviceManagment.removeDevice(getApplication(), deviceAddress);
                deviceListAdapter.updateDevices();

                // Create a Snackbar notification that allows the user to undo the deletion
                Snackbar snackbar = Snackbar.make(deviceList, String.format(getString(R.string.snackbar_deleted_item),device.getName()), Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.snackbar_deletion_undo_action, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DeviceManagment.addDevice(getApplication(),deviceAddress,deviceVolume);
                        deviceListAdapter.updateDevices();
                        Snackbar.make(deviceList,String.format(getString(R.string.snackbar_deletion_undone),deviceName),Snackbar.LENGTH_SHORT).show();
                    }
                });
                snackbar.show();
            }
        };

        // Attach the ItemTouchHelper to the RV
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(deviceList);

        // Check if the debug options should be displayed
        displayDebugOption = Preferences.getEnableDebugging(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restart the acitivty if the debug preference has changed
        // so we can recreate the context menu
        if(displayDebugOption != Preferences.getEnableDebugging(this)) {
            recreate();
        }
    }

    private void checkNewDeviceFABVisibility() {
        // Check if we can even add more devices. If not, we
        // hide the NewDeviceFAB
        if(isNewDevicesAvailable()) {
            // We have more devices available, so we can continue
            // showing the NewDeviceFAB
            newDeviceFAB.show();
        } else {
            if(BluetoothAdapter.getDefaultAdapter() == null || !BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                Snackbar.make(newDeviceFAB,R.string.snackbar_err_no_adapterAvailable,Snackbar.LENGTH_INDEFINITE).show();
            }

            // If there are new devices available to add and we
            // don't have any right now, something is wrong.
            // We better let the user know.
            else if(DeviceManagment.getDevices(this).size() == 0) {
                Snackbar.make(newDeviceFAB,R.string.snackbar_err_no_devicesAvailable,Snackbar.LENGTH_INDEFINITE).show();
            }

            // Hide the NewDeviceFAB if no more devices can be added
            newDeviceFAB.hide();
        }
    }

    /**
     * Refreshes the list of managed devices in the {@link #deviceList}.
     */
    public void refreshDevices() {
        deviceListAdapter.updateDevices();
    }

    /**
     * Displays the {@link NewDeviceDialog} to allow the user to
     * add a new device.
     * @param view The view that called this method (probably a {@link Button})
     */
    public void showNewDeviceDialog(View view) {
        DialogFragment newFragment = new NewDeviceDialog();
        newFragment.show(getFragmentManager(), "dialog");
    }

    /**
     * Checks if the {@link #newDeviceFAB} has to be shown to
     * the user. It should not be shown if no new device can
     * be added. That is the case if we already have added
     * all bonded devices, or if the {@link BluetoothAdapter}
     * is not available.
     * @return Returns 'true' only, if the {@link #newDeviceFAB}
     * should be shown to the user and 'false' otherwise
     */
    private boolean isNewDevicesAvailable() {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            L.w("(MainActivity) There is no bluetooth adapter available, there may be a hardware error or the Bluetooth permission has been revoked");
            return false;
        }

        if (BluetoothAdapter.getDefaultAdapter().getBondedDevices().size() == 0) {
            L.w("(MainActivity) There are no devices paired");
            return false;
        }

        for(BluetoothDevice device: BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if(!DeviceManagment.getDevices(this).contains(device.getAddress())) {
                return true;
            }
        }

        L.i("(MainActivity) No device left to manage");
        return false;
    }

    /**
     * The custom {@link RecyclerView.Adapter} that contains the devices for
     * displaying them inside the {@link #deviceList}.
     */
    private class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
        /**
         * The list of {@link BluetoothDevice}s to be displayed in the {@link #deviceList}.
         */
        private List<BluetoothDevice> devices = new ArrayList<>();

        /**
         * The applications {@link Context}.
         */
        private Context context;

        /**
         * Creates a new {@link DeviceListAdapter} with the given application {@link Context}.
         * @param context
         */
        public DeviceListAdapter(Context context) {
            this.context = context;
        }

        /**
         * Update the list of {@link BluetoothDevice}s to match the list
         * of devices stored in the applications {@link SharedPreferences}.
         */
        public void updateDevices() {
            // Clear all items, we will get a fresh list later
            devices.clear();

            // Make sure that the Bluetooth Adapter is available to us
            // or we will fail later on
            if(BluetoothAdapter.getDefaultAdapter() == null) {
                // If we end up here, we probably have to hide the NewDeviceFAB
                checkNewDeviceFABVisibility();
                return;
            }

            // Get all device addresses we currently manage
            final Set<String> deviceAddresses = DeviceManagment.getDevices(context);

            // Get the list of all devices bonded with this device
            final List<BluetoothDevice> bondedDevices = new ArrayList<>(BluetoothAdapter.getDefaultAdapter().getBondedDevices());

            // Iterate over all of them to add them to our list
            for(BluetoothDevice device: bondedDevices) {
                // Only add the ones we are actually managing
                if(deviceAddresses.contains(device.getAddress())) {
                    devices.add(device);
                }
            }

            // Tell the adapter that the list is updated
            notifyDataSetChanged();

            // If the device list has changed, we may have toggle the
            // visibility of the NewDeviceFAB
            checkNewDeviceFABVisibility();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View viewRoot = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_list, parent, false);
            ViewHolder viewHolder = new ViewHolder(viewRoot);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // Get the device for this ViewHolder
            final BluetoothDevice device = devices.get(position);

            // Set the attributes for the ViewHolder
            holder.deviceAddress = device.getAddress();
            holder.name.setText(device.getName());
            holder.bar.setProgress((int) (DeviceManagment.getDeviceVolume(context,device.getAddress()) * 100f));
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        /**
         * A custom {@link RecyclerView.ViewHolder} that contains
         * the {@link View}s inside the items displayed inside
         * the {@link #deviceList}.
         */
        class ViewHolder extends RecyclerView.ViewHolder {

            /**
             * The devices address, so we can associate it with
             * a {@link BluetoothDevice} later. (for the Swipe
             * handler for example)
             */
            public String deviceAddress;

            /**
             * The {@link TextView} displaying the name of the {@link BluetoothDevice}.
             */
            public TextView name;

            /**
             * The {@link ProgressBar} displaying the volume used for the {@link BluetoothDevice}.
             */
            public ProgressBar bar;

            /**
             * Creates a new {@link ViewHolder} associated with the given {@link View}.
             * @param view The {@link View} associated with the new {@link ViewHolder}.
             */
            public ViewHolder(View view) {
                super(view);
                this.name = (TextView) view.findViewById(R.id.item_device_list_name);
                this.bar = (ProgressBar) view.findViewById(R.id.item_device_list_bar);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Start the AboutActivity if the associated menu item
            // was selected by the user
            case R.id.menu_main_about: {
                Intent intent = new Intent(this,AboutActivity.class);
                startActivity(intent);
                return true;
            }

            case R.id.menu_main_sendReport: {
                sendProblemReport();
                return true;
            }

            // Start the PreferenceActivity if the associated menu item
            // was selected by the user
            case R.id.menu_main_settings: {
                Intent intent = new Intent(this,CustomPreferenceActivity.class);
                startActivity(intent);
                return true;
            }

            // Default case when something else was selected
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // If the debug preference is not set, hide the corresponding
        // context menu entry
        if(!Preferences.getEnableDebugging(this)) {
            menu.findItem(R.id.menu_main_sendReport).setVisible(false);
        }

        return true;
    }

    /**
     * Opens an E-Mail application to share the problem report.
     */
    private void sendProblemReport() {
        // The E-Mail contents
        String header = "-> Add a description of your problem here <-\n\n==============Do not change anything under this line============\n\n";
        String report = L.getLog();

        // Create the intent
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("text/plain");
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"tsdev@posteo.de"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Bluetooth Volume Adjust Problem Report");
        intent.putExtra(Intent.EXTRA_TEXT, header+report);

        // Start the E-Mail application
        startActivity(Intent.createChooser(intent, "Send Report"));
    }
}


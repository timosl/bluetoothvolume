package de.timosl.bluetoothvolumeadjust;

import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

/**
 * The main (and only) {@link AppCompatActivity} that the user
 * will be able to interact with. Here the user can add new
 * devices to manage and remove existing ones.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * The {@link Log} prefix.
     */
    public static final String TAG = "bluetoothAdjust";

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
    }

    private void checkNewDeviceFABVisibility() {
        // Check if we can even add more devices. If not, we
        // hide the NewDeviceFAB
        if(isNewDevicesAvailable()) {
            // We have more devices available, so we can continue
            // showing the NewDeviceFAB
            newDeviceFAB.show();
        } else {
            // If there are new devices available to add and we
            // don't have any right now, something is wrong.
            // We better let the user know.
            if(DeviceManagment.getDevices(this).size() == 0) {
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
        return BluetoothAdapter.getDefaultAdapter() != null
                && BluetoothAdapter.getDefaultAdapter().getBondedDevices().size() > 0
                && BluetoothAdapter.getDefaultAdapter().getBondedDevices().size() != DeviceManagment.getDevices(this).size();
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
                Log.w(TAG,"There is no bluetooth adapter available!");
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
        // Start the AboutActivity if the associated menu item
        // was selected by the user
        if(item.getItemId() == R.id.menu_main_about) {
            Intent intent = new Intent(this,AboutActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }
}


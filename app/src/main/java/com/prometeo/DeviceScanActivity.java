package com.prometeo;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity {
    public static final String USER_ID = "USER_ID";

    private ArrayAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    ListView listDevices;
    Button buttonAddDevice;
    Button buttonScanDevice;

    private String user_id;

    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    ArrayList<String> addresses = new ArrayList<>();
    ArrayList<String> deviceNames = new ArrayList<>();


    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);
        mHandler = new Handler();

        final Intent intent = getIntent();

        user_id = intent.getStringExtra(USER_ID);

        listDevices = findViewById(R.id.listDevices);
        buttonScanDevice = findViewById(R.id.buttonScanDevice);
        buttonAddDevice = findViewById(R.id.buttonAddDevice);


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        buttonAddDevice.setEnabled(false);
        buttonAddDevice.setBackgroundColor(Color.parseColor("#646464"));


        // Initializes list view adapter.
        mLeDeviceListAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,deviceNames);

        listDevices.setAdapter(mLeDeviceListAdapter);


        listDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView < ? > adapter, View view,int position, long arg) {
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                buttonScanDevice.setEnabled(true);
                buttonAddDevice.setEnabled(true);
                buttonAddDevice.setBackgroundColor(Color.parseColor("#1764EC"));

            }
        });
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    @Override
//    protected void onPause() {
//        super.onPause();
//    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        buttonScanDevice.setEnabled(true);
    }


    // Device scan callback.
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String name = device.getName();
                            String address = device.getAddress();
                            if (!addresses.contains(address) & name!=null) {
                                if (name.indexOf("Prometeo") != -1) {
                                    addresses.add(address);
                                    bluetoothDevices.add(device);
                                    deviceNames.add(name);
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });
                }
            };


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void scanClicked(View view) {
        buttonScanDevice.setEnabled(false);
        buttonAddDevice.setEnabled(false);
        mLeDeviceListAdapter.clear();
        bluetoothDevices.clear();
        addresses.clear();
        deviceNames.clear();
        scanLeDevice(true);
    }

    public void addClicked(View view) {
        Intent intent;



        final BluetoothDevice device = bluetoothDevices.get(listDevices.getCheckedItemPosition());
        if (device == null) return;

//        Log.i("Device Selected", "Name: " + device);

        String name = device.getName();
        String address = device.getAddress();

        intent = new Intent(DeviceScanActivity.this, DeviceDashboard.class);
        intent.putExtra(DeviceDashboard.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceDashboard.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(DeviceDashboard.USER_ID, user_id);

        startActivity(intent);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLeDeviceListAdapter.clear();
        bluetoothDevices.clear();
        addresses.clear();
        deviceNames.clear();


    }

}


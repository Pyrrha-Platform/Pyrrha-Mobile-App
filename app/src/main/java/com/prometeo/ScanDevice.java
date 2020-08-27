package com.prometeo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.prometeo.ui.login.LoginActivity;

import java.util.ArrayList;

public class ScanDevice extends AppCompatActivity {

    ListView listDevices;
    Button buttonAddDevice;
    Button buttonScanDevice;
    ArrayList<String> bluetoothDevices = new ArrayList<>();
    ArrayList<String> addresses = new ArrayList<>();
    ArrayAdapter arrayAdapter;


    BluetoothAdapter bluetoothAdapter;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("Action",action);

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                buttonScanDevice.setEnabled(true);
                buttonAddDevice.setEnabled(true);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String address = device.getAddress();
                String rssi = Integer.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE));

                Log.i("Device Found","Name: " + name + " Address: " + address + " RSSI: " + rssi);

                if (!addresses.contains(address)) {
                    addresses.add(address);
                    String deviceString = "";
                    if (name == null || name.equals("")) {
                        deviceString = address + " - RSSI " + rssi + "dBm";
                    } else {
                        deviceString = name + " - RSSI " + rssi + "dBm";
                    }

                    bluetoothDevices.add(deviceString);
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    public void scanClicked(View view) {
        buttonScanDevice.setEnabled(false);
        buttonAddDevice.setEnabled(false);
        bluetoothDevices.clear();
        addresses.clear();
        bluetoothAdapter.startDiscovery();
    }

    public void addClicked(View view) {
        Intent intent;

        intent = new Intent(ScanDevice.this, MainActivity.class);

        startActivity(intent);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_scan_device);

        listDevices = findViewById(R.id.listDevices);
        buttonScanDevice = findViewById(R.id.buttonScanDevice);
        buttonAddDevice = findViewById(R.id.buttonAddDevice);

        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        androidx.core.app.ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,bluetoothDevices);

        listDevices.setAdapter(arrayAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, intentFilter);

    }
}
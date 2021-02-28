package com.prometeo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.prometeo.ble.BluetoothLeService;
import com.prometeo.db.AppDatabase;
import com.prometeo.db.PrometeoTable;
import com.prometeo.io.RetrofitAdapter;
import com.prometeo.io.RetrofitService;
import com.prometeo.io.StatusCloud;
import com.prometeo.iot.IoTClient;
import com.prometeo.utils.Constants;
import com.prometeo.utils.MessageFactory;
import com.prometeo.utils.MyIoTActionListener;
import com.prometeo.utils.PrometeoEvent;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import javax.net.SocketFactory;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class DeviceDashboard extends AppCompatActivity {

    private final static String TAG = DeviceDashboard.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String USER_ID = "USER_ID";

    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private String user_id;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mGattCharacteristic;
    private BluetoothGattCharacteristic mGattDateTime;
    private BluetoothGattCharacteristic mGattStatusCloud;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    // Variable to maintain the app connected in mobile sleeping mode
    private  PowerManager.WakeLock mWakeLock;


    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private UUID uuidService = UUID.fromString("2c32fd5f-5082-437e-8501-959d23d3d2fb");
    private UUID uuidCharacteristic = UUID.fromString("dcaaccb4-c1d1-4bc4-b406-8f6f45df0208");
    private UUID uuidDateTime = UUID.fromString("e39c34e9-d574-47fc-a66e-425cec812aab");
    private UUID uuidStatusCloud = UUID.fromString("125ad2af-97cd-4f7a-b1e2-5109561f740d");

    private BluetoothGattService mGattService;

    Button valueTemperature;
    Button valueHumidity;
    Button valueCO;
    Button valueNO2;

    ImageView imgBluetooh;
    ImageView imgConnectivity;

    Handler handler = new Handler();

    Context context;
    IoTStarterApplication app;
    BroadcastReceiver iotBroadCastReceiver;

    Call<StatusCloud> callStatus;
    Retrofit retrofit;
    RetrofitService retrofitService;

    AppDatabase db;

    boolean connectivity = true;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint("NewApi")
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                imgBluetooh.setVisibility(View.INVISIBLE);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                imgBluetooh.setVisibility(View.VISIBLE);
                //               clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                updateDateTime();
                System.out.println("Estamos en ACTION_GATT_SERVICES_DISCOVERED");


                handler.postDelayed(new Runnable() {
                    public void run() {
                        displayGattService();
                    }
                }, 10000); // 10 seconds of "delay"


            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    public void run() {
                        displayGattService();
                    }
                }, 10000); // 10 seconds of "delay"

                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

            }
        }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_dashboard);

        // We maintain the screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        mWakeLock.acquire();
        // end of block to maintain the screen on

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        user_id = intent.getStringExtra(USER_ID);


        valueTemperature = findViewById(R.id.btTemperature);
        valueHumidity = findViewById(R.id.btHumidity);
        valueCO = findViewById(R.id.btCO);
        valueNO2 = findViewById(R.id.btNO2);

        imgBluetooh = findViewById(R.id.imgBluetooth);
        imgConnectivity = findViewById(R.id.imgConnectivity);


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        System.out.println("CREAMOS LA BASE DE DATOS");
        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "prometeo").build();


        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        context = this.getApplicationContext();
        // connect to the IoT platform and send random readings for now
        // how are these variables entered? How are they updated in the Application?
        app = (IoTStarterApplication) this.getApplication();
        app.setCurrentRunningActivity(TAG);

        if (iotBroadCastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering iotBroadcastReceiver");
            iotBroadCastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for iotBroadcastReceiver");
                }
            };
        }

        this.getApplicationContext().registerReceiver(iotBroadCastReceiver,
                new IntentFilter(Constants.APP_ID + Constants.INTENT_IOT));


        app.setDeviceType(Constants.DEVICE_TYPE);
        app.setDeviceId(user_id.replace("@", "-"));   // TO-DO: check this part
        app.setOrganization("p0g2ka");
        app.setAuthToken("prometeopriegoholaquetal");

        Log.d(TAG, "We are going to create the iotClient");
        IoTClient iotClient = IoTClient.getInstance(context, app.getOrganization(), app.getDeviceId(), app.getDeviceType(), app.getAuthToken());

        try {
            SocketFactory factory = null;
            // need to implement ssl here
            Log.d(TAG, "We are going to create the listener");

            MyIoTActionListener listener = new MyIoTActionListener(context, Constants.ActionStateStatus.CONNECTING);
            Log.d(TAG, "Listener created");

            //start connection - if this method returns, connection has not yet happened
            iotClient.connectDevice(app.getMyIoTCallbacks(), listener, factory);
            Log.d(TAG, ".start connection");

            // iotClient.disconnectDevice(listener);
            Log.d(TAG, ".connectDevice finished");

        } catch (MqttException e) {
            if (e.getReasonCode() == (Constants.ERROR_BROKER_UNAVAILABLE)) {
                // error while connecting to the broker - send an intent to inform the user
//                Intent actionIntent = new Intent(Constants.ACTION_INTENT_CONNECTIVITY_MESSAGE_RECEIVED);
//                actionIntent.putExtra(Constants.CONNECTIVITY_MESSAGE, Constants.ERROR_BROKER_UNAVAILABLE);
//                context.sendBroadcast(actionIntent);
                imgConnectivity.setVisibility(View.VISIBLE);

            }
        }

        // We use retrofit to call the api res
        retrofit = new RetrofitAdapter().getAdapter();
        retrofitService = retrofit.create(RetrofitService.class);


    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        unregisterReceiver(mGattUpdateReceiver);
//    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        unbindService(mServiceConnection);
//        mBluetoothLeService = null;
//    }




    @RequiresApi(api = Build.VERSION_CODES.O)
    private void displayData(String data) {
        if (data != null) {

            // We display the data in the mobile screen
            String[] parts = data.split(" ");

 //           System.out.println("RS CO: " + parts[8]);

            // tempValue, tempValueStDev, humValue, humValueStDev, coValue, coValueStDev, no2Value, no2ValueStDev
            valueTemperature.setText(parts[0]+"\n celsius");
            valueHumidity.setText(parts[2]+"\n %");



//            if (Float.parseFloat(parts[4]) < 0 || Float.parseFloat(parts[4]) > 1000)
//                valueCO.setText("##.## \n ppm");
//            else
                valueCO.setText(parts[4]+"\n ppm");

//            if (Float.parseFloat(parts[6]) < 0 || Float.parseFloat(parts[6]) > 10)
//                valueNO2.setText("##.## \n ppm");
//            else
                valueNO2.setText(parts[6]+"\n ppm");

   //         System.out.println("RS CO: " + parts[8]);
//            System.out.println("RS NO2: " + parts[9]);


            final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            f.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date device_timestamp = new Date();

            // create a random PrometeoEvent
            PrometeoEvent pe = new PrometeoEvent();
            pe.setFirefighter_id(user_id);
            pe.setDevice_id(mDeviceName);
            pe.setDevice_battery_level("0");
            pe.setAcrolein((float) 0.0);
            pe.setBenzene((float) 0.0);
            pe.setCarbon_monoxide(Float.parseFloat(parts[4]));
            pe.setFormaldehyde((float) 0.0);
            pe.setNitrogen_dioxide(Float.parseFloat(parts[6]));
            pe.setTemperature(Float.parseFloat(parts[0]));
            pe.setHumidity(Float.parseFloat(parts[2]));
            pe.setDevice_timestamp(f.format(device_timestamp));


            // We send the data to the cloud through IOT Platform
            try {
                sendData(pe, device_timestamp);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("It was not possible to send the data, so we storage in the database");
                savePrometeoEvent(pe, device_timestamp);
                imgConnectivity.setVisibility(View.VISIBLE);
                connectivity = false;
            }

        // We get the status from the cloud
            getStatus(pe, device_timestamp);

            if (connectivity == true) {
                sendDataDatabase();
            }
        }

    }

    private Date addMinutes(Date date, int amount){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, amount);

        return calendar.getTime();
    }

    private void getStatus(PrometeoEvent pe, Date device_timestamp) {

        final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm"); // it has to be without the seconds

        f.setTimeZone(TimeZone.getTimeZone("UTC"));


//        callStatus = retrofitService.get_status(user_id, f.format(new Date())); // TO-DO: timestamp of the device
//        callStatus = retrofitService.get_status("1999", f.format(new Date()).toString()+":00"); // TO-DO: timestamp of the device
        callStatus = retrofitService.get_status(pe.getFirefighter_id(), f.format(addMinutes(device_timestamp, -2))+":00");
//        callStatus = retrofitService.get_status(pe.getFirefighter_id(), "2020-11-13 00:21:00"); // TO-DO: timestamp of the device


        Log.d(TAG, "callStatus created");
        callStatus.enqueue(new Callback<StatusCloud>() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onResponse(Call<StatusCloud> callStatus, Response<StatusCloud> response) {
                if (!response.isSuccessful()) {
                    Log.d(TAG, "Error when calling to get_status: "+response.code());
                    return;
                }
                else {

                    System.out.println("*** Status cloud get");
                    System.out.print("Firefighter: " + response.body().getFirefighter_id());
                    System.out.print(" Status color: " + response.body().getStatus());
                    System.out.println(" Timestamp: " + response.body().getTimestamp_mins());

                    updateStatusCloud(response.body().getStatus());

                }

            }

            @Override
            public void onFailure(Call<StatusCloud> callStatus, Throwable t) {
                if (t.getCause()!=null)
                    System.out.println(t.getCause().toString());

                if (imgConnectivity.getVisibility() == View.INVISIBLE) {
                    imgConnectivity.setVisibility(View.VISIBLE);
                }

        }
        });


    }


    private void savePrometeoEvent(PrometeoEvent pe, Date device_timestamp) {
        System.out.println("No communication - we save the data into the prometeo lite database in local");

        System.out.println("Create object to insert in database");
        final PrometeoTable prometeoTable = new PrometeoTable();

        prometeoTable.device_timestamp = pe.getDevice_timestamp();
        prometeoTable.firefighter_id = pe.getFirefighter_id();
        prometeoTable.device_id = pe.getDevice_id();
        prometeoTable.device_battery_level = pe.getDevice_battery_level();
        prometeoTable.temperature = pe.getTemperature();
        prometeoTable.humidity = pe.getHumidity();
        prometeoTable.carbon_monoxide = pe.getCarbon_monoxide();
        prometeoTable.nitrogen_dioxide = pe.getNitrogen_dioxide();
        prometeoTable.formaldehyde = pe.getFormaldehyde();
        prometeoTable.acrolein = pe.getAcrolein();
        prometeoTable.benzene = pe.getBenzene();


        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("Insert row in database");
                try {
                    db.prometeoDao().insertAll(prometeoTable);
                } catch (Exception e) {
                    System.out.println("It was not possible to insert the row");
                }

//                PrometeoTable user_query = new PrometeoTable();
//                System.out.println("LEO LA FILA");
//                try {
//                    user_query = db.prometeoDao().getAll();
//                } catch (Exception e) {
//                    System.out.println("NO SE HA ENCONTRADO EL REGISTRO");
//                }

            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendData(PrometeoEvent pe, Date device_timestamp) throws Exception {
        try {


            // create ActionListener to handle message published results
            MyIoTActionListener listener = new MyIoTActionListener(context, Constants.ActionStateStatus.PUBLISH);
            IoTClient iotClient = IoTClient.getInstance(context);
//            String messageData = MessageFactory.getPrometeoDeviceMessage(getMacAddress(this.getApplicationContext()), pe);
            String messageData = MessageFactory.getPrometeoDeviceMessage(pe);


            System.out.println("Estamos en el sitio");
            System.out.println(messageData.toString());


            iotClient.publishEvent(Constants.TEXT_EVENT, "json", messageData, 0, false, listener);

            int count = app.getPublishCount();
            app.setPublishCount(++count);

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null && runningActivity.equals(this.getClass().getName())) {
                Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
                actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_PUBLISHED);
                context.sendBroadcast(actionIntent);
            }

            if (imgConnectivity.getVisibility() == View.VISIBLE) {
                imgConnectivity.setVisibility(View.INVISIBLE);
                connectivity = true;
            }

            if(iotClient.isMqttConnected()) {
                System.out.println("****** ESTAMOS CONECTADOS ****+");

            }
            else {
                System.out.println("****** AHORA ESTAMOS DESCONECTADOS ****+");

            }
        } catch (MqttException e) {
            e.printStackTrace();

            System.out.println("It was not possible to send data, we launch an exception");

//            throw new Exception("Data not sent", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendDataDatabase() {
        final PrometeoEvent pe = new PrometeoEvent();
        final SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        System.out.println("Create object to insert in database");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<PrometeoTable> user_query = new ArrayList<PrometeoTable>();
                System.out.println("Read the row");
                try {
                    user_query = (ArrayList<PrometeoTable>) db.prometeoDao().getAll();
                    System.out.println("There are " + user_query.size() + " rows in the database");

                    int i;
                    for (i=0;i<user_query.size();i++) {
                        pe.setDevice_timestamp(user_query.get(i).device_timestamp);
                        pe.setFirefighter_id(user_query.get(i).firefighter_id);
                        pe.setDevice_id(user_query.get(i).device_id);
                        pe.setDevice_battery_level(user_query.get(i).device_battery_level);
                        pe.setTemperature(user_query.get(i).temperature);
                        pe.setHumidity(user_query.get(i).humidity);
                        pe.setCarbon_monoxide(user_query.get(i).carbon_monoxide);
                        pe.setNitrogen_dioxide(user_query.get(i).nitrogen_dioxide);
                        pe.setFormaldehyde(user_query.get(i).formaldehyde);
                        pe.setAcrolein(user_query.get(i).acrolein);
                        pe.setBenzene(user_query.get(i).benzene);

                        sendData(pe, f.parse(user_query.get(i).device_timestamp));
                        db.prometeoDao().delete(user_query.get(i));
                    }

                } catch (Exception e) {
                    System.out.println("THERE WERE NO ROWS");
                }
            }
        });

    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void displayGattService() {

        if (mBluetoothLeService.mBluetoothGatt == null) {
            return;
        }

        try {
            mGattCharacteristic = mBluetoothLeService.getGattService(uuidService).getCharacteristic(uuidCharacteristic);

            if (mGattCharacteristic != null) {
                final BluetoothGattCharacteristic characteristic = mGattCharacteristic;
                final int charaProp = characteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService.setCharacteristicNotification(
                                mNotifyCharacteristic, false);
                        mNotifyCharacteristic = null;

                    }
                    mBluetoothLeService.readCharacteristic(characteristic);
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    mNotifyCharacteristic = characteristic;
                    mBluetoothLeService.setCharacteristicNotification(
                            characteristic, true);
                }

            }
//            if (imgBluetooh.getVisibility() == View.VISIBLE) {
//                imgBluetooh.setVisibility(View.INVISIBLE);
//            }
        } catch (Exception e) {
            imgBluetooh.setVisibility(View.VISIBLE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void updateDateTime() {

//        mGattService = mBluetoothLeService.getGattService(uuidService);
        mGattDateTime = mBluetoothLeService.getGattService(uuidService).getCharacteristic(uuidDateTime);

        if (mGattDateTime != null) {
            Date currentTime = Calendar.getInstance().getTime(); // convert into utc
            mGattDateTime.setValue(currentTime.toString());
            mBluetoothLeService.writeCharacteristic(mGattDateTime);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void updateStatusCloud(Integer status_choice) {

//        mGattService = mBluetoothLeService.getGattService(uuidService);
        mGattStatusCloud = mBluetoothLeService.getGattService(uuidService).getCharacteristic(uuidStatusCloud);


        if (status_choice == -1) {
//            Integer[] status_choices = new Integer[3];
//            Random ga = new Random();

//            status_choices[0] = 3;   // red
//            status_choices[1] = 2;    // yellow
//            status_choices[2] = 1;    // green

//            int random_number = ga.nextInt(3);

//            status_choice = status_choices[random_number];

            status_choice = 3;  // red

        }
        if (mGattStatusCloud != null) {
            mGattStatusCloud.setValue(status_choice.toString());
            mBluetoothLeService.writeCharacteristic(mGattStatusCloud);
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }



    public void scanClicked(View view) {
        Intent intent;

        intent = new Intent(DeviceDashboard.this, DeviceScanActivity.class);
        intent.putExtra(DeviceScanActivity.USER_ID, user_id);

        startActivity(intent);

    }

    public void homeClicked(View view) {
        Intent intent;

        intent = new Intent(DeviceDashboard.this, DeviceScanActivity.class);
        intent.putExtra(DeviceScanActivity.USER_ID, user_id);

        startActivity(intent);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService.close();
        handler.removeCallbacksAndMessages(null);

        // we have to release the variablre to maintain the app connected
        mWakeLock.release();
    }


}
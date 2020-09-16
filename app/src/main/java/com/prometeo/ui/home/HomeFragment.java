package com.prometeo.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.prometeo.IoTStarterApplication;
import com.prometeo.R;
import com.prometeo.iot.IoTClient;
import com.prometeo.utils.Constants;
import com.prometeo.utils.MessageFactory;
import com.prometeo.utils.MyIoTActionListener;
import com.prometeo.utils.PrometeoEvent;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Random;

import javax.net.SocketFactory;

public class HomeFragment extends Fragment {

    private final static String TAG = HomeFragment.class.getName();
    Context context;
    IoTStarterApplication app;
    BroadcastReceiver broadcastReceiver;


    private HomeViewModel homeViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    private void sendData() {
        try {
            Random random = new Random();
            // create a random PrometeoEvent
            PrometeoEvent pe = new PrometeoEvent();
            pe.setAcroleine(1 + random.nextFloat() * (100 - 1));
            pe.setBenzene(1 + random.nextFloat() * (100 - 1));
            pe.setCO(1 + random.nextFloat() * (100 - 1));
            pe.setFormaldehyde(1 + random.nextFloat() * (100 - 1));
            pe.setNo2(1 + random.nextFloat() * (100 - 1));
            pe.setTemp(1 + random.nextFloat() * (100 - 1));
            pe.setHumidity(1 + random.nextFloat() * (100 - 1));

            // create ActionListener to handle message published results
            MyIoTActionListener listener = new MyIoTActionListener(context, Constants.ActionStateStatus.PUBLISH);

            IoTClient iotClient = IoTClient.getInstance(context);

            String messageData = MessageFactory.getPrometeoDeviceMessage(getMacAddress(getContext()), pe);
            iotClient.publishEvent(Constants.TEXT_EVENT, "json", messageData, 0, false, listener);

            int count = app.getPublishCount();
            app.setPublishCount(++count);

            String runningActivity = app.getCurrentRunningActivity();
            if (runningActivity != null && runningActivity.equals(HomeFragment.class.getName())) {
                Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_IOT);
                actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_PUBLISHED);
                context.sendBroadcast(actionIntent);
            }
        } catch (MqttException e) {
            // Publish failed
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume entered");

        context = getActivity().getApplicationContext();
        // connect to the IoT platform and send random readings for now
        // how are these variables entered? How are they updated in the Application?
        app = (IoTStarterApplication) getActivity().getApplication();
        app.setCurrentRunningActivity(TAG);

        if (broadcastReceiver == null) {
            Log.d(TAG, ".onResume() - Registering iotBroadcastReceiver");
            broadcastReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, ".onReceive() - Received intent for iotBroadcastReceiver");
                }
            };
        }

        getActivity().getApplicationContext().registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.APP_ID + Constants.INTENT_IOT));


        app.setDeviceType(Constants.DEVICE_TYPE);
        app.setDeviceId("123456789");
        app.setOrganization("p0g2ka");
        app.setAuthToken("_alp0VdLDkIu?ze63I");
        IoTClient iotClient = IoTClient.getInstance(context, app.getOrganization(), app.getDeviceId(), app.getDeviceType(), app.getAuthToken());

        try {
            SocketFactory factory = null;
            // need to implement ssl here

            MyIoTActionListener listener = new MyIoTActionListener(context, Constants.ActionStateStatus.CONNECTING);
            //start connection - if this method returns, connection has not yet happened
            iotClient.connectDevice(app.getMyIoTCallbacks(), listener, factory);
            // iotClient.disconnectDevice(listener);
            Log.d(TAG, ".connectDevice finished");

        } catch (MqttException e) {
            if (e.getReasonCode() == (Constants.ERROR_BROKER_UNAVAILABLE)) {
                // error while connecting to the broker - send an intent to inform the user
                Intent actionIntent = new Intent(Constants.ACTION_INTENT_CONNECTIVITY_MESSAGE_RECEIVED);
                actionIntent.putExtra(Constants.CONNECTIVITY_MESSAGE, Constants.ERROR_BROKER_UNAVAILABLE);
//                context.sendBroadcast(actionIntent);
            }
        }

        final TextView valueCO = getView().findViewById(R.id.valueCO);
        Button startButton = getView().findViewById(R.id.btnStartMessage);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData();
            }
        });
    }

    public String getMacAddress(Context context) {
        WifiManager wimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String macAddress = wimanager.getConnectionInfo().getMacAddress();
        if (macAddress == null) {
            macAddress = "Device don't have mac address or wi-fi is disabled";
        }
        return macAddress;
    }

}
/*******************************************************************************
 * Copyright (c) 2014-2015 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *******************************************************************************/
package org.pyrrha_platform.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.pyrrha_platform.PyrrhaApplication;
import org.pyrrha_platform.iot.IoTActionListener;
import org.pyrrha_platform.iot.IoTClient;

/**
 * This class implements the IMqttActionListener interface of the MQTT Client.
 * It provides the functionality for handling the success or failure of MQTT API calls.
 * One instance should only be used for one connection.
 */
public class MyIoTActionListener implements IoTActionListener {

    private final static String TAG = MyIoTActionListener.class.getName();

    private final Context context;
    private final Constants.ActionStateStatus action;
    private final PyrrhaApplication app;

    public MyIoTActionListener(Context context, Constants.ActionStateStatus action) {
        this.context = context;
        this.action = action;
        app = (PyrrhaApplication) context.getApplicationContext();
    }

    /**
     * Determine the type of callback that completed successfully.
     *
     * @param token The MQTT Token for the completed action.
     */
    @Override
    public void onSuccess(IMqttToken token) {
        Log.d(TAG, ".onSuccess() entered");
        switch (action) {
            case CONNECTING:
                handleConnectSuccess();
                break;

            case SUBSCRIBE:
                handleSubscribeSuccess();
                break;

            case PUBLISH:
                handlePublishSuccess();
                break;

            case DISCONNECTING:
                handleDisconnectSuccess();
                break;

            default:
                break;
        }
    }


    /**
     * Determine the type of callback that failed.
     *
     * @param token     The MQTT Token for the completed action.
     * @param throwable The exception corresponding to the failure.
     */
    @Override
    public void onFailure(IMqttToken token, Throwable throwable) {
        Log.e(TAG, ".onFailure() entered");
        switch (action) {
            case CONNECTING:
                handleConnectFailure(throwable);
                break;

            case SUBSCRIBE:
                handleSubscribeFailure(throwable);
                break;

            case PUBLISH:
                handlePublishFailure(throwable);
                break;

            case DISCONNECTING:
                handleDisconnectFailure(throwable);
                break;

            default:
                break;
        }
    }

    /**
     * Called on successful connection to the MQTT broker.
     */
    private void handleConnectSuccess() {
        Log.d(TAG, ".handleConnectSuccess() entered");

        app.setConnected(true);

        // create ActionListener to handle message published results
        try {
            MyIoTActionListener listener = new MyIoTActionListener(context, Constants.ActionStateStatus.PUBLISH);
            IoTClient iotClient = IoTClient.getInstance(context);
            iotClient.subscribeToCommand("+", "json", 0, "subscribe", listener);
        } catch (MqttException e) {
            Log.d(TAG, ".handleConnectSuccess() received exception on subscribeToCommand()");
        }


        String runningActivity = app.getCurrentRunningActivity();
        //if (runningActivity != null && runningActivity.equals(LoginPagerFragment.class.getName())) {
        Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
        actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_CONNECT);
        //  context.sendBroadcast(actionIntent);
        //}
    }

    /**
     * Called on successful subscription to the MQTT topic.
     */
    private void handleSubscribeSuccess() {
        Log.d(TAG, ".handleSubscribeSuccess() entered");
    }

    /**
     * Called on successful publish to the MQTT topic.
     */
    private void handlePublishSuccess() {
        Log.d(TAG, ".handlePublishSuccess() entered");
    }

    /**
     * Called on successful disconnect from the MQTT server.
     */
    private void handleDisconnectSuccess() {
        Log.d(TAG, ".handleDisconnectSuccess() entered");

        app.setConnected(false);

        //String runningActivity = app.getCurrentRunningActivity();
        //if (runningActivity != null && runningActivity.equals(LoginPagerFragment.class.getName())) {
        //    Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
        //    actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_DISCONNECT);
        //    context.sendBroadcast(actionIntent);
        //}
    }

    /**
     * Called on failure to connect to the MQTT server.
     *
     * @param throwable The exception corresponding to the failure.
     */
    private void handleConnectFailure(Throwable throwable) {
        Log.e(TAG, ".handleConnectFailure() entered");
        Log.e(TAG, ".handleConnectFailure() - Failed with exception", throwable);
        throwable.printStackTrace();

        app.setConnected(false);

        //broadcast disconnect event
        //String runningActivity = app.getCurrentRunningActivity();
        //if (runningActivity != null && runningActivity.equals(LoginPagerFragment.class.getName())) {
        Intent actionIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
        actionIntent.putExtra(Constants.INTENT_DATA, Constants.INTENT_DATA_DISCONNECT);
        // context.sendBroadcast(actionIntent);
        //}

        //also broadcast an alert event so user sees error message
        String NL = System.getProperty("line.separator");
        String errMsg = "Failed to connect to Watson IoT: " + NL + throwable;
        Intent alertIntent = new Intent(Constants.APP_ID + Constants.INTENT_LOGIN);
        alertIntent.putExtra(Constants.INTENT_DATA, Constants.ALERT_EVENT);
        alertIntent.putExtra(Constants.INTENT_DATA_MESSAGE, errMsg);
        // context.sendBroadcast(alertIntent);

        Log.e(TAG, ".handleConnectFailure() exit");
    }

    /**
     * Called on failure to subscribe to the MQTT topic.
     *
     * @param throwable The exception corresponding to the failure.
     */
    private void handleSubscribeFailure(Throwable throwable) {
        Log.e(TAG, ".handleSubscribeFailure() entered");
        Log.e(TAG, ".handleSubscribeFailure() - Failed with exception", throwable.getCause());
    }

    /**
     * Called on failure to publish to the MQTT topic.
     *
     * @param throwable The exception corresponding to the failure.
     */
    private void handlePublishFailure(Throwable throwable) {
        Log.e(TAG, ".handlePublishFailure() entered");
        Log.e(TAG, ".handlePublishFailure() - Failed with exception", throwable.getCause());
    }

    /**
     * Called on failure to disconnect from the MQTT server.
     *
     * @param throwable The exception corresponding to the failure.
     */
    private void handleDisconnectFailure(Throwable throwable) {
        Log.e(TAG, ".handleDisconnectFailure() entered");
        Log.e(TAG, ".handleDisconnectFailure() - Failed with exception", throwable.getCause());
    }

}

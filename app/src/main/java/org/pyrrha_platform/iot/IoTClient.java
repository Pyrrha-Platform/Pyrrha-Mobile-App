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
package org.pyrrha_platform.iot;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.SocketFactory;

/**
 * IoTClient provides a wrapper around the Eclipse Paho Project Android MQTT Service.
 * <p>
 * Created by mprobert on 3/12/2015.
 */
public class IoTClient {
    private static final String TAG = IoTClient.class.getName();
    private static final String IOT_ORGANIZATION_TCP = ".messaging.internetofthings.ibmcloud.com:1883";
    private static final String IOT_ORGANIZATION_SSL = ".messaging.internetofthings.ibmcloud.com:8883";
    private static final String IOT_DEVICE_USERNAME = "use-token-auth";
    
    // Local MQTT broker configuration
    private static final String LOCAL_MQTT_HOST = "10.0.2.2";
    private static final String LOCAL_MQTT_PORT = "1883";
    private static IoTClient instance;
    private final Context context;
    private MqttAsyncClient client;
    private String organization;
    private String deviceType;
    private String deviceID;
    private String authorizationToken;

    private IoTClient(Context context) {
        this.context = context;
        this.client = null;
    }

    private IoTClient(Context context, String organization, String deviceID, String deviceType, String authorizationToken) {
        this.context = context;
        this.client = null;
        this.organization = organization;
        this.deviceID = deviceID;
        this.deviceType = deviceType;
        this.authorizationToken = authorizationToken;
    }

    /**
     * @param context The application context for the object
     * @return The IoTClient object for the application
     */
    public static IoTClient getInstance(Context context) {
        Log.d(TAG, ".getInstance() entered");
        if (instance == null) {
            instance = new IoTClient(context);
        }
        return instance;
    }

    /**
     * @param context            The application context for the object
     * @param organization       The organization id the device is registered to
     * @param deviceID           The device ID used to identify the device
     * @param deviceType         The type of the device as registered in IoT
     * @param authorizationToken The authorization token for the device
     * @return The IoTClient object for the application
     */
    public static IoTClient getInstance(Context context, String organization, String deviceID, String deviceType, String authorizationToken) {
        Log.d(TAG, ".getInstance() entered");
        if (instance == null) {
            instance = new IoTClient(context, organization, deviceID, deviceType, authorizationToken);
        } else {
            instance.setAuthorizationToken(authorizationToken);
            instance.setOrganization(organization);
            instance.setDeviceID(deviceID);
            instance.setDeviceType(deviceType);
        }
        return instance;
    }

    /**
     * @param event  The event to create a topic string for
     * @param format The format of the data sent to this topic
     * @return The event topic for the specified event string
     */
    public static String getEventTopic(String event, String format) {
        return "iot-2/evt/" + event + "/fmt/json";
    }

    /**
     * @param command The command to create a topic string for
     * @param format  The format of the data sent to this topic
     * @return The command topic for the specified command string
     */
    public static String getCommandTopic(String command, String format) {
        return "iot-2/cmd/" + command + "/fmt/json";
    }

    /**
     * Connect to the Watson Internet of Things Platform
     *
     * @param callbacks The IoTCallbacks object to register with the Mqtt Client
     * @param listener  The IoTActionListener object to register with the Mqtt Token.
     * @return IMqttToken The token returned by the Mqtt Connect call
     * @throws MqttException
     */
    public IMqttToken connectDevice(IoTCallbacks callbacks, IoTActionListener listener, SocketFactory factory) throws MqttException {
        Log.d(TAG, ".connectDevice() entered");
        
        // Use local MQTT broker configuration instead of IBM Watson IoT
        String clientID;
        String connectionURI;
        String username;
        String password;
        
        // Check if we should use local MQTT broker (based on properties configuration)
        if (this.getOrganization() != null && this.getOrganization().equals("local")) {
            // Local MQTT broker configuration
            // Map any device ID to one of the 4 available Prometeo devices (01-04)
            String deviceNumber = "1"; // Default to device 1 for now
            if (this.getDeviceID() != null && this.getDeviceID().length() > 0) {
                // Simple hash-based mapping to distribute devices across 01-04
                int hash = Math.abs(this.getDeviceID().hashCode()) % 4;
                deviceNumber = String.valueOf(hash + 1);
            }
            clientID = "Prometeo:00:00:00:00:00:0" + deviceNumber;
            connectionURI = "tcp://" + LOCAL_MQTT_HOST + ":" + LOCAL_MQTT_PORT;
            username = clientID; // VerneMQ uses client ID as username
            password = "password"; // Standard password for local development
            Log.d(TAG, "Using local MQTT broker: " + connectionURI + " with device: " + clientID);
        } else {
            // Original IBM Watson IoT configuration for backward compatibility
            clientID = "d:" + this.getOrganization() + ":" + this.getDeviceType() + ":" + this.getDeviceID();
            if (factory == null || this.getOrganization().equals("quickstart")) {
                connectionURI = "tcp://" + this.getOrganization() + IOT_ORGANIZATION_TCP;
            } else {
                connectionURI = "ssl://" + this.getOrganization() + IOT_ORGANIZATION_SSL;
            }
            username = IOT_DEVICE_USERNAME;
            password = this.getAuthorizationToken();
            Log.d(TAG, "Using IBM Watson IoT: " + connectionURI);
        }

        if (!isMqttConnected()) {
            if (client != null && client.isConnected()) {
                try {
                    client.disconnect();
                } catch (MqttException e) {
                    Log.w(TAG, "Error disconnecting existing client", e);
                }
            }
            
            try {
                // Use pure Java MQTT client with memory persistence
                MemoryPersistence persistence = new MemoryPersistence();
                client = new MqttAsyncClient(connectionURI, clientID, persistence);
                client.setCallback(callbacks);

                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setUserName(username);
                options.setPassword(password.toCharArray());
                options.setConnectionTimeout(30);
                options.setKeepAliveInterval(60);
                options.setAutomaticReconnect(true);

                if (factory != null && !this.getOrganization().equals("quickstart")) {
                    options.setSocketFactory(factory);
                }

                Log.d(TAG, "Connecting to server: " + connectionURI + " with clientID: " + clientID);
                
                // Connect with pure Java client
                return client.connect(options, null, listener);
            } catch (MqttException e) {
                Log.e(TAG, "Exception caught while attempting to connect to server", e);
                throw e;
            }
        }
        return null;
    }
    /**
     * Disconnect the device from the Watson Internet of Things Platform
     *
     * @param listener The IoTActionListener object to register with the Mqtt Token.
     * @return IMqttToken The token returned by the Mqtt Disconnect call
     * @throws MqttException
     */
    public IMqttToken disconnectDevice(IoTActionListener listener) throws MqttException {
        Log.d(TAG, ".disconnectDevice() entered");
        if (isMqttConnected()) {
            try {
                return client.disconnect(null, listener);
            } catch (MqttException e) {
                Log.e(TAG, "Exception caught while attempting to disconnect from server", e);
                throw e;
            }
        }
        return null;
    }

    /**
     * Subscribe to a device event
     *
     * @param event       The IoT event to subscribe to
     * @param format      The format of data sent to the event topic
     * @param qos         The Quality of Service to use for the subscription
     * @param userContext The context to associate with the subscribe call
     * @param listener    The IoTActionListener object to register with the Mqtt Token
     * @throws MqttException
     * @@return IMqttToken The token returned by the Mqtt Subscribe call
     */
    public IMqttToken subscribeToEvent(String event, String format, int qos, Object userContext, IMqttActionListener listener) throws MqttException {
        Log.d(TAG, ".subscribeToEvent() entered");
        String eventTopic = getEventTopic(event, format);
        return subscribe(eventTopic, qos, userContext, listener);
    }

    /**
     * Unsubscribe from a device event
     *
     * @param event       The IoT event to unsubscribe from
     * @param format      The format of data sent to the event topic
     * @param userContext The context to associate with the unsubscribe call
     * @param listener    The IoTActionListener object to register with the Mqtt Token
     * @throws MqttException
     * @@return IMqttToken The token returned by the Mqtt Unsubscribe call
     */
    public IMqttToken unsubscribeFromEvent(String event, String format, Object userContext, IMqttActionListener listener) throws MqttException {
        Log.d(TAG, ".unsubscribeFromEvent() entered");
        String eventTopic = getEventTopic(event, format);
        return unsubscribe(eventTopic, userContext, listener);
    }

    /**
     * Subscribe to a device  command
     *
     * @param command     The IoT command to subscribe to
     * @param format      The format of data sent to the event topic
     * @param qos         The Quality of Service to use for the subscription
     * @param userContext The context to associate with the subscribe call
     * @param listener    The IoTActionListener object to register with the Mqtt Token
     * @throws MqttException
     * @@return IMqttToken The token returned by the Mqtt Subscribe call
     */
    public IMqttToken subscribeToCommand(String command, String format, int qos, Object userContext, IMqttActionListener listener) throws MqttException {
        Log.d(TAG, "subscribeToCommand() entered");
        String commandTopic = getCommandTopic(command, format);
        return subscribe(commandTopic, qos, userContext, listener);
    }

    /**
     * Unsubscribe from a device command
     *
     * @param command     The IoT command to unsubscribe from
     * @param format      The format of data sent to the event topic
     * @param userContext The context to associate with the unsubscribe call
     * @param listener    The IoTActionListener object to register with the Mqtt Token
     * @throws MqttException
     * @@return IMqttToken The token returned by the Mqtt Unsubscribe call
     */
    public IMqttToken unsubscribeFromCommand(String command, String format, Object userContext, IMqttActionListener listener) throws MqttException {
        Log.d(TAG, ".unsubscribeFromCommand() entered");
        String commandTopic = getCommandTopic(command, format);
        return unsubscribe(commandTopic, userContext, listener);
    }

    // PRIVATE FUNCTIONS

    /**
     * Publish a device event message
     *
     * @param event    The IoT event string to publish the message to
     * @param format   The format of data sent to the event topic
     * @param payload  The payload to be sent
     * @param qos      The Quality of Service to use when publishing the message
     * @param retained The flag to specify whether the message should be retained
     * @param listener The IoTActionListener object to register with the Mqtt Token
     * @throws MqttException
     * @@return IMqttDeliveryToken The token returned by the Mqtt Publish call
     */
    public IMqttDeliveryToken publishEvent(String event, String format, String payload, int qos, boolean retained, IoTActionListener listener) throws MqttException {
        Log.d(TAG, ".publishEvent() entered");
        String eventTopic = getEventTopic(event, format);
        return publish(eventTopic, payload, qos, retained, listener);
    }

    /**
     * Publish a device command message
     *
     * @param command  The IoT command to publish the message to
     * @param format   The format of data sent to the command topic
     * @param payload  The payload to be sent
     * @param qos      The Quality of Service to use when publishing the message
     * @param retained The flag to specify whether the message should be retained
     * @param listener The IoTActionListener object to register with the Mqtt Token
     * @throws MqttException
     * @@return IMqttDeliveryToken The token returned by the Mqtt Publish call
     */
    public IMqttDeliveryToken publishCommand(String command, String format, String payload, int qos, boolean retained, IoTActionListener listener) throws MqttException {
        Log.d(TAG, ".publishCommand() entered");
        String commandTopic = getCommandTopic(command, format);
        return publish(commandTopic, payload, qos, retained, listener);
    }

    /**
     * Subscribe to an MQTT topic
     *
     * @param topic       The MQTT topic string to subscribe to
     * @param qos         The Quality of Service to use for the subscription
     * @param userContext The context to associate with the subscribe call
     * @param listener    The IoTActionListener object to register with the Mqtt Token
     * @throws MqttException
     * @@return IMqttToken The token returned by the Mqtt Subscribe call
     */
    private IMqttToken subscribe(String topic, int qos, Object userContext, IMqttActionListener listener) throws MqttException {
        Log.d(TAG, ".subscribe() entered");
        if (isMqttConnected()) {
            try {
                return client.subscribe(topic, qos, userContext, listener);
            } catch (MqttException e) {
                Log.e(TAG, "Exception caught while attempting to subscribe to topic " + topic, e.getCause());
                throw e;
            }
        }
        return null;
    }

    /**
     * Unsubscribe from an MQTT topic
     *
     * @param topic       The MQTT topic string to unsubscribe from
     * @param userContext The context to associate with the unsubscribe call
     * @param listener    The IoTActionListener object to register with the Mqtt Token
     * @throws MqttException
     * @@return IMqttToken The token returned by the Mqtt Unsubscribe call
     */
    private IMqttToken unsubscribe(String topic, Object userContext, IMqttActionListener listener) throws MqttException {
        Log.d(TAG, ".unsubscribe() entered");
        if (isMqttConnected()) {
            try {
                return client.unsubscribe(topic, userContext, listener);
            } catch (MqttException e) {
                Log.e(TAG, "Exception caught while attempting to subscribe to topic " + topic, e.getCause());
                throw e;
            }
        }
        return null;
    }

    /**
     * Publish to an MQTT topic
     *
     * @param topic    The MQTT topic string to publish the message to
     * @param payload  The payload to be sent
     * @param qos      The Quality of Service to use when publishing the message
     * @param retained The flag to specify whether the message should be retained
     * @param listener The IoTActionListener object to register with the Mqtt Token
     * @return IMqttDeliveryToken The token returned by the Mqtt Publish call
     * @throws MqttException
     */
    private IMqttDeliveryToken publish(String topic, String payload, int qos, boolean retained, IoTActionListener listener) throws MqttException {
        Log.d(TAG, ".publish() entered");

        // check if client is connected
        if (isMqttConnected()) {
            // create a new MqttMessage from the message string
            MqttMessage mqttMsg = new MqttMessage(payload.getBytes());
            // set retained flag
            mqttMsg.setRetained(retained);
            // set quality of service
            mqttMsg.setQos(qos);
            try {
                // create ActionListener to handle message published results
                Log.d(TAG, ".publish() - Publishing " + payload + " to: " + topic + ", with QoS: " + qos + " with retained flag set to " + retained);
                return client.publish(topic, mqttMsg, null, listener);
            } catch (MqttPersistenceException e) {
                Log.e(TAG, "MqttPersistenceException caught while attempting to publish a message", e.getCause());
                throw e;
            } catch (MqttException e) {
                Log.e(TAG, "MqttException caught while attempting to publish a message", e.getCause());
                throw e;
            }
        }
        return null;
    }

    /**
     * Checks if the MQTT client has an active connection
     *
     * @return True if client is connected, false if not
     */
    public boolean isMqttConnected() {
        Log.d(TAG, ".isMqttConnected() entered");
        boolean connected = false;
        try {
            if ((client != null) && (client.isConnected())) {
                connected = true;

            }
        } catch (Exception e) {
            // swallowing the exception as it means the client is not connected
        }

        if (client == null) {
            Log.d(TAG, "Client is null" + connected);

        }

        Log.d(TAG, ".isMqttConnected() - returning " + connected);
        return connected;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthorizationToken(String authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}

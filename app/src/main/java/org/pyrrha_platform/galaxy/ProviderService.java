/*
 * Copyright (c) 2016 Samsung Electronics Co., Ltd. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that
 * the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation and/or
 *       other materials provided with the distribution.
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.pyrrha_platform.galaxy;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAMessage;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import org.json.JSONException;
import org.json.JSONObject;
import org.pyrrha_platform.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Samsung Accessory Protocol Provider Service for Pyrrha Platform
 * 
 * This service acts as a data provider, sending sensor readings from the mobile app
 * to connected Galaxy Watch consumers. It receives sensor data from Prometeo devices
 * via BLE and forwards it to the watch for real-time monitoring.
 */
public class ProviderService extends SAAgent {
    private static final String TAG = "PyrrhaMobileProvider";
    private static final int CHANNEL_ID = 104;
    
    private final IBinder mBinder = new LocalBinder();
    private final Handler mHandler = new Handler();
    private ScheduledExecutorService mSensorBroadcastExecutor;
    
    private SAMessage mMessage = null;
    private List<SAPeerAgent> mConnectedPeers = new ArrayList<>();
    private Toast mToast;
    
    // Sensor data storage
    private volatile SensorData mLatestSensorData = new SensorData();
    private boolean mIsConnectedToWatch = false;
    
    /**
     * Container for sensor readings from Prometeo devices
     */
    public static class SensorData {
        public float temperature = 0.0f;
        public float humidity = 0.0f;
        public float co = 0.0f;
        public float no2 = 0.0f;
        public long timestamp = System.currentTimeMillis();
        public String deviceId = "unknown";
        public String status = "normal";
        
        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("temperature", temperature);
            json.put("humidity", humidity);
            json.put("co", co);
            json.put("no2", no2);
            json.put("timestamp", timestamp);
            json.put("deviceId", deviceId);
            json.put("status", status);
            json.put("messageType", "sensor_data");
            return json;
        }
    }

    public ProviderService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ProviderService onCreate()");
        
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            if (processUnsupportedException(e)) {
                return;
            }
        } catch (Exception e1) {
            Log.e(TAG, "Samsung Accessory SDK initialization failed", e1);
            stopSelf();
        }

        setupMessageHandler();
        startSensorBroadcasting();
    }
    
    private void setupMessageHandler() {
        mMessage = new SAMessage(this) {

            @Override
            protected void onSent(SAPeerAgent peerAgent, int id) {
                Log.d(TAG, "Message sent successfully to " + peerAgent.getPeerId() + ", id: " + id);
            }

            @Override
            protected void onError(SAPeerAgent peerAgent, int id, int errorCode) {
                Log.e(TAG, "Message send error to " + peerAgent.getPeerId() + 
                      ", id: " + id + ", errorCode: " + errorCode);
                
                String errorMessage = getErrorMessage(errorCode);
                displayToast("Send failed: " + errorMessage, Toast.LENGTH_SHORT);
            }

            @Override
            protected void onReceive(SAPeerAgent peerAgent, byte[] message) {
                String receivedData = new String(message);
                Log.d(TAG, "Received from watch: " + receivedData);
                
                try {
                    JSONObject json = new JSONObject(receivedData);
                    String messageType = json.optString("messageType", "unknown");
                    
                    switch (messageType) {
                        case "sensor_request":
                            Log.d(TAG, "Watch requested sensor data");
                            sendCurrentSensorData(peerAgent);
                            break;
                        case "heartbeat":
                            Log.d(TAG, "Received heartbeat from watch");
                            sendHeartbeatResponse(peerAgent);
                            break;
                        default:
                            Log.d(TAG, "Unknown message type: " + messageType);
                    }
                } catch (JSONException e) {
                    Log.w(TAG, "Failed to parse received message as JSON: " + receivedData);
                }
            }
        };
    }
    
    private void startSensorBroadcasting() {
        mSensorBroadcastExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Send sensor data every 3 seconds to connected watches
        mSensorBroadcastExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!mConnectedPeers.isEmpty()) {
                    broadcastSensorData();
                }
            }
        }, 1, 3, TimeUnit.SECONDS);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ProviderService onDestroy()");
        
        if (mSensorBroadcastExecutor != null) {
            mSensorBroadcastExecutor.shutdown();
        }
        
        mConnectedPeers.clear();
        mIsConnectedToWatch = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        Log.d(TAG, "onFindPeerAgentsResponse: result=" + result + 
              ", agents=" + (peerAgents != null ? peerAgents.length : 0));
        
        if ((result == SAAgent.PEER_AGENT_FOUND) && (peerAgents != null)) {
            for (SAPeerAgent peerAgent : peerAgents) {
                Log.d(TAG, "Found peer agent: " + peerAgent.getPeerId());
                if (!mConnectedPeers.contains(peerAgent)) {
                    mConnectedPeers.add(peerAgent);
                    mIsConnectedToWatch = true;
                    displayToast("Connected to Galaxy Watch", Toast.LENGTH_SHORT);
                    
                    // Send initial sensor data
                    sendCurrentSensorData(peerAgent);
                }
            }
        } else {
            String message = getResultMessage(result);
            Log.w(TAG, "Find peer agents failed: " + message);
            displayToast(message, Toast.LENGTH_SHORT);
        }
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        Log.e(TAG, "onError: " + errorMessage + ", code: " + errorCode);
        super.onError(peerAgent, errorMessage, errorCode);
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        Log.d(TAG, "onPeerAgentsUpdated: result=" + result);
        
        if (peerAgents != null) {
            if (result == SAAgent.PEER_AGENT_AVAILABLE) {
                Log.d(TAG, "Peer agent became available");
                displayToast("Galaxy Watch connected", Toast.LENGTH_SHORT);
                mIsConnectedToWatch = true;
            } else if (result == SAAgent.PEER_AGENT_UNAVAILABLE) {
                Log.d(TAG, "Peer agent became unavailable");
                displayToast("Galaxy Watch disconnected", Toast.LENGTH_SHORT);
                mConnectedPeers.clear();
                mIsConnectedToWatch = false;
            }
        }
    }
    
    /**
     * Update sensor data from BLE device readings
     */
    public void updateSensorData(float temperature, float humidity, float co, float no2, String deviceId) {
        mLatestSensorData.temperature = temperature;
        mLatestSensorData.humidity = humidity;
        mLatestSensorData.co = co;
        mLatestSensorData.no2 = no2;
        mLatestSensorData.deviceId = deviceId;
        mLatestSensorData.timestamp = System.currentTimeMillis();
        
        // Determine status based on thresholds
        mLatestSensorData.status = calculateStatus(temperature, humidity, co, no2);
        
        Log.d(TAG, "Updated sensor data: T=" + temperature + "°C, H=" + humidity + 
              "%, CO=" + co + "ppm, NO2=" + no2 + "ppm, Status=" + mLatestSensorData.status);
    }
    
    private String calculateStatus(float temperature, float humidity, float co, float no2) {
        // Thresholds from watch constants
        final float TMP_RED = 32.0f;
        final float HUM_RED = 80.0f;
        final float CO_RED = 420.0f;
        final float NO2_RED = 8.0f;
        
        if (temperature > TMP_RED || humidity > HUM_RED || co > CO_RED || no2 > NO2_RED) {
            return "alert";
        } else if (temperature > TMP_RED * 0.8f || humidity > HUM_RED * 0.8f || 
                   co > CO_RED * 0.8f || no2 > NO2_RED * 0.8f) {
            return "warning";
        }
        return "normal";
    }
    
    private void broadcastSensorData() {
        if (mMessage == null || mConnectedPeers.isEmpty()) {
            return;
        }
        
        for (SAPeerAgent peerAgent : mConnectedPeers) {
            sendCurrentSensorData(peerAgent);
        }
    }
    
    private void sendCurrentSensorData(SAPeerAgent peerAgent) {
        try {
            JSONObject sensorJson = mLatestSensorData.toJSON();
            String jsonString = sensorJson.toString();
            
            mMessage.send(peerAgent, jsonString.getBytes());
            Log.d(TAG, "Sent sensor data to watch: " + jsonString);
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create sensor data JSON", e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to send sensor data to watch", e);
            mConnectedPeers.remove(peerAgent);
            mIsConnectedToWatch = !mConnectedPeers.isEmpty();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid argument when sending sensor data", e);
        }
    }
    
    private void sendHeartbeatResponse(SAPeerAgent peerAgent) {
        try {
            JSONObject heartbeat = new JSONObject();
            heartbeat.put("messageType", "heartbeat");
            heartbeat.put("timestamp", System.currentTimeMillis());
            heartbeat.put("status", "alive");
            
            mMessage.send(peerAgent, heartbeat.toString().getBytes());
            Log.d(TAG, "Sent heartbeat response to watch");
            
        } catch (JSONException | IOException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to send heartbeat response", e);
        }
    }
    
    public void findWatches() {
        Log.d(TAG, "Searching for Galaxy Watches...");
        findPeerAgents();
    }
    
    public boolean isConnectedToWatch() {
        return mIsConnectedToWatch;
    }
    
    public int getConnectedWatchCount() {
        return mConnectedPeers.size();
    }
    
    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        int errType = e.getType();
        
        if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED
                || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
            Log.e(TAG, "Samsung Accessory SDK not supported on this device");
            stopSelf();
        } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
            Log.e(TAG, "Samsung Accessory SDK not installed");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {
            Log.e(TAG, "Samsung Accessory SDK update required");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
            Log.e(TAG, "Samsung Accessory SDK update recommended");
            return false;
        }
        return true;
    }
    
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SAMessage.ERROR_PEER_AGENT_UNREACHABLE:
                return "Watch unreachable";
            case SAMessage.ERROR_PEER_AGENT_NO_RESPONSE:
                return "Watch not responding";
            case SAMessage.ERROR_PEER_AGENT_NOT_SUPPORTED:
                return "Watch not supported";
            case SAMessage.ERROR_PEER_SERVICE_NOT_SUPPORTED:
                return "Service not supported";
            case SAMessage.ERROR_SERVICE_NOT_SUPPORTED:
                return "Protocol not supported";
            default:
                return "Unknown error (" + errorCode + ")";
        }
    }
    
    private String getResultMessage(int result) {
        switch (result) {
            case SAAgent.FINDPEER_DEVICE_NOT_CONNECTED:
                return "Galaxy Watch not connected";
            case SAAgent.FINDPEER_SERVICE_NOT_FOUND:
                return "Pyrrha Watch App not found";
            default:
                return "No compatible watches found";
        }
    }
    
    public void clearToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    private void displayToast(String str, int duration) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), str, duration);
        mToast.show();
    }

    public class LocalBinder extends Binder {
        public ProviderService getService() {
            return ProviderService.this;
        }
    }
}
package org.pyrrha_platform.simulation;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.pyrrha_platform.DeviceDashboard;
import org.pyrrha_platform.PyrrhaApplication;
import org.pyrrha_platform.ble.BluetoothLeService;
import org.pyrrha_platform.iot.IoTClient;
import org.pyrrha_platform.utils.Constants;
import org.pyrrha_platform.utils.MessageFactory;
import org.pyrrha_platform.utils.MyIoTActionListener;
import org.pyrrha_platform.utils.PyrrhaEvent;
import org.pyrrha_platform.galaxy.ProviderService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

/**
 * Simulates Prometeo device sensor readings and publishes them to MQTT
 * This class generates realistic sensor data every 2 seconds:
 * - Temperature: 20-40°C (realistic firefighter environment)
 * - Humidity: 30-90% (varying conditions)
 * - CO: 0-50 ppm (normal to elevated levels)
 * - NO2: 0-2 ppm (normal to slightly elevated)
 */
public class SensorDataSimulator {
    private static final String TAG = SensorDataSimulator.class.getSimpleName();
    private static final int UPDATE_INTERVAL_MS = 2000; // 2 seconds
    
    private final Context context;
    private final String deviceName;
    private final String firefighterId;
    private final Handler handler;
    private final Random random;
    private final SimpleDateFormat dateFormatter;
    
    private boolean isRunning = false;
    private Runnable simulationRunnable;
    
    // Sensor reading ranges for realistic Prometeo device simulation
    private static final float TEMP_MIN = 20.0f;
    private static final float TEMP_MAX = 40.0f;
    private static final float HUMIDITY_MIN = 30.0f;
    private static final float HUMIDITY_MAX = 90.0f;
    private static final float CO_MIN = 0.0f;
    private static final float CO_MAX = 50.0f;
    private static final float NO2_MIN = 0.0f;
    private static final float NO2_MAX = 2.0f;
    
    // Callbacks for UI updates
    public interface SensorDataCallback {
        void onSensorDataReceived(String sensorData);
        void onMqttDataSent(PyrrhaEvent event);
        void onGalaxyWatchDataSent(float temperature, float humidity, float co, float no2);
    }
    
    private SensorDataCallback callback;
    
    public SensorDataSimulator(Context context, String deviceName, String firefighterId) {
        this.context = context;
        this.deviceName = deviceName;
        this.firefighterId = firefighterId;
        this.handler = new Handler(Looper.getMainLooper());
        this.random = new Random();
        
        // Initialize date formatter for UTC timestamps
        this.dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        this.dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    public void setCallback(SensorDataCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Start generating sensor data every 2 seconds
     */
    public void startSimulation() {
        if (isRunning) {
            Log.w(TAG, "Simulation already running");
            return;
        }
        
        isRunning = true;
        Log.i(TAG, "Starting sensor simulation for device: " + deviceName);
        
        simulationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    generateAndPublishSensorData();
                    handler.postDelayed(this, UPDATE_INTERVAL_MS);
                }
            }
        };
        
        handler.post(simulationRunnable);
    }
    
    /**
     * Stop the sensor data simulation
     */
    public void stopSimulation() {
        isRunning = false;
        if (simulationRunnable != null) {
            handler.removeCallbacks(simulationRunnable);
            simulationRunnable = null;
        }
        Log.i(TAG, "Stopped sensor simulation for device: " + deviceName);
    }
    
    /**
     * Generate realistic sensor readings and publish to all endpoints
     */
    private void generateAndPublishSensorData() {
        try {
            // Generate realistic sensor readings
            float temperature = generateReading(TEMP_MIN, TEMP_MAX, 1); // 1 decimal place
            float humidity = generateReading(HUMIDITY_MIN, HUMIDITY_MAX, 1);
            float co = generateReading(CO_MIN, CO_MAX, 2); // 2 decimal places for gas concentration
            float no2 = generateReading(NO2_MIN, NO2_MAX, 2);
            
            // Create formatted sensor data string matching Prometeo device format
            String sensorData = String.format("TEMP %.1f STDEV 0.1 HUM %.1f STDEV 0.1 CO %.2f STDEV 0.01 NO2 %.2f STDEV 0.01",
                    temperature, humidity, co, no2);
            
            Log.d(TAG, "Generated sensor data: " + sensorData);
            
            // 1. Update UI through callback (simulates Bluetooth data reception)
            if (callback != null) {
                callback.onSensorDataReceived(sensorData);
            }
            
            // 2. Send data directly to MQTT (same as DeviceDashboard does)
            publishToMqtt(temperature, humidity, co, no2);
            
            // 3. Send data to Galaxy Watch
            sendToGalaxyWatch(temperature, humidity, co, no2);
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating sensor data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a random reading within specified range with given precision
     */
    private float generateReading(float min, float max, int decimalPlaces) {
        float value = min + random.nextFloat() * (max - min);
        float multiplier = (float) Math.pow(10, decimalPlaces);
        return Math.round(value * multiplier) / multiplier;
    }
    
    /**
     * Publish sensor data to MQTT using the same logic as DeviceDashboard
     */
    private void publishToMqtt(float temperature, float humidity, float co, float no2) {
        try {
            PyrrhaApplication app = (PyrrhaApplication) context.getApplicationContext();
            IoTClient iotClient = IoTClient.getInstance(context);
            
            Date timestamp = new Date();
            
            // Create PyrrhaEvent with sensor data
            PyrrhaEvent event = new PyrrhaEvent();
            event.setFirefighter_id(firefighterId);
            event.setDevice_id(deviceName);
            event.setDevice_battery_level("85"); // Simulated battery level
            event.setTemperature(temperature);
            event.setHumidity(humidity);
            event.setCarbon_monoxide(co);
            event.setNitrogen_dioxide(no2);
            event.setFormaldehyde(0.0f);
            event.setAcrolein(0.0f);
            event.setBenzene(0.0f);
            event.setDevice_timestamp(dateFormatter.format(timestamp));
            
            // Create message and publish
            String messageData = MessageFactory.getPyrrhaDeviceMessage(event);
            Log.d(TAG, "Publishing to MQTT: " + messageData);
            
            MyIoTActionListener listener = new MyIoTActionListener(context, Constants.ActionStateStatus.PUBLISH);
            iotClient.publishEvent(Constants.TEXT_EVENT, "json", messageData, 0, false, listener);
            
            // Update publish count
            int count = app.getPublishCount();
            app.setPublishCount(++count);
            
            // Notify callback
            if (callback != null) {
                callback.onMqttDataSent(event);
            }
            
            Log.d(TAG, "Successfully published sensor data to MQTT");
            
        } catch (MqttException e) {
            Log.e(TAG, "Failed to publish to MQTT: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error publishing to MQTT: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send sensor data to Galaxy Watch via ProviderService
     */
    private void sendToGalaxyWatch(float temperature, float humidity, float co, float no2) {
        try {
            // This would typically be called through the DeviceDashboard's ProviderService
            // For simulation, we'll just log and notify callback
            Log.d(TAG, "Sending to Galaxy Watch: T=" + temperature + "°C, H=" + humidity + "%, CO=" + co + "ppm, NO2=" + no2 + "ppm");
            
            if (callback != null) {
                callback.onGalaxyWatchDataSent(temperature, humidity, co, no2);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending to Galaxy Watch: " + e.getMessage(), e);
        }
    }
    
    /**
     * Simulate a Bluetooth data broadcast intent (for integration with existing code)
     */
    private void sendBluetoothSimulationIntent(String sensorData) {
        Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intent.putExtra(BluetoothLeService.EXTRA_DATA, sensorData);
        context.sendBroadcast(intent);
        Log.d(TAG, "Sent Bluetooth simulation broadcast: " + sensorData);
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public String getFirefighterId() {
        return firefighterId;
    }
}
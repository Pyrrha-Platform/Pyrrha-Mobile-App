package org.pyrrha_platform.simulation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Random;

/**
 * Simulates Bluetooth data reception from a Prometeo device
 * Generates realistic sensor readings with proper timing intervals
 */
public class BluetoothDataSimulator {
    private static final String TAG = "BluetoothDataSimulator";
    
    // Simulation parameters
    private static final int SIMULATION_INTERVAL_MS = 2000; // 2 seconds
    
    // Realistic sensor ranges for Prometeo device
    private static final float TEMP_MIN = 20.0f;
    private static final float TEMP_MAX = 40.0f;
    private static final float HUMIDITY_MIN = 30.0f;
    private static final float HUMIDITY_MAX = 90.0f;
    private static final float CO_MIN = 0.0f;
    private static final float CO_MAX = 50.0f;
    private static final float NO2_MIN = 0.0f;
    private static final float NO2_MAX = 2.0f;
    
    private Handler mHandler;
    private Runnable mSimulationRunnable;
    private Random mRandom;
    private boolean mIsRunning = false;
    private Context mContext;
    private DataCallback mDataCallback;
    
    // Current sensor values (for gradual changes)
    private float mCurrentTemp = 25.0f;
    private float mCurrentHumidity = 50.0f;
    private float mCurrentCO = 5.0f;
    private float mCurrentNO2 = 0.5f;
    
    public interface DataCallback {
        void onDataReceived(String simulatedBluetoothData);
    }
    
    public BluetoothDataSimulator(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
        mRandom = new Random();
    }
    
    public void setDataCallback(DataCallback callback) {
        mDataCallback = callback;
    }
    
    public void startSimulation() {
        if (mIsRunning) {
            Log.w(TAG, "Simulation already running");
            return;
        }
        
        mIsRunning = true;
        Log.i(TAG, "Starting Bluetooth data simulation - generating readings every " + 
              SIMULATION_INTERVAL_MS/1000 + " seconds");
        
        mSimulationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mIsRunning) return;
                
                // Generate realistic sensor data
                String simulatedData = generateSensorData();
                
                // Send data to callback
                if (mDataCallback != null) {
                    mDataCallback.onDataReceived(simulatedData);
                }
                
                // Schedule next reading
                mHandler.postDelayed(this, SIMULATION_INTERVAL_MS);
            }
        };
        
        // Start immediately
        mHandler.post(mSimulationRunnable);
    }
    
    public void stopSimulation() {
        if (!mIsRunning) {
            Log.w(TAG, "Simulation not running");
            return;
        }
        
        mIsRunning = false;
        Log.i(TAG, "Stopping Bluetooth data simulation");
        
        if (mHandler != null && mSimulationRunnable != null) {
            mHandler.removeCallbacks(mSimulationRunnable);
        }
    }
    
    public boolean isRunning() {
        return mIsRunning;
    }
    
    /**
     * Generates realistic sensor data in the format expected by the DeviceDashboard
     * Format: "timestamp tempValue tempStdDev humValue humStdDev coValue coStdDev no2Value no2StdDev"
     */
    private String generateSensorData() {
        // Generate gradual changes to make data more realistic
        mCurrentTemp = generateGradualChange(mCurrentTemp, TEMP_MIN, TEMP_MAX, 2.0f);
        mCurrentHumidity = generateGradualChange(mCurrentHumidity, HUMIDITY_MIN, HUMIDITY_MAX, 5.0f);
        mCurrentCO = generateGradualChange(mCurrentCO, CO_MIN, CO_MAX, 3.0f);
        mCurrentNO2 = generateGradualChange(mCurrentNO2, NO2_MIN, NO2_MAX, 0.2f);
        
        // Add some random noise
        float tempReading = mCurrentTemp + (mRandom.nextFloat() - 0.5f) * 1.0f;
        float humidityReading = mCurrentHumidity + (mRandom.nextFloat() - 0.5f) * 2.0f;
        float coReading = mCurrentCO + (mRandom.nextFloat() - 0.5f) * 1.0f;
        float no2Reading = mCurrentNO2 + (mRandom.nextFloat() - 0.5f) * 0.1f;
        
        // Ensure values stay within realistic bounds
        tempReading = Math.max(TEMP_MIN, Math.min(TEMP_MAX, tempReading));
        humidityReading = Math.max(HUMIDITY_MIN, Math.min(HUMIDITY_MAX, humidityReading));
        coReading = Math.max(CO_MIN, Math.min(CO_MAX, coReading));
        no2Reading = Math.max(NO2_MIN, Math.min(NO2_MAX, no2Reading));
        
        // Format data string (matching expected format from real Bluetooth device)
        // Format: "timestamp tempValue tempStdDev humValue humStdDev coValue coStdDev no2Value no2StdDev"
        String simulatedData = String.format("SimBT %.1f 0.1 %.1f 0.1 %.1f 0.1 %.2f 0.01",
                tempReading, humidityReading, coReading, no2Reading);
        
        Log.d(TAG, "Generated sensor data: " + simulatedData);
        return simulatedData;
    }
    
    /**
     * Generates gradual changes to sensor values for more realistic simulation
     */
    private float generateGradualChange(float currentValue, float min, float max, float maxChange) {
        // Random walk with bounds
        float change = (mRandom.nextFloat() - 0.5f) * maxChange;
        float newValue = currentValue + change;
        
        // Keep within bounds
        newValue = Math.max(min, Math.min(max, newValue));
        
        return newValue;
    }
    
    public void cleanup() {
        stopSimulation();
        mHandler = null;
        mDataCallback = null;
    }
}
package org.pyrrha_platform.simulation;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.pyrrha_platform.R;
import org.pyrrha_platform.utils.PyrrhaEvent;

/**
 * Test activity to demonstrate the SensorDataSimulator functionality
 * This simulates a Prometeo device connected via Bluetooth and shows:
 * 1. Sensor readings every 2 seconds
 * 2. Data display on mobile app
 * 3. MQTT publishing to IoT platform
 * 4. Samsung Galaxy Watch integration
 */
public class SimulationTestActivity extends AppCompatActivity implements SensorDataSimulator.SensorDataCallback {
    private static final String TAG = SimulationTestActivity.class.getSimpleName();
    
    private SensorDataSimulator simulator;
    private TextView statusText;
    private TextView sensorDataText;
    private TextView mqttStatusText;
    private TextView watchStatusText;
    private Button startButton;
    private Button stopButton;
    
    private int dataUpdateCount = 0;
    private int mqttPublishCount = 0;
    private int watchUpdateCount = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation_test);
        
        initializeViews();
        initializeSimulator();
        updateUI();
    }
    
    private void initializeViews() {
        statusText = findViewById(R.id.status_text);
        sensorDataText = findViewById(R.id.sensor_data_text);
        mqttStatusText = findViewById(R.id.mqtt_status_text);
        watchStatusText = findViewById(R.id.watch_status_text);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSimulation();
            }
        });
        
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSimulation();
            }
        });
    }
    
    private void initializeSimulator() {
        // Initialize simulator with realistic Prometeo device settings
        String deviceName = "Prometeo:00:00:00:00:00:01";
        String firefighterId = "firefighter_1";
        
        simulator = new SensorDataSimulator(this, deviceName, firefighterId);
        simulator.setCallback(this);
        
        Log.i(TAG, "Initialized SensorDataSimulator for device: " + deviceName);
    }
    
    private void startSimulation() {
        if (!simulator.isRunning()) {
            simulator.startSimulation();
            Toast.makeText(this, "Started Prometeo device simulation", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Started sensor simulation");
        } else {
            Toast.makeText(this, "Simulation already running", Toast.LENGTH_SHORT).show();
        }
        updateUI();
    }
    
    private void stopSimulation() {
        if (simulator.isRunning()) {
            simulator.stopSimulation();
            Toast.makeText(this, "Stopped Prometeo device simulation", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Stopped sensor simulation");
        } else {
            Toast.makeText(this, "Simulation not running", Toast.LENGTH_SHORT).show();
        }
        updateUI();
    }
    
    private void updateUI() {
        boolean isRunning = simulator != null && simulator.isRunning();
        
        statusText.setText(isRunning ? "Status: RUNNING" : "Status: STOPPED");
        startButton.setEnabled(!isRunning);
        stopButton.setEnabled(isRunning);
        
        if (!isRunning) {
            sensorDataText.setText("Sensor Data: Waiting...");
            mqttStatusText.setText("MQTT: Ready");
            watchStatusText.setText("Galaxy Watch: Ready");
        }
    }
    
    // SensorDataCallback implementation
    
    @Override
    public void onSensorDataReceived(String sensorData) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataUpdateCount++;
                sensorDataText.setText("Sensor Data (#" + dataUpdateCount + "):\n" + sensorData);
                Log.d(TAG, "Received sensor data: " + sensorData);
            }
        });
    }
    
    @Override
    public void onMqttDataSent(PyrrhaEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mqttPublishCount++;
                mqttStatusText.setText("MQTT: Published #" + mqttPublishCount + 
                    "\nLast: " + event.getDevice_timestamp() +
                    "\nCO: " + event.getCarbon_monoxide() + "ppm, NO2: " + event.getNitrogen_dioxide() + "ppm");
                Log.d(TAG, "Published to MQTT: " + event.getFirefighter_id());
            }
        });
    }
    
    @Override
    public void onGalaxyWatchDataSent(float temperature, float humidity, float co, float no2) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                watchUpdateCount++;
                watchStatusText.setText("Galaxy Watch: Update #" + watchUpdateCount +
                    "\nT: " + temperature + "°C, H: " + humidity + "%" +
                    "\nCO: " + co + "ppm, NO2: " + no2 + "ppm");
                Log.d(TAG, "Sent to Galaxy Watch: T=" + temperature + ", CO=" + co);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (simulator != null && simulator.isRunning()) {
            simulator.stopSimulation();
        }
    }
}
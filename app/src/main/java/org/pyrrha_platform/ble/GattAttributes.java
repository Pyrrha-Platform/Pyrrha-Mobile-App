package org.pyrrha_platform.ble;

import org.pyrrha_platform.BuildConfig;

import java.util.HashMap;


public class GattAttributes {

    public static String PYRRHA_SENSORS_MEASUREMENT = BuildConfig.PYRRHA_SENSORS_MEASUREMENT;
    private static final HashMap<String, String> attributes = new HashMap();

    static {

        // Sample Services.
        attributes.put(BuildConfig.PYRRHA_DEVICE_SERVICE, "Pyrrha Device Service");

        // Sample Characteristics.
        attributes.put(BuildConfig.PYRRHA_SENSORS_MEASUREMENT, "Pyrrha Sensors Measurement");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}

package org.pyrrha_platform.ble;

import org.pyrrha_platform.BuildConfig;

import java.util.HashMap;


public class GattAttributes {

    public static final String FLAVOR_SENSORS_MEASUREMENT = BuildConfig.FLAVOR_SENSORS_MEASUREMENT;
    private static final HashMap<String, String> attributes = new HashMap();

    static {

        // Sample Services.
        attributes.put(BuildConfig.FLAVOR_DEVICE_SERVICE, BuildConfig.FLAVOR_DEVICE_SERVICE_LABEL);

        // Sample Characteristics.
        attributes.put(BuildConfig.FLAVOR_SENSORS_MEASUREMENT, BuildConfig.FLAVOR_SENSORS_MEASUREMENT_LABEL);

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}

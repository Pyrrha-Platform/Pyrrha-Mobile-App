package org.pyrrha_platform.ble;

import java.util.HashMap;


public class GattAttributes {
    public static String PYRRHA_MEASUREMENT = "dcaaccb4-c1d1-4bc4-b406-8f6f45df0208";
    private static final HashMap<String, String> attributes = new HashMap();

    static {
        // Sample Services.
        attributes.put("2c32fd5f-5082-437e-8501-959d23d3d2fb", "Pyrrha Device Service");

        // Sample Characteristics.
        attributes.put(PYRRHA_MEASUREMENT, "Pyrrha Sensors Measurement");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}

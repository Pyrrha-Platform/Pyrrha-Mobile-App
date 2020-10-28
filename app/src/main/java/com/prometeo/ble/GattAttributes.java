package com.prometeo.ble;

import java.util.HashMap;



public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String PROMETEO_MEASUREMENT = "dcaaccb4-c1d1-4bc4-b406-8f6f45df0208";

    static {
        // Sample Services.
        attributes.put("2c32fd5f-5082-437e-8501-959d23d3d2fb", "Prometeo Device Service");
        // Sample Characteristics.
        attributes.put(PROMETEO_MEASUREMENT, "Prometeo Sensors Measurement");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}

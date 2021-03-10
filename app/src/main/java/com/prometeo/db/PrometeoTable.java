package com.prometeo.db;

import androidx.annotation.NonNull;
import androidx.room.*;

@Entity
public class PrometeoTable {

    @PrimaryKey
    @NonNull
    public String device_timestamp;

    public String firefighter_id;
    public String device_id;
    public String device_battery_level;
    public Float temperature;
    public Float humidity;
    public Float carbon_monoxide;
    public Float nitrogen_dioxide;
    public Float formaldehyde;
    public Float acrolein;
    public Float benzene;

}


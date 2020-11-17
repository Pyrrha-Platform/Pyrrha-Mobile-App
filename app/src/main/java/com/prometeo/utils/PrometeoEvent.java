package com.prometeo.utils;

public class PrometeoEvent {
    private static final String TAG = PrometeoEvent.class.getName();

    private String firefighter_id;
    private String device_id;
    private String device_battery_level;
    private Float temperature;
    private Float humidity;
    private Float carbon_monoxide;
    private Float nitrogen_dioxide;
    private Float formaldehyde;
    private Float acrolein;
    private Float benzene;
    private String device_timestamp;

    public String getFirefighter_id() {
        return this.firefighter_id;
    }

    public void setFirefighter_id(String firefighter_id)    {
        this.firefighter_id = firefighter_id;
    }

    public String getDevice_id() {
        return this.device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getDevice_battery_level() {
        return this.device_battery_level;
    }

    public void setDevice_battery_level(String device_battery_level)  {
        this.device_battery_level = device_battery_level;
    }

    public Float getTemperature() {
        return this.temperature;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    public Float getCarbon_monoxide() {
        return this.carbon_monoxide;
    }

    public void setCarbon_monoxide(Float carbon_monoxide) {
        this.carbon_monoxide = carbon_monoxide;
    }

    public Float getNitrogen_dioxide() {
        return this.nitrogen_dioxide;
    }

    public void setNitrogen_dioxide(Float nitrogen_dioxide) {
        this.nitrogen_dioxide = nitrogen_dioxide;
    }

    public Float getFormaldehyde() {
        return this.formaldehyde;
    }

    public void setFormaldehyde(Float formaldehyde) {
        this.formaldehyde = formaldehyde;
    }

    public Float getAcrolein() {
        return this.acrolein;
    }

    public void setAcrolein(Float acrolein) {
        this.acrolein = acrolein;
    }

    public Float getBenzene() {
        return this.benzene;
    }

    public void setBenzene(Float benzene) {
        this.benzene = benzene;
    }

    public Float getHumidity() {
        return humidity;
    }

    public void setHumidity(Float humidity) {
        this.humidity = humidity;
    }

    public String getDevice_timestamp() {
        return this.device_timestamp;
    }

    public void setDevice_timestamp(String device_timestamp) {
        this.device_timestamp = device_timestamp;
    }
}

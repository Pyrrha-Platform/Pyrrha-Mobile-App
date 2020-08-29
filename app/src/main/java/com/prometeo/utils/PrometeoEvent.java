package com.prometeo.utils;

public class PrometeoEvent {
    private static final String TAG = PrometeoEvent.class.getName();

    private Float temp;
    private Float humidity;
    private Float CO;
    private Float no2;
    private Float formaldehyde;
    private Float acroleine;
    private Float benzene;

    public Float getTemp() {
        return temp;
    }

    public void setTemp(Float temp) {
        this.temp = temp;
    }

    public Float getCO() {
        return CO;
    }

    public void setCO(Float CO) {
        this.CO = CO;
    }

    public Float getNo2() {
        return no2;
    }

    public void setNo2(Float no2) {
        this.no2 = no2;
    }

    public Float getFormaldehyde() {
        return formaldehyde;
    }

    public void setFormaldehyde(Float formaldehyde) {
        this.formaldehyde = formaldehyde;
    }

    public Float getAcroleine() {
        return acroleine;
    }

    public void setAcroleine(Float acroleine) {
        this.acroleine = acroleine;
    }

    public Float getBenzene() {
        return benzene;
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
}

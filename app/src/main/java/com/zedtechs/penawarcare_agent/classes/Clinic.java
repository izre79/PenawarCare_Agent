package com.zedtechs.penawarcare_agent.classes;

public class Clinic {
    public String NAME;
    public String CLINIC_CODE;
    public String ADDRESS;
    public String PHONE;
    public Double MAP_LATITUDE;
    public Double MAP_LONGITUDE;

    Double DISTANCE;

    public Double getDistance() {
        if (DISTANCE!=null) return DISTANCE; else return new Double(1000000);
    }

}


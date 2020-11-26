package com.zedtechs.penawarcare_agent.classes;

import com.google.gson.JsonObject;

public class Job {

    public String JOB_ID;
    public String REGISTRATION_ID;
    public String AGENT_ID;
    public String APPOINT_DATETIME;
    public String OFFER_DATETIME;
    public String CLINIC_CODE;
    public String CLINIC_NAME;
    public String JOB_CATEGORY_NAME;
    public String JOB_STATUS_CODE;
    public String JOB_STATUS_DESC;

    public Job (JsonObject obj) {

        System.out.println("Init job object : " + obj.get("JOB_ID").toString());

        this.JOB_ID = obj.get("JOB_ID").toString();
        this.REGISTRATION_ID = obj.get("REGISTRATION_ID").toString();
        this.AGENT_ID = obj.get("AGENT_ID").toString();
        if (obj.has("APPOINT_DATETIME")) this.APPOINT_DATETIME = obj.get("APPOINT_DATETIME").toString();
        this.OFFER_DATETIME = obj.get("OFFER_DATETIME").toString();
        this.CLINIC_CODE = obj.get("CLINIC_CODE").toString();
        this.CLINIC_NAME = obj.get("CLINIC_NAME").toString();
        this.JOB_CATEGORY_NAME = obj.get("JOB_CATEGORY_NAME").toString();
        this.JOB_STATUS_CODE = obj.get("JOB_STATUS_CODE").toString();
        this.JOB_STATUS_DESC = obj.get("JOB_STATUS_DESC").toString();
    }

    public String getJobID(){
        return "27";
    }

}

package com.zedtechs.penawarcare_agent.classes;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Consultation {

    public String REGISTRATION_ID;
    public String CONSULTATION_ID;
    public String CLINIC_CODE;
    public String CLINIC_NAME;
    public String DOCTOR_NAME;
    public String DOCTOR_APC;
    public String BILL_FINAL_STATUS;
    public String QUEUE_NO;
    public String QUEUE_NO_DATETIME;
    public String DONE_STATUS;
    public String PATREGNO;
    public String DIAGNOSIS;
    public String CLINICAL_NOTES;
    public String REGISTER_DATE;
    public String REGISTER_TIME;
    public String AMOUNT;
    public List<ConsItem> ITEM_DATA;

    public Consultation (JsonObject obj) {
        this.REGISTRATION_ID = obj.get("REGISTRATION_ID").toString();
        this.CONSULTATION_ID = obj.get("CONSULTATION_ID").toString();
        this.CLINIC_CODE = obj.get("CLINIC_CODE").toString();
        this.CLINIC_NAME = obj.get("CLINIC_NAME").toString();
        this.DOCTOR_NAME = obj.get("DOCTOR_NAME").toString();
        this.DOCTOR_APC = obj.get("DOCTOR_APC").toString();
        this.BILL_FINAL_STATUS = obj.get("BILL_FINAL_STATUS").toString();
        this.QUEUE_NO = obj.get("QUEUE_NO").toString();
        this.QUEUE_NO_DATETIME = obj.get("QUEUE_NO_DATETIME").toString();
        this.DONE_STATUS = obj.get("DONE_STATUS").toString();
        this.REGISTER_DATE = obj.get("BILL_DATE").toString();
        this.REGISTER_TIME = obj.get("BILL_TIME").toString();
        this.PATREGNO = obj.get("PATREGNO").toString();
        this.DIAGNOSIS = obj.get("DIAGNOSIS").toString();
        this.CLINICAL_NOTES = obj.get("CLINICAL_NOTES").toString();
        this.AMOUNT = obj.get("AMOUNT").toString();
        this.ITEM_DATA = new ArrayList<>();
    }

    public class ConsItem{

        public String ITEM_CODE;
        public String ITEM_DESC;
        public Double AMOUNT;
        public Double QUANTITY;
        public String MEASURE_UNIT;

        public ConsItem(JsonObject obj) {

            this.ITEM_CODE = obj.get("ITEM_CODE").toString();
            this.ITEM_DESC = obj.get("ITEM_DESC").toString();
            this.AMOUNT = obj.get("AMOUNT").getAsDouble();
            this.QUANTITY = obj.get("QUANTITY").getAsDouble();
            this.MEASURE_UNIT = obj.get("MEASURE_UNIT").toString();
        }
    }


}

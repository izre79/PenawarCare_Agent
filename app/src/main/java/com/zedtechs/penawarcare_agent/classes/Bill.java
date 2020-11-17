package com.zedtechs.penawarcare_agent.classes;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Bill {


    public String BILL_ID;
    public String PATREGNO;
    public String BILL_REG_NO;
    public String BILL_DATE;
    public String BILL_TIME;
    public String CLINIC_CODE;
    public String CLINIC_NAME;
    public Double AMOUNT;
    public List<BillItem> ITEM_DATA;

    public Bill (JsonObject obj) {
        this.BILL_ID = obj.get("BILL_ID").toString();
        this.BILL_REG_NO = obj.get("BILL_REG_NO").toString();
        this.CLINIC_CODE = obj.get("CLINIC_CODE").toString();
        this.CLINIC_NAME = obj.get("CLINIC_NAME").toString();
        this.BILL_DATE = obj.get("BILL_DATE").toString();
        this.BILL_TIME = obj.get("BILL_TIME").toString();
        this.PATREGNO = obj.get("PATREGNO").toString();
        this.AMOUNT = obj.get("AMOUNT").getAsDouble();

        this.ITEM_DATA = new ArrayList<>();
    }

    public class BillItem{

        public String ITEM_CODE;
        public String ITEM_DESC;
        public Double TRANS_AMOUNT;
        public Double QUANTITY;
        public String MEASURE_UNIT;

        public BillItem(JsonObject obj) {

            this.ITEM_CODE = obj.get("ITEM_CODE").toString();
            this.ITEM_DESC = obj.get("ITEM_DESC").toString();
            this.TRANS_AMOUNT = obj.get("TRANS_AMOUNT").getAsDouble();
            this.QUANTITY = obj.get("QUANTITY").getAsDouble();
            this.MEASURE_UNIT = obj.get("MEASURE_UNIT").toString();
        }
    }



}

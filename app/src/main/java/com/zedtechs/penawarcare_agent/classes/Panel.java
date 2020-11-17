package com.zedtechs.penawarcare_agent.classes;

import com.google.gson.JsonObject;

public class Panel {

    public String PANEL_CODE;
    public String PANEL_NAME;
    public String FULL_ADDRESS;
    public String EMPLOYEE_NO;

    public Panel (JsonObject obj) {
        this.PANEL_CODE = obj.get("PANEL_CODE").toString();
        this.PANEL_NAME = obj.get("PANEL_NAME").toString();
        this.FULL_ADDRESS = obj.get("FULL_ADDRESS").toString();
        this.EMPLOYEE_NO = obj.get("EMPLOYEE_NO").toString();

    }
}

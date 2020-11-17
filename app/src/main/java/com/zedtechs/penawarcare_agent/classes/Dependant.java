package com.zedtechs.penawarcare_agent.classes;

import com.google.gson.JsonObject;

public class Dependant {

    public String PATREGNO;
    public String NAME;
    public String RELATION;

    public Dependant (JsonObject obj) {
        this.PATREGNO = obj.get("PATREGNO").toString();
        this.NAME = obj.get("NAME").toString();
        this.RELATION = obj.get("RELATION").toString();

    }
}

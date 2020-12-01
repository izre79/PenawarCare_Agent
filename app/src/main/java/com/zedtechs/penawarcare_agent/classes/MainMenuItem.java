package com.zedtechs.penawarcare_agent.classes;

public class MainMenuItem {
    private final String _title;
    private final String _desc;
    private boolean mOnline;

    public MainMenuItem(String title, String desc) {
        _title = title;
        _desc = desc;
    }

    public String getTitle() {
        return _title;
    }

    public String getDesc() {
        return _desc;
    }

}
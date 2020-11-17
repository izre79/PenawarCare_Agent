package com.zedtechs.penawarcare_agent.util;

import android.os.Build;

class GlobalFunctions {

    public String getAndroidVersion() {
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        return "Android SDK: " + sdkVersion + " (" + release +")";
    }
}

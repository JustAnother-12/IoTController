package com.example.iotcontroller.helper;

import android.content.Context;
import android.net.wifi.WifiManager;

import java.util.Locale;

public class helper {
    public static String extractLocationUrl(String responseData) {
        String[] lines = responseData.split("\r\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith("location:")) {
                // get the link behind "location:"
                return line.substring(9).trim();
            }
        }
        return null;
    }

    public static String getWifiIPAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }
}

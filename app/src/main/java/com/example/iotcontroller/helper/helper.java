package com.example.iotcontroller.helper;

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
}

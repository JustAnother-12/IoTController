package com.example.iotcontroller.model;

public class IoTDevice {
    private String name;
    private String IP;
    private String client_key;

    public IoTDevice(String name, String IP) {
        this.name = name;
        this.IP = IP;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getClient_key() {
        return client_key;
    }

    public void setClient_key(String client_key) {
        this.client_key = client_key;
    }
}

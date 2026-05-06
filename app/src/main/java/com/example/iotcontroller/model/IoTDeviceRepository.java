package com.example.iotcontroller.model;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;

public class IoTDeviceRepository {
    private static IoTDeviceRepository instance;
    private MutableLiveData<ArrayList<IoTDevice>> deviceList = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<IoTDevice> pairedDevice = new MutableLiveData<>();

    public static synchronized IoTDeviceRepository getInstance() {
        if (instance == null) instance = new IoTDeviceRepository();
        return instance;
    }
    public MutableLiveData<ArrayList<IoTDevice>> getDeviceList() {
        return deviceList;
    }

    public MutableLiveData<IoTDevice> getPairedDevice(){
        return pairedDevice;
    }

    public void updatePairedDevice(IoTDevice device){
        pairedDevice.postValue(device);
    }
    public void updateDevices(ArrayList<IoTDevice> newList) {
        deviceList.postValue(newList);
    }
}

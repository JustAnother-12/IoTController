package com.example.iotcontroller.controllers;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import androidx.annotation.NonNull;

public class FlashlightController {
    private CameraManager cameraManager;
    private boolean isEnabled;

    public FlashlightController(Context context){
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.isEnabled = false;

        registerCallBack();
    }

    private void registerCallBack(){
        CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                isEnabled = enabled;
            }
        };
        cameraManager.registerTorchCallback(torchCallback, null);
    }

    public void toggleFlashlight(){
        try{
            String cameraId = cameraManager.getCameraIdList()[0];
            Log.i("Flashlight DEBUG:", "Enabled: " + isEnabled);
            if(isEnabled)
                cameraManager.setTorchMode(cameraId, false);
            else
                cameraManager.setTorchMode(cameraId, true);
        }catch (Exception e){
            Log.e("IOT", "Exception: " + e.getMessage());
        }
    }
}

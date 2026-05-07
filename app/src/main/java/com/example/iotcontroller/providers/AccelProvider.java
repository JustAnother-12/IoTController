package com.example.iotcontroller.providers;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.iotcontroller.interfaces.OnSensorActionListener;

public class AccelProvider implements SensorEventListener {
    private static final float FLASHLIGHT_THRESHOLD = 15f;
    private static final float IOT_THRESHOLD = 20f;
    private static final float VOLUME_CEIL_THRESHOLD = 12f;
    private static final float VOLUME_FlOOR_THRESHOLD = 3f;
    private final OnSensorActionListener listener;
    private long lastUpdated;

    private final SharedPreferences sharedPreferences;

    public AccelProvider(Context context){
        this.listener = (OnSensorActionListener) context;
        this.sharedPreferences = context.getSharedPreferences("SmartControlPreference", Context.MODE_PRIVATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //check if the toggle is on
            if (sharedPreferences.getBoolean("VolumeControl", false))
                callForVolumeChange(event);
            if (sharedPreferences.getBoolean("FlashlightControl", false))
                callForFlashlightToggle(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    private void callForVolumeChange(SensorEvent event){
        float x = event.values[0];
        long currentTime = System.currentTimeMillis();

        //  100ms delay from last update
        if((currentTime - lastUpdated) > 100){
            if(x > VOLUME_FlOOR_THRESHOLD && x < VOLUME_CEIL_THRESHOLD){
                // callback
                if(listener != null) listener.onVolumeTargetChanged(1);
                lastUpdated = currentTime;
            }else if(x < -VOLUME_FlOOR_THRESHOLD && x > -VOLUME_CEIL_THRESHOLD){
                // callback
                if(listener != null) listener.onVolumeTargetChanged(-1);
                lastUpdated = currentTime;
            }
        }
    }

    private void callForFlashlightToggle(SensorEvent event){
        float x = event.values[0];
        long currentTime = System.currentTimeMillis();

        //  300ms delay from last update
        if((currentTime - lastUpdated) > 300){
            if(x > FLASHLIGHT_THRESHOLD || x < -FLASHLIGHT_THRESHOLD){
                // callback
                if(listener != null) listener.onFlashlightTargetChanged();
                lastUpdated = currentTime;
            }
        }
    }
}

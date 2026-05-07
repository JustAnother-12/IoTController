package com.example.iotcontroller.providers;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.iotcontroller.interfaces.OnSensorActionListener;

public class GyroProvider implements SensorEventListener {
    private static final float THRESHOLD = 15f;
    private static final float POINTER_SENSITIVITY = 20f;
    private static final float POINTER_BOTTOM_THRESHOLD = 0.05f;
    private final OnSensorActionListener listener;
    private long lastUpdated;

    private final SharedPreferences sharedPreferences;

    public GyroProvider(Context context){
        this.listener = (OnSensorActionListener) context;
        this.sharedPreferences = context.getSharedPreferences("SmartControlPreference", Context.MODE_PRIVATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //check if the Flashlight toggle is on
        if(sharedPreferences.getBoolean("MediaControl", false))
            callForMediaChange(event);
        if(sharedPreferences.getBoolean("SmartTVControl", false)
                && sharedPreferences.getBoolean("SyncPointer", false))
            callForPointerChange(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void callForPointerChange(SensorEvent event){
        long currentTime = System.currentTimeMillis();
        // get angular velocity
        float x = event.values[0];
        float z = event.values[2];

        //skip if value is too low
        if(Math.abs(x) < POINTER_BOTTOM_THRESHOLD) x = 0f;
        if(Math.abs(z) < POINTER_BOTTOM_THRESHOLD) z = 0f;

        //calculate angular distance with sensitivity multiplier (distance = velocity * time step)
        // skip time step because it's too small
        int dx = (int) (-z * POINTER_SENSITIVITY);
        int dy = (int) (-x * POINTER_SENSITIVITY);

        //  20ms delay from last update
        if((currentTime - lastUpdated) > 20) {
            if(dx != 0 || dy != 0){
                if(listener != null){
                    listener.onPointerMovementChanged(dx, dy);
                    lastUpdated = currentTime;
                }
            }
        }
    }

    private void callForMediaChange(SensorEvent event){
        float x = event.values[0];
        long currentTime = System.currentTimeMillis();

        //  300ms delay from last update
        if((currentTime - lastUpdated) > 300){
//            if(x > THRESHOLD || x < -THRESHOLD){
//                // callback
//                if(listener != null) listener.onMediaSkipTarget(true);
//                lastUpdated = currentTime;
//            }
        }
    }
}

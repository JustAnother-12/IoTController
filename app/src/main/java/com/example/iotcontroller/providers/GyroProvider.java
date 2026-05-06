package com.example.iotcontroller.providers;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.iotcontroller.interfaces.OnSensorActionListener;

public class GyroProvider implements SensorEventListener {
    private Context context;
    private static final float THRESHOLD = 15f;
    private final OnSensorActionListener listener;
    private long lastUpdated;

    private final SharedPreferences sharedPreferences;

    public GyroProvider(Context context){
        this.context = context;
        this.listener = (OnSensorActionListener) context;
        this.sharedPreferences = context.getSharedPreferences("SmartControlPreference", Context.MODE_PRIVATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //check if the Flashlight toggle is on
        if(sharedPreferences.getBoolean("MediaControl", false))
            callForMediaChange(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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

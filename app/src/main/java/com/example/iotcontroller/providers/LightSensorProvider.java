package com.example.iotcontroller.providers;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.iotcontroller.interfaces.OnSensorActionListener;

public class LightSensorProvider implements SensorEventListener {
    private float lastLux = 0;
    private long lastDarkTime = 0;
    private static final float DARK_THRESHOLD = 20.0f;
    private static final int WAVE_TIMEOUT = 600;
    private final OnSensorActionListener listener;
    private final SharedPreferences sharedPreferences;

    public LightSensorProvider(Context context){
        this.listener = (OnSensorActionListener) context;
        this.sharedPreferences = context.getSharedPreferences("SmartControlPreference", Context.MODE_PRIVATE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sharedPreferences.getBoolean("FlashlightControl", false))
            callForFlashlightToggle(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void callForFlashlightToggle(SensorEvent event){
       float currentLux = event.values[0];
       long currentTime = System.currentTimeMillis();

       if(currentLux < DARK_THRESHOLD && lastLux >= DARK_THRESHOLD){
           lastDarkTime = currentTime;
       } else if (currentLux >= DARK_THRESHOLD && lastLux < DARK_THRESHOLD) {
           if(lastDarkTime != 0){
               long duration = currentTime - lastDarkTime;

               if(duration > 100 && duration < WAVE_TIMEOUT){
                   if(listener != null) listener.onFlashlightTargetChanged();
               }
               lastDarkTime = 0;
           }
       }
        lastLux = currentLux;
    }
}

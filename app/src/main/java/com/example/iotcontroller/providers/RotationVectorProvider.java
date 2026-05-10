package com.example.iotcontroller.providers;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.example.iotcontroller.interfaces.OnSensorActionListener;

public class RotationVectorProvider implements SensorEventListener {
    private boolean isWaitingForReset = false;
    private boolean UIVideoActive = false;
    private static final float VIDEO_THRESHOLD = 30f;

    private final OnSensorActionListener listener;
    private long lastUpdated;

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationValues = new float[3];
    private float startAngleY = Float.NaN;

    public RotationVectorProvider(Context context){
        this.listener = (OnSensorActionListener) context;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(UIVideoActive)
            callForMediaChange(event);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setUIVideoActive(boolean isVisible) {
        UIVideoActive = isVisible;
        if (!isVisible) {
            startAngleY = Float.NaN;
        }
    }

    private void callForMediaChange(SensorEvent event){

        float currentAngleY = conversion(event);
        if (Float.isNaN(startAngleY)) {
            startAngleY = currentAngleY;
            return;
        }

        float deltaAngle = currentAngleY - startAngleY;
        long currentTime = System.currentTimeMillis();

        if (isWaitingForReset) {
            if (Math.abs(deltaAngle) < 10) {
                isWaitingForReset = false;
                Log.d("RotationProvider", "Đã về vùng trung tâm - Sẵn sàng");
            }
            return;
        }

        //  600ms delay from last update
        if((currentTime - lastUpdated) > 600){
            if(deltaAngle > VIDEO_THRESHOLD){
                Log.d("RotationProvider", "Nghiêng Phải: " + deltaAngle);
                // callback
                if(listener != null) listener.onMediaSkipTarget(true);
                lastUpdated = currentTime;
                isWaitingForReset = true;
            } else if (deltaAngle < -VIDEO_THRESHOLD) {
                Log.d("RotationProvider", "Nghiêng Trái: " + deltaAngle);
                // callback
                if(listener != null) listener.onMediaSkipTarget(false);
                lastUpdated = currentTime;
                isWaitingForReset = true;
            }
        }
    }

    private float conversion(SensorEvent event){
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

        // orientationValues[2]: Roll (Y)
        SensorManager.getOrientation(rotationMatrix, orientationValues);
        
        return (float) Math.toDegrees(orientationValues[2]);
    }
}

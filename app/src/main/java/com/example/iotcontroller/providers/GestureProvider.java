package com.example.iotcontroller.providers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class GestureProvider extends GestureDetector.SimpleOnGestureListener {
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public GestureProvider(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("SmartControlPreference", Context.MODE_PRIVATE);
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        if(sharedPreferences.getBoolean("SmartTVControl", false)
                && sharedPreferences.getString("ConnectedIp", null) != null
                && sharedPreferences.getString("ClientKey", null) != null){

            sendActionBroadcast("ENTER");
        }
        return true;
    }

    @Override
    public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
        if(sharedPreferences.getBoolean("SmartTVControl", false)
                && sharedPreferences.getString("ConnectedIp", null) != null
                && sharedPreferences.getString("ClientKey", null) != null){

            if(e1 != null){
                float difX = e2.getX() - e1.getX();
                float difY = e2.getY() - e1.getY();

                if(Math.abs(difX) > Math.abs(difY)){ //horizontal swipe
                    if(Math.abs(difX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD){
                        if(difX > 0)
                            sendActionBroadcast("LEFT");
                        else
                            sendActionBroadcast("RIGHT");
                    }
                } else if(Math.abs(difX) < Math.abs(difY)){ //vertical swipe
                    if(Math.abs(difY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD){
                        if(difY > 0)
                            sendActionBroadcast("UP");
                        else
                            sendActionBroadcast("DOWN");
                    }
                }
            }
        }
        return true;
    }

    public void handleTwoFingerGestures(MotionEvent event){

        if(sharedPreferences.getBoolean("SmartTVControl", false)
                && sharedPreferences.getString("ConnectedIp", null) != null
                && sharedPreferences.getString("ClientKey", null) != null) {

            float startX2 = 0;
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    startX2 = event.getX();
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    float endX2 = event.getX();
                    float deltaX = endX2 - startX2;
                    if (Math.abs(deltaX) > 150) {
                        if (deltaX > 0) sendActionBroadcast("BACK");
                        else sendActionBroadcast("HOME");
                    }
                    break;
            }
        }
    }

    private void sendActionBroadcast(String actionName) {
        Intent intent = new Intent("COM_IOT_GESTURE");
        intent.putExtra("action_type", actionName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}

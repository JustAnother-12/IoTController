package com.example.iotcontroller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.iotcontroller.services.SensorService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity{
    private BottomNavigationView bottomNavigationView;

    //fragment
    IoTControllerFragment ioTFragment;
    FeatureSwitchFragment featureFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.navigationBar);

        bottomNavigationView.setOnItemSelectedListener((item) ->{
            int itemId = item.getItemId();

            if (itemId == R.id.navItemFeatures){
                switchFragment(featureFragment);
                return true;
            }else if (itemId == R.id.navItemMedia){
//                switchFragment(mediaFragment);
                return true;
            }else if (itemId == R.id.navItemSmartTV){
                switchFragment(ioTFragment);
                return true;
            }
            return false;
        });

        if(savedInstanceState == null){
            ioTFragment = IoTControllerFragment.newInstance();
            featureFragment = FeatureSwitchFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, ioTFragment, "IOT")
                    .add(R.id.fragment_container, featureFragment, "FEATURE")
                    .hide(ioTFragment)
                    .commitAllowingStateLoss();

            bottomNavigationView.setSelectedItemId(R.id.navItemFeatures);
        } else {
            ioTFragment = (IoTControllerFragment) getSupportFragmentManager().findFragmentByTag("IOT");
            featureFragment = (FeatureSwitchFragment) getSupportFragmentManager().findFragmentByTag("FEATURE");
        }

    }

    private void switchFragment(Fragment toShow) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Ẩn tất cả các Fragment hiện có
        if (ioTFragment != null) ft.hide(ioTFragment);
        if (featureFragment != null) ft.hide(featureFragment);
//        if (mediaFragment != null) ft.hide(mediaFragment);

        ft.show(toShow).commit();
    }

    private void stopSensorService(){
        Intent intent = new Intent(this, SensorService.class);
        intent.setAction("ACTION_STOP_SERVICE");
        stopService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSensorService();
    }
}

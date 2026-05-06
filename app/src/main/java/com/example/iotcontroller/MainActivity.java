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

import com.example.iotcontroller.interfaces.OnSensorActionListener;
import com.example.iotcontroller.services.SensorService;

public class MainActivity extends AppCompatActivity{
    private SwitchCompat tglMaster;
    private SwitchCompat tglVolume;
    private SwitchCompat tglMedia;
    private SwitchCompat tglFlashlight;
    private SwitchCompat tglSmartTV;

    private SharedPreferences sharedPreferences;

    //fragment
    IoTControllerFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tglMaster = findViewById(R.id.tgl_master);
        tglVolume = findViewById(R.id.tgl_volume);
        tglMedia = findViewById(R.id.tgl_media);
        tglFlashlight = findViewById(R.id.tgl_flashlight);
        tglSmartTV = findViewById(R.id.tgl_smartTV);

        sharedPreferences = getSharedPreferences("SmartControlPreference", Context.MODE_PRIVATE);

        if(savedInstanceState == null){
            findViewById(R.id.fragment_container).post(() -> {
                fragment = IoTControllerFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, fragment, "BT_CONTROLLER_TAG")
                        .hide(fragment)
                        .commitAllowingStateLoss();
            });
        }

        //handle toggle
        tglMaster.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    checkAndStartService();
                }else{
                    tglVolume.setChecked(false);
                    tglMedia.setChecked(false);
                    tglFlashlight.setChecked(false);
                    stopSensorService();
                }
            }
        });

        tglVolume.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("VolumeControl", tglVolume.isChecked());
                editor.apply();
            }
        });

        tglMedia.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("MediaControl", tglMedia.isChecked());
                editor.apply();
            }
        });

        tglFlashlight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("FlashlightControl", tglFlashlight.isChecked());
                editor.apply();
            }
        });

        tglSmartTV.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("SmartTVControl", tglSmartTV.isChecked());
                editor.apply();

                toggleFragment(isChecked);
            }
        });
    }

    private void toggleFragment(boolean enable){
        if(enable){
            getSupportFragmentManager().beginTransaction()
                    .show(fragment)
                    .commitAllowingStateLoss();
        }else{
            getSupportFragmentManager().beginTransaction()
                    .hide(fragment)
                    .commitAllowingStateLoss();
        }

    }
    private void checkAndStartService() {
        // list of relevant permission
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.CAMERA
        };

        // if the permissions are granted, start the service, else ask for permission
        if (hasAllPermissions(permissions)) {
            startSensorService();
        }else{
            requestPermissions(permissions, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSensorService();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền để các tính năng có thể hoạt động!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean hasAllPermissions(String[] permissions){
        for (String permission:permissions) {
            int status = ContextCompat.checkSelfPermission(MainActivity.this, permission);
            if (status != PackageManager.PERMISSION_GRANTED) {
                return false;
            }else{
                continue;
            }
        }

        return true;
    }

    private void startSensorService() {
        Context context = getApplicationContext();
        Intent intent = new Intent(this, SensorService.class);
        intent.setAction("ACTION_START_SERVICE");
        try {
            context.startForegroundService(intent);
        } catch (Exception e) {
            Log.e("IOT_DEBUG", "Lỗi thực thi startService: " + e.getMessage());
            e.printStackTrace();
        }
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

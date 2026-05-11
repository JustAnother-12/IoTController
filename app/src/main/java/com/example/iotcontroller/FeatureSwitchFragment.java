package com.example.iotcontroller;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.example.iotcontroller.services.SensorService;

public class FeatureSwitchFragment extends Fragment {

    private SwitchCompat tglMaster;
    private SwitchCompat tglVolume;
    private SwitchCompat tglFlashlight;

    private SharedPreferences sharedPreferences;
    //BroadcastReceiver
    private BroadcastReceiver localBroadcastReceiver;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) { allGranted = false; break; }
                }
                if (allGranted) {
                    startSensorService();
                } else {
                    Toast.makeText(getContext(), "Cần cấp quyền để hoạt động!", Toast.LENGTH_SHORT).show();
                }
            });

    public FeatureSwitchFragment() {
        // Required empty public constructor
    }


    public static FeatureSwitchFragment newInstance() {
        FeatureSwitchFragment fragment = new FeatureSwitchFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_feature_switch, container, false);

        tglMaster = rootView.findViewById(R.id.tgl_master);
        tglVolume = rootView.findViewById(R.id.tgl_volume);
        tglFlashlight = rootView.findViewById(R.id.tgl_flashlight);

        sharedPreferences = requireContext().getSharedPreferences("SmartControlPreference", Context.MODE_PRIVATE);

        tglVolume.setChecked(sharedPreferences.getBoolean("VolumeControl", false));
        tglFlashlight.setChecked(sharedPreferences.getBoolean("FlashlightControl", false));

        localBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String actionName = intent.getStringExtra("action");
                if ("TGL_MASTER_OFF".equals(actionName)) {
                    tglMaster.setChecked(false);
                }
            }
        };

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                localBroadcastReceiver, new IntentFilter("COMMAND_FEATURE_UI"));

        //handle toggle
        tglMaster.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    checkAndStartService();
                }else{
                    tglVolume.setChecked(false);
                    tglFlashlight.setChecked(false);
                    stopSensorService();
                }
            }
        });

        setupToggleListener(tglVolume, "VolumeControl");
        setupToggleListener(tglFlashlight, "FlashlightControl");
        // Inflate the layout for this fragment
        return rootView;
    }

    private void setupToggleListener(SwitchCompat sw, String key) {
        sw.setOnCheckedChangeListener((v, isChecked) -> {
            sharedPreferences.edit().putBoolean(key, isChecked).apply();
        });
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
            requestPermissionLauncher.launch(permissions);
        }
    }

    private boolean hasAllPermissions(String[] permissions){
        for (String permission:permissions) {
            int status = ContextCompat.checkSelfPermission(requireContext(), permission);
            if (status != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private void startSensorService() {
        Context context = getContext();
        Intent intent = new Intent(context, SensorService.class);
        intent.setAction("ACTION_START_SERVICE");
        if(context != null){
            try {
                context.startForegroundService(intent);
            } catch (Exception e) {
                Log.e("IOT_DEBUG", "Lỗi thực thi startService: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void stopSensorService(){
        Context context = getContext();
        Intent intent = new Intent(context, SensorService.class);
        intent.setAction("ACTION_STOP_SERVICE");
        if(context != null)
            context.stopService(intent);
    }
}
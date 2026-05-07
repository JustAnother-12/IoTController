package com.example.iotcontroller;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iotcontroller.model.IoTDevice;
import com.example.iotcontroller.model.IoTDeviceRepository;
import com.example.iotcontroller.providers.GestureProvider;
import com.example.iotcontroller.services.SensorService;

import java.util.ArrayList;


public class IoTControllerFragment extends Fragment {
    private TextView txtPairedName, txtPairedIP;
    private EditText hiddenEditText;
    private GestureOverlayView gestureOverlayView;
    private Button btnSyncKeyboard;
    private SwitchCompat tglSyncPointer;
    private ToggleButton btnDiscover;
    private ArrayList<IoTDevice> deviceList;
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private IoTDeviceRepository ioTDeviceRepository;
    private GestureDetector gestureDetector;
    private GestureProvider gestureProvider;
    //BroadcastReceiver
    private BroadcastReceiver broadcastReceiver;

    private InputMethodManager inputMethodManager;

    private SharedPreferences sharedPreferences;


    public IoTControllerFragment() {
        // Required empty public constructor
    }

    public static IoTControllerFragment newInstance() {
        IoTControllerFragment fragment = new IoTControllerFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_iot_controller, container, false);

        txtPairedName = rootView.findViewById(R.id.txt_paired_name);
        txtPairedIP = rootView.findViewById(R.id.txt_paired_IP);
        hiddenEditText = rootView.findViewById(R.id.hiddenEdit);
        btnDiscover = rootView.findViewById(R.id.btn_Discover);
        btnSyncKeyboard = rootView.findViewById(R.id.btn_sync_keyboard);
        tglSyncPointer = rootView.findViewById(R.id.btn_sync_pointer);
        recyclerView = rootView.findViewById(R.id.recylerDevices);
        gestureOverlayView = rootView.findViewById(R.id.gestureOverlay);

        if (deviceList == null) deviceList = new ArrayList<>();

        if(recyclerView != null){
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            deviceAdapter = new DeviceAdapter(getContext(), deviceList);
            recyclerView.setAdapter(deviceAdapter);
        }else{
            Log.e("IOT_DEBUG", "Cannot find recycleView in layout!");
        }

        gestureProvider = new GestureProvider(getContext());
        gestureDetector = new GestureDetector(getContext(), gestureProvider);

        inputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        sharedPreferences = requireContext().getSharedPreferences("SmartControlPreference", Context.MODE_PRIVATE);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String actionName = intent.getStringExtra("action");
                if ("SHOW_KEYBOARD".equals(actionName)) {
                    hiddenEditText.requestFocus();
                    inputMethodManager.showSoftInput(hiddenEditText, InputMethodManager.SHOW_IMPLICIT);
                } else if ("HIDE_KEYBOARD".equals(actionName)) {
                    inputMethodManager.hideSoftInputFromWindow(hiddenEditText.getWindowToken(), 0);
                }
            }
        };

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                broadcastReceiver, new IntentFilter("COMMAND_UI"));

        ioTDeviceRepository = IoTDeviceRepository.getInstance();
        ioTDeviceRepository.getDeviceList().observe(getViewLifecycleOwner(), devices ->{
            if (devices != null) {
                deviceAdapter.updateData(devices);
                Log.d("BT_Fragment", "UI đã cập nhật với " + devices.size() + " thiết bị");
            }
        });
        ioTDeviceRepository.getPairedDevice().observe(getViewLifecycleOwner(), device -> {
            if(device != null){
                txtPairedName.setText(device.getName());
                txtPairedIP.setText(device.getIP());
            }
        });

        //handle event
        btnDiscover.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    startSSDPScan();
                }else{
                    stopSSDPScan();
                }
            }
        });
        tglSyncPointer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("SyncPointer", tglSyncPointer.isChecked());
                editor.apply();
            }
        });
        btnSyncKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subscribeKeyboard();
            }
        });

        hiddenEditText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    sendActionToService("BACKSPACE");
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    sendActionToService("ENTER_KEY");
                    return true;
                }
            }
            return false;
        });

        hiddenEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count > before){
                    String newChar = s.subSequence(start, start + count).toString();
                    sendActionToService(newChar);
                }
            }
        });

        gestureOverlayView.setOnTouchListener((v, event) ->{
            int pointerCount = event.getPointerCount();
            if(pointerCount == 1){
                return gestureDetector.onTouchEvent(event);
            } else if (pointerCount == 2 ) {
                gestureProvider.handleTwoFingerGestures(event);
                return true;
            }
            return true;
        });

        // Inflate the layout for this fragment
        return rootView;
    }
    private void startSSDPScan() {
        Intent intent = new Intent(getContext(), SensorService.class);
        intent.setAction("ACTION_START_SCAN");
        getContext().startService(intent);
    }

    private void stopSSDPScan(){
        Intent intent = new Intent(getContext(), SensorService.class);
        intent.setAction("ACTION_STOP_SCAN");
        getContext().startService(intent);
    }

    private void subscribeKeyboard(){
        Intent intent = new Intent(getContext(), SensorService.class);
        intent.setAction("ACTION_SUBSCRIBE_KEYBOARD");
        getContext().startService(intent);
    }

    private void sendActionToService(String keycode){
        Intent intent = new Intent("IOT_COMMAND");
        intent.putExtra("typed_key", keycode);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
    }
}
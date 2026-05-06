package com.example.iotcontroller.services;

import static com.example.iotcontroller.helper.helper.extractLocationUrl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.gesture.GestureOverlayView;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.Xml;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.iotcontroller.controllers.FlashlightController;
import com.example.iotcontroller.controllers.IoTController;
import com.example.iotcontroller.controllers.VolumeController;
import com.example.iotcontroller.interfaces.OnSensorActionListener;
import com.example.iotcontroller.model.IoTDevice;
import com.example.iotcontroller.model.IoTDeviceRepository;
import com.example.iotcontroller.providers.AccelProvider;
import com.example.iotcontroller.providers.GestureProvider;
import com.example.iotcontroller.providers.GyroProvider;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SensorService extends Service implements OnSensorActionListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private IoTDeviceRepository ioTDeviceRepository;
    private ArrayList<IoTDevice> discoveredDevices;
    private ExecutorService executorService;

    // Providers
    private AccelProvider accelProvider;
    private GyroProvider gyroProvider;

    // Controllers
    private VolumeController volumeController;
    private FlashlightController flashlightController;
    private IoTController ioTController;

    //SharedPreferences
    private SharedPreferences sharedPreferences;

    //WebOSClient
    private WebOSClient client = null;

    //BroadcastReceiver
    private BroadcastReceiver broadcastReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter("COM_EXAMPLE_IOT_GESTURE"));
    }

    private void init(){
        // sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        discoveredDevices = new ArrayList<>();

        // Providers
        accelProvider = new AccelProvider(this);
        gyroProvider = new GyroProvider(this);

        // Controllers
        volumeController = new VolumeController(this);
        flashlightController = new FlashlightController(this);
        ioTController = new IoTController();

        //LiveData
        ioTDeviceRepository = IoTDeviceRepository.getInstance();

        //ExecutorService
        executorService = Executors.newSingleThreadExecutor();

        //SharedPreferences
        sharedPreferences = getApplicationContext().getSharedPreferences("SmartControlPreference", Context.MODE_PRIVATE);

        //BroadcastReceiver
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String actionName = intent.getStringExtra("action_type");
                String newChars = intent.getStringExtra("new_typed_chars");
                if (actionName != null) {
                    handleGestureAction(actionName);
                }
                if (newChars != null){
                    if(ioTController != null){
                        ioTController.insertKeyboardText(newChars, false);
                    }
                }
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if ("ACTION_START_SCAN".equals(action)) {
                discoveredDevices.clear();
                scanIP();
            } else if ("ACTION_STOP_SCAN".equals(action)) {
                stopScanning();
            } else if ("ACTION_PAIR_DEVICE".equals(action)) {
                // disconnect first
                disconnect();

                String address = intent.getStringExtra("IP_ADDRESS");
                for(IoTDevice device : discoveredDevices){
                    if(device.getIP().equals(address)) {
                        pairDevice(device);
                    }
                }
            } else if ("ACTION_UNPAIR_DEVICE".equals(action)) {
                disconnect();
            } else if ("ACTION_SUBSCRIBE_KEYBOARD".equals(action)) {
                if(ioTController != null)
                    ioTController.subscribeKeyboard();
            } else if ("ACTION_STOP_SERVICE".equals(action)) {
                stopForeground(true);
                stopSelf();
            } else if ("ACTION_START_SERVICE".equals(action)) {
                String CHANNEL_ID = "sensor_service_channel";
                // create notification channel
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Sensor Service Channel",
                            NotificationManager.IMPORTANCE_LOW
                    );
                    NotificationManager manager = getSystemService(NotificationManager.class);
                    manager.createNotificationChannel(channel);
                }

                // create the notification
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Smart Control Active")
                        .setContentText("Đang lắng nghe cảm biến để điều khiển thiết bị...")
                        .build();

                if (Build.VERSION.SDK_INT >= 34) { // Android 14+
                    ServiceCompat.startForeground(
                            this,
                            1,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    );
                } else {
                    ServiceCompat.startForeground(
                            this,
                            1,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                    );
                }
                Toast.makeText(this, "Service Started!", Toast.LENGTH_SHORT).show();
                Log.d("SensorService", "Service đang hoạt động.");
                sensorRegister();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        if(sensorManager != null){
            sensorManager.unregisterListener(accelProvider);
            // add more later
        }
        Toast.makeText(this, "Service Stopped!", Toast.LENGTH_SHORT).show();
        Log.d("SensorService", "Service Stopped.");
    }
    private void scanIP(){
        new Thread(()->{
            try{
                String ssdpRequest = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 5\r\n" + // response time
                        "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n" +
                        "\r\n";

                byte[] requestData = ssdpRequest.getBytes();
                InetAddress address = InetAddress.getByName("239.255.255.250");
                DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, address, 1900);

                //send the data
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(5000);
                socket.send(requestPacket);

                //listen for response
                long startTime = System.currentTimeMillis();
                Set<String> foundIpsInThisSession = new HashSet<>();

                while (System.currentTimeMillis() - startTime < 5000){
                    try{
                        byte[] responseData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(responseData, responseData.length);
                        socket.receive(receivePacket);

                        String ip = receivePacket.getAddress().getHostAddress();
                        String data = new String(receivePacket.getData());
                        if (!foundIpsInThisSession.contains(ip)) {
                            foundIpsInThisSession.add(ip);
                            Log.d("IOT_DEBUG", "ResponseIP mới: " + ip);
                            String urlString = extractLocationUrl(data);
                            fetchAndParseXML(urlString, ip);
                        }
                    }catch (SocketTimeoutException e){
                        break;
                    }

                }
            }catch (Exception e){
                Log.e("IOT_DEBUG", "Lỗi SSDP: " + e.getMessage());
            }
        }).start();
    }

    private void stopScanning(){

    }

    private void fetchAndParseXML(String urlString, String ip){
        executorService.execute(() -> {
            try{
                Log.d("IOT DEBUG", "Fetching from URL: " + urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setDoInput(true);

                int responseCode = connection.getResponseCode();
                Log.d("IOT DEBUG", "Response Code: " + responseCode);

                if(responseCode == HttpURLConnection.HTTP_OK){
                    try(InputStream inputStream = connection.getInputStream()){
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(inputStream, "UTF-8");

                        int eventType = parser.getEventType();
                        String extractedName = null;

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                if (parser.getName().equalsIgnoreCase("friendlyName")) {
                                    extractedName = parser.nextText();
                                    break;
                                }
                            }
                            eventType = parser.next();
                        }

                        if (extractedName != null) {
                            final String finalName = extractedName.trim();
                            Log.d("IOT DEBUG", "Tìm thấy FriendlyName: " + finalName);

                            // save to repo
                            IoTDevice device = new IoTDevice(finalName, ip);
                            discoveredDevices.add(device);
                            ioTDeviceRepository.updateDevices(new ArrayList<>(discoveredDevices));
                        }
                    }
                } else {
                    Log.e("IOT DEBUG", "HTTP Error: " + responseCode);
                }
            }catch (Exception e){
                e.printStackTrace();
                Log.e("IOT DEBUG", "No data fetched! Check network & RSS format.");
            }
        });
    }

    private void pairDevice(IoTDevice device){
        try {
            URI uri = new URI("ws://" + device.getIP() + ":3000");
            client = new WebOSClient(uri, new WebOSClient.OnConnectionListener() {
                @Override
                public void onKeyboardFocused() {
                    Log.d("WebOS", "KEYBOARD FOCUSED");
                    Intent intent = new Intent("COMMAND_UI");
                    intent.putExtra("action", "SHOW_KEYBOARD");
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }

                @Override
                public void onKeyboardUnFocused() {
                    Log.d("WebOS", "KEYBOARD UNFOCUSED");
                    Intent intent = new Intent("COMMAND_UI");
                    intent.putExtra("action", "HIDE_KEYBOARD");
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }

                @Override
                public void onPairingRequired() {
                    Log.d("WebOS", "Vui lòng nhấn 'Đồng ý' trên màn hình TV");
                }

                @Override
                public void onConnected(String key) {
                    Log.d("WebOS", "Kết nối thành công! Client Key: " + key);
                    device.setClient_key(key);
                    ioTDeviceRepository.updatePairedDevice(device);

                    // Save key and IP in sharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("ConnectedIp", device.getIP());
                    editor.putString("ClientKey", key);
                    editor.apply();
                }

                @Override
                public void onError(String message) {
                    Log.e("WebOS", "Lỗi kết nối: " + message);
                }
            });
            client.connect();
            ioTController.setWebOSClient(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disconnect(){
        if(client != null && client.isOpen()){
            client.disconnect();
            client = null;
        }
    }

    private void sensorRegister(){
        if (accelerometer != null) {
            sensorManager.registerListener(accelProvider, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(gyroProvider, gyroscope, SensorManager.SENSOR_DELAY_UI);

            Log.d("IOT_DEBUG", "Sensor Registered!");
        } else {
            Log.e("IOT_DEBUG", "The device does not have an accelerometer!");
        }
    }

    private void handleGestureAction(String actionName){
        if(ioTController.getWebOSClient() != null){
            switch (actionName){
                case "TOAST":
                    ioTController.handleShowToast(actionName);
                    break;
                case "ENTER":
                    ioTController.handleRemoteButton("ENTER");
                    break;
                case "HOME":
                    ioTController.handleRemoteButton("HOME");
                    break;
                case "BACK":
                    ioTController.handleRemoteButton("BACK");
                    break;
                case "LEFT":
                    ioTController.handleRemoteButton("LEFT");
                    break;
                case "RIGHT":
                    ioTController.handleRemoteButton("RIGHT");
                    break;
                case "UP":
                    ioTController.handleRemoteButton("UP");
                    break;
                case "DOWN":
                    ioTController.handleRemoteButton("DOWN");
                    break;
            }
        }
    }

    @Override
    public void onVolumeTargetChanged(int direction) {
        volumeController.adjustVolume(direction);
    }

    @Override
    public void onFlashlightTargetChanged() {
        flashlightController.toggleFlashlight();
    }

    @Override
    public void onMediaSkipTarget(boolean forward) {

    }

    @Override
    public void onActionTriggered(String actionName) {
//        if(ioTController.getWebOSClient() != null){
//            switch (actionName){
//                case "TOAST":
//                    ioTController.handleShowToast(actionName);
//                    break;
//                case "ENTER":
//                    ioTController.handleRemoteButton("ENTER");
//                    break;
//                case "HOME":
//                    ioTController.handleRemoteButton("HOME");
//                    break;
//                case "BACK":
//                    ioTController.handleRemoteButton("BACK");
//                    break;
//                case "LEFT":
//                    ioTController.handleRemoteButton("LEFT");
//                    break;
//                case "RIGHT":
//                    ioTController.handleRemoteButton("RIGHT");
//                    break;
//                case "UP":
//                    ioTController.handleRemoteButton("UP");
//                    break;
//                case "DOWN":
//                    ioTController.handleRemoteButton("DOWN");
//                    break;
//            }
//        }
    }

}

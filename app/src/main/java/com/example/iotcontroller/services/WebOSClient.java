package com.example.iotcontroller.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class WebOSClient extends WebSocketClient {
    private String clientKey = null;
    private OnConnectionListener listener;

    private WebSocketClient pointerClient;

    private boolean isAuthorized = false;

    public interface OnConnectionListener {
        void onKeyboardFocused();
        void onKeyboardUnFocused();
        void onPairingRequired();
        void onConnected(String key);
        void onError(String message);
    }

    public WebOSClient(URI serverUri, OnConnectionListener listener) {
        super(serverUri);
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d("WebOS", "Đã mở kết nối WebSocket");
        sendRegisterPayload();
    }

    public void disconnect(){
        if(pointerClient != null && pointerClient.isOpen()){
            pointerClient.close();
            pointerClient = null;
            Log.d("WebOS", "Đã đóng Pointer Socket");
        }

        if(isOpen()){
            close();
            Log.d("WebOS", "Đã ngắt kết nối chính");
        }
    }

    private void sendRegisterPayload() {
        try {
            JSONObject root = new JSONObject();
            root.put("type", "register");
            root.put("id", "register_0");

            JSONObject payload = new JSONObject();
            payload.put("forcePairing", false);
            payload.put("pairingType", "PROMPT");

            JSONObject manifest = new JSONObject();
            JSONArray permissions = new JSONArray();
            permissions.put("LAUNCH");
            permissions.put("CONTROL_AUDIO");
            permissions.put("WRITE_NOTIFICATION_TOAST");
            permissions.put("CONTROL_INPUT_TEXT");
            permissions.put("CONTROL_INPUT_MOUSE");
            permissions.put("CONTROL_MOUSE_AND_KEYBOARD");
            permissions.put("READ_TV_CHANNEL_LIST");
            permissions.put("CONTROL_POWER");

            manifest.put("permissions", permissions);
            payload.put("manifest", manifest);

            if (clientKey != null) {
                payload.put("client-key", clientKey);
            }

            root.put("payload", payload);
            send(root.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(String message) {
        Log.d("WebOS", "Phản hồi từ TV: " + message);
        if (message.contains("\"type\":\"registered\"")) {
            isAuthorized = true;
            Log.d("WebOS", "Hệ thống đã sẵn sàng nhận lệnh!");
            sendSystemCommand("com.webos.service.networkinput/getPointerInputSocket", null);
        }
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type");

            if (json.has("payload") && json.getJSONObject("payload").has("socketPath")) {
                String socketUrl = json.getJSONObject("payload").getString("socketPath");
                Log.d("WebOS_Pointer", "Đã lấy được Pointer URL: " + socketUrl);

                setupPointerWebSocket(socketUrl);
            }

            if (json.has("payload") && json.getJSONObject("payload").has("currentWidget")) {
                JSONObject payload = json.getJSONObject("payload");
                JSONObject currentWidget = payload.getJSONObject("currentWidget");
                boolean isKeyboardVisible = currentWidget.optBoolean("focus", false);
                if (isKeyboardVisible) {
                    listener.onKeyboardFocused();
                }else
                    listener.onKeyboardUnFocused();
            }

            if ("registered".equals(type)) {
                JSONObject payload = json.optJSONObject("payload");
                if (payload != null) {
                    clientKey = payload.optString("client-key");
                    listener.onConnected(clientKey);
                }
            } else if ("response".equals(type) && message.contains("pairing")) {
                listener.onPairingRequired();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("WebOS", "Đã đóng kết nối: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        listener.onError(ex.getMessage());
    }

    public void sendPointerCommand(String type, String name, int dx, int dy){
        if (pointerClient != null && pointerClient.isOpen()) {
            try {
                StringBuilder sb = new StringBuilder();

                sb.append("type:").append(type).append("\n");

                if (type.equals("button")) {
                    sb.append("name:").append(name).append("\n");
                } else if (type.equals("move")) {
                    sb.append("dx:").append(dx).append("\n");
                    sb.append("dy:").append(dy).append("\n");
                    sb.append("down:0\n");
                }


                sb.append("\n");

                pointerClient.send(sb.toString());

            } catch (Exception e) {
                Log.e("WebOS_Pointer", "Lỗi gửi pointer: " + e.getMessage());
            }
        } else {
            Log.w("WebOS_Pointer", "PointerClient chưa sẵn sàng!");
        }
    }

    public void sendSystemCommand(String uri, JSONObject payload){
        if(isOpen() && isAuthorized){
            try{
                JSONObject request = new JSONObject();
                request.put("id", "sys_" + System.currentTimeMillis());
                request.put("type", "request");
                request.put("uri", "ssap://" + uri);
                request.put("payload", payload);

                send(request.toString());
            }catch (Exception e){
                Log.e("WebOS_Pointer", "Lỗi gửi system command: " + e.getMessage());
            }
        }
    }

    private void setupPointerWebSocket(String socketUrl) {
        try {
            URI uri = new URI(socketUrl);
            pointerClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d("WebOS_Pointer", "Pointer Socket đã mở!");
                }

                @Override
                public void onMessage(String message) {

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d("WebOS_Pointer", "Pointer Socket đã đóng");
                }

                @Override
                public void onError(Exception ex) {
                    Log.e("WebOS_Pointer", "Lỗi Pointer Socket: " + ex.getMessage());
                }
            };
            pointerClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}

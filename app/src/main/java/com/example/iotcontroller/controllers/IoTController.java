package com.example.iotcontroller.controllers;

import android.util.Log;

import com.example.iotcontroller.services.WebOSClient;

import org.json.JSONObject;

public class IoTController {

    private WebOSClient webOSClient;

    public IoTController() {
    }

    public void setWebOSClient(WebOSClient client) {
        this.webOSClient = client;
    }

    public WebOSClient getWebOSClient(){
        return webOSClient;
    }

    public void handlePointerMove(int dx, int dy) {
        if (webOSClient != null && webOSClient.isOpen()) {
            webOSClient.sendPointerCommand("move", null, dx, dy);
        }
    }

    public void handleRemoteButton(String buttonName) {
        if (webOSClient != null && webOSClient.isOpen()) {
            webOSClient.sendPointerCommand("button", buttonName, 0, 0);
        }
    }

    public void handleShowToast(String message){
        try {
            JSONObject payload = new JSONObject();
            payload.put("message", message);
            webOSClient.sendSystemCommand("system.notifications/createToast", payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestPointerSocket(){
        webOSClient.sendSystemCommand("com.webos.service.networkinput/getPointerInputSocket", null);
    }

    public void subscribeKeyboard(){
        if(webOSClient != null && webOSClient.isOpen()){
            try {
                JSONObject payload = new JSONObject();
                payload.put("subscribe", true);
                webOSClient.sendSystemCommand("com.webos.service.ime/registerRemoteKeyboard", payload);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void insertKeyboardText(String newChars, boolean isReplace){
        if(webOSClient != null && webOSClient.isOpen()){
            try{
                JSONObject payload = new JSONObject();
                payload.put("text", newChars);
                payload.put("replace", isReplace);
                webOSClient.sendSystemCommand("com.webos.service.ime/insertText", payload);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void deleteKeyboardText(int count){
        if(webOSClient != null && webOSClient.isOpen()){
            try{
                JSONObject payload = new JSONObject();
                payload.put("count", count);
                webOSClient.sendSystemCommand("com.webos.service.ime/deleteCharacters", payload);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void sendEnterKey(){
        if(webOSClient != null && webOSClient.isOpen()){
            webOSClient.sendSystemCommand("com.webos.service.ime/sendEnterKey", null);
        }
    }

    public void streamMedia(String url){
        try {

            Log.e("DLNA", "Video URL: " + url);
            JSONObject payload = new JSONObject();
            payload.put("target", url);

            webOSClient.sendSystemCommand("system.launcher/open", payload);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}

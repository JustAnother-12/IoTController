package com.example.iotcontroller.controllers;

import android.content.Context;
import android.media.AudioManager;

public class VolumeController {
    private AudioManager audioManager;

    public VolumeController(Context context) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void adjustVolume(int direction){
        int flag = AudioManager.FLAG_SHOW_UI;
        if(direction > 0){
            this.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, flag);
        }else{
            this.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, flag);
        }
    }
}

package com.example.iotcontroller.interfaces;

public interface OnSensorActionListener {
    void onFlashlightTargetChanged();
    void onVolumeTargetChanged(int direction); // 1: incre, -1: decre
    void onMediaSkipTarget(boolean forward);
    void onActionTriggered(String actionName); // for IoTs
}

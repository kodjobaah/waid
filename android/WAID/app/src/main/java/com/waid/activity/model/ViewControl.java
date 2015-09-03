package com.waid.activity.model;

/**
 * Created by kodjobaah on 19/08/2015.
 */
public class ViewControl {
    private boolean switchCamera;
    private boolean startTransmission;
    private boolean cameraStarted;
    private boolean recording;

    public void setSwitchCamera(boolean switchCamera) {
        this.switchCamera = switchCamera;
    }

    public boolean isSwitchCamera() {
        return switchCamera;
    }

    public void setStartTransmission(boolean startTransmission) {
        this.startTransmission = startTransmission;
    }

    public boolean isStartTransmission() {
        return startTransmission;
    }

    public void setCameraStarted(boolean cameraStarted) {
        this.cameraStarted = cameraStarted;
    }

    public boolean isCameraStarted() {
        return cameraStarted;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }
}

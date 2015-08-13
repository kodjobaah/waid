package com.waid.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.TextView;

import com.waid.activity.main.WhatAmIdoing;

import java.text.DecimalFormat;

/**
 * Created by kodjobaah on 10/08/2015.
 */
public class SensorRotationListener implements SensorEventListener {

    public static final String TAG = "OrientationTestActivity";
    private final WhatAmIdoing mActivity;
    private final Sensor mAccelerometer;
    private final Sensor mMagnetometer;
    private final TextView view;
    private final SensorFusion sensorFusion;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    public SensorRotationListener(WhatAmIdoing whatAmIdoing, Sensor mAccelerometer, Sensor mMagnetometer, TextView view) {
        this.mActivity = whatAmIdoing;
        this.mAccelerometer = mAccelerometer;
        this.mMagnetometer = mMagnetometer;
        this.view = view;
        sensorFusion = new SensorFusion();
        sensorFusion.setMode(SensorFusion.Mode.ACC_MAG);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorFusion.setAccel(event.values);
                sensorFusion.calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                sensorFusion.gyroFunction(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorFusion.setMagnet(event.values);
                break;
        }


        double azimuthValue = sensorFusion.getAzimuth();
        double rollValue = sensorFusion.getRoll();
        double pitchValue = sensorFusion.getPitch();
        DecimalFormat df = new DecimalFormat("#");

        view.setText(String.format("Y%s\nP%s\nR%s",
                df.format(azimuthValue), df.format(rollValue), df.format(pitchValue)));

        int sensorEventType = event.sensor.getType();

        /*
        if (sensorEventType == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (sensorEventType == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            DecimalFormat df = new DecimalFormat("#");

            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            view.setText(String.format("Y%s\nP%s\nR%s",
                    df.format(mOrientation[0] * 57.2957795), df.format(mOrientation[1] * 57.2957795), df.format(mOrientation[2] * 57.2957795)));
            mLastAccelerometerSet = false;
            mLastMagnetometerSet = false;
            Log.i(TAG,"orientation["+calculateOrientation(mOrientation[2],mOrientation[1]));
         }
         */
    }

    public static final int ORIENTATION_PORTRAIT = 0;
    public static final int ORIENTATION_LANDSCAPE_REVERSE = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int ORIENTATION_PORTRAIT_REVERSE = 3;
    public int orientation = ORIENTATION_PORTRAIT;

    private int calculateOrientation(double roll, double pitch) {
        if (((orientation == ORIENTATION_PORTRAIT || orientation == ORIENTATION_PORTRAIT_REVERSE)
                && (roll > -30 && roll < 30))) {
            if (pitch > 0)
                return ORIENTATION_PORTRAIT_REVERSE;
            else
                return ORIENTATION_PORTRAIT;
        } else {
            // divides between all orientations
            if (Math.abs(pitch) >= 30) {
                if (pitch > 0)
                    return ORIENTATION_PORTRAIT_REVERSE;
                else
                    return ORIENTATION_PORTRAIT;
            } else {
                if (roll > 0) {
                    return ORIENTATION_LANDSCAPE_REVERSE;
                } else {
                    return ORIENTATION_LANDSCAPE;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

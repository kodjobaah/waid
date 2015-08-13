package com.waid.activity.main;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.util.Log;
import android.view.WindowManager;

import com.waid.nativecamera.GL2JNILib;

/**
 * Created by kodjobaah on 31/07/2015.
 */
public class OrientationListener extends OrientationEventListener {

    private static final String TAG = "OrientationListener";
    private final Context context;

    public OrientationListener(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public void onOrientationChanged(int orientation) {

        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int screenOrientation = display.getRotation();
        switch (screenOrientation){
            default:
            case Surface.ROTATION_0: // Portrait
                Log.i(TAG,"---ROATION 0");
                break;
            case Surface.ROTATION_90: // Landscape right
                // do smth.
                Log.i(TAG,"---ROTATION 90");
                break;
            case Surface.ROTATION_180: // Landscape left
                // do smth.
                Log.i(TAG,"--ROTATION-180");
                break;
            case Surface.ROTATION_270:
                Log.i(TAG,"--ROTATION-270");
                break;
        }
        int orie = context.getResources().getConfiguration().orientation;
        if (Configuration.ORIENTATION_LANDSCAPE == orie) {
            Log.i(TAG,"ORIENTATION-LANDSCAPE");
            GL2JNILib.orientationChange(0);
        } else {
            Log.i(TAG,"ORIENTATION-PORTRAIT");
            GL2JNILib.orientationChange(1);
        }


    }
}

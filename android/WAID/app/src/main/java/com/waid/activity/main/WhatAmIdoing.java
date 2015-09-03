package com.waid.activity.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.waid.R;
import com.waid.activity.login.LoginActivity;
import com.waid.activity.model.ViewControl;
import com.waid.contentproviders.Authentication;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.contentproviders.LinkedInAuthenticationToken;
import com.waid.contentproviders.StateAttribute;
import com.waid.contentproviders.TwitterAuthenticationToken;
import com.waid.nativecamera.GL2JNILib;
import com.waid.nativecamera.GL2JNIView;
import com.waid.nativecamera.NativeToJavaMessenger;
import com.waid.sensors.SensorRotationListener;
import com.waid.sensors.WaidLocationListener;
import com.waid.utils.ViewGroupUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.UUID;


public class WhatAmIdoing extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            System.exit(-1);
        }
    }

    //Sensor Items
    private Sensor mMagnetometer;
    private WaidLocationListener locationListener;
    private SensorRotationListener sensorRotationListener;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private LocationManager mLocationManager;

    //Camera Control Items
    private SurfaceView viewToReplace;
    private GL2JNIView mView;
    //private OrientationListener orientationListener;

    private Boolean onPause = false;

    private WhatAmIdoing mActivty;
    private String TAG = "WhatAmIdoing";
    private ViewControl viewControl;

    public WhatAmIdoing() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StateAttribute sa = DatabaseHandler.getInstance(mActivty).getStateAttribute(StateAttribute.ON_PAUSE);

        if (sa == null) {
            sa = new StateAttribute(UUID.randomUUID().toString(),StateAttribute.ON_PAUSE,"false");
        }

        Log.i(TAG, "APPINIT_onCreate_onPause[" + sa.getValue() + "]");

        if (Boolean.valueOf(sa.getValue()) == false) {
            setContentView(R.layout.activity_what_am_idoing);

            mActivty = this;
            //orientationListener = new OrientationListener(this);
            viewControl = new ViewControl();

            viewControl.setSwitchCamera(true);
            viewControl.setStartTransmission(false);
            viewControl.setCameraStarted(true);
            viewControl.setRecording(false);


            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            // mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        StateAttribute sa = DatabaseHandler.getInstance(mActivty).getStateAttribute(StateAttribute.ON_PAUSE);
        if (sa == null) {
            sa = new StateAttribute(UUID.randomUUID().toString(),StateAttribute.ON_PAUSE,"false");
        } else {
            sa.setValue("false");
        }
        DatabaseHandler.getInstance(mActivty).putStateAttribute(sa);
        Log.i(TAG,"APPINIT_onStop");
    }


    @Override
    public void onResume()
    {
        super.onResume();

        StateAttribute sa = DatabaseHandler.getInstance(mActivty).getStateAttribute(StateAttribute.ON_PAUSE);
        if (sa == null) {
            sa = new StateAttribute(UUID.randomUUID().toString(),StateAttribute.ON_PAUSE,"false");
        }

        Log.i(TAG,"APPINIT_onResume["+sa.getValue()+"]");
        if (Boolean.valueOf(sa.getValue()) == false) {
            Log.i(TAG,"APPINIT_onResume_creating");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
            viewToReplace = (SurfaceView) findViewById(R.id.viewToReplace);
            final DisplayMetrics display = this.getResources().getDisplayMetrics();
            mView = new GL2JNIView(getApplication(), display);
            ViewGroupUtils.replaceView(viewToReplace, mView);
            init();

            TextView view = (TextView) findViewById(R.id.totalWatchers);
            registerSensorManagerListeners(view);
        } else {
            sa.setValue("false");
            DatabaseHandler.getInstance(mActivty).putStateAttribute(sa);
            Log.i(TAG, "APPINIT_onResume_no_creating["+sa.getContent()+"]");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG,"APPINIT_onStart");
    }
    @Override
    public void onRestart() {
        super.onRestart();
        Log.i(TAG,"APPINIT_onRestart");
    }

    public void registerSensorManagerListeners(TextView view) {

        sensorRotationListener = new SensorRotationListener(this,mAccelerometer,mMagnetometer,view);
        locationListener = new WaidLocationListener();

        mSensorManager.registerListener(sensorRotationListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(sensorRotationListener, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0, 0, locationListener);

        mSensorManager.registerListener(sensorRotationListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(sensorRotationListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(sensorRotationListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_what_am_idoing, menu);
        menu.findItem(R.id.logout).setIcon(
                resizeImage(R.drawable.logout, 120, 120));
        return true;
    }

    private Drawable resizeImage(int resId, int w, int h) {
        // load the origial Bitmap
        Bitmap BitmapOrg = BitmapFactory.decodeResource(getResources(), resId);
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;
        // calculate the scale
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                height, matrix, true);
        return new BitmapDrawable(resizedBitmap);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.logout) {
            Authentication auth = DatabaseHandler.getInstance(mActivty).getDefaultAuthentication();
            if (auth != null) {
                    DatabaseHandler.getInstance(mActivty).removeAuthentication(auth);

            }

            TwitterAuthenticationToken tat = DatabaseHandler.getInstance(mActivty).getDefaultTwitterAuthentication();
            if (tat != null) {
                    DatabaseHandler.getInstance(mActivty).removeAuthentication(tat);

            }

            LinkedInAuthenticationToken la = DatabaseHandler.getInstance(mActivty).getDefaultLinkedinAuthentication();
            if (la != null) {
                    DatabaseHandler.getInstance(mActivty).removeAuthentication(la);

            }

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);

        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        //GL2JNILib.cleanUp();
        //mView.setVisibility(View.GONE);

        StateAttribute sa = DatabaseHandler.getInstance(mActivty).getStateAttribute(StateAttribute.ON_PAUSE);
        if (sa == null) {
            sa = new StateAttribute(UUID.randomUUID().toString(),StateAttribute.ON_PAUSE,"true");
        } else {
            sa.setValue("true");
        }
        DatabaseHandler.getInstance(mActivty).putStateAttribute(sa);
        Log.i(TAG,"APPINIT_onPause");
        super.onStop();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
       // orientationListener.disable();
        mView.setVisibility(View.GONE);
        Log.i(TAG, "APPINIT_onDestry");
        StateAttribute sa = DatabaseHandler.getInstance(mActivty).getStateAttribute(StateAttribute.ON_PAUSE);
        if (sa == null) {
            sa = new StateAttribute(UUID.randomUUID().toString(),StateAttribute.ON_PAUSE,"false");
        } else {
            sa.setValue("false");
        }
        DatabaseHandler.getInstance(mActivty).putStateAttribute(sa);
        /*
        TODO: GRACEFULL EXIT ALL ZEROMQ connections
         */
        GL2JNILib.cleanUp();
        finish();

    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    System.loadLibrary("gl2jni");
                } break;
                default:
                {
                    super.onManagerConnected(status);} break;
            }
        }
    };


    public void startVideo(final View view) {

        ImageButton startVideoButton  = (ImageButton) mActivty.findViewById(R.id.start_video);
        startVideoButton.setEnabled(false);
        Log.i(TAG, "--STOPPING--");
        if(viewControl.isCameraStarted()) {
            GL2JNILib.stopCamera();
            viewControl.setCameraStarted(false);
            startVideoButton.setImageResource(R.drawable.stop_camera);
        } else {
            GL2JNILib.startCamera();
            viewControl.setCameraStarted(true);
            startVideoButton.setImageResource(R.drawable.camera);
        }
        startVideoButton.setEnabled(true);

    }


    public void startTransmission(final View view) {
        Authentication auth =  DatabaseHandler.getInstance(this).getDefaultAuthentication();

        ImageButton startTransmissionButton = (ImageButton) mActivty.findViewById(R.id.start_transmission);
        startTransmissionButton.setEnabled(false);
        if (viewControl.isStartTransmission()) {
            viewControl.setStartTransmission(false);
            GL2JNILib.stopZeroMQ();
            startTransmissionButton.setImageResource(R.drawable.share_blue);
            Log.i(TAG, "STOPPING-ZMQ");

        } else  {
            startTransmissionButton.setImageResource(R.drawable.share_red);
            viewControl.setStartTransmission(true);
            String zeroMqUrl = getString(R.string.zeromq_url);
            //GL2JNILib.startZeroMQ("tcp://192.168.0.2:12345", auth.getToken());
            GL2JNILib.startZeroMQ(zeroMqUrl, auth.getToken());
            Log.i(TAG, "STARTING-ZMQ");
        }
        startTransmissionButton.setEnabled(true);
        Log.i(TAG,"START-TRANS-STATE["+startTransmissionButton.isEnabled()+"]");
    }

    public void switchCamera(final View view) {
        Log.i(TAG, "SWITCHING CAMERA");

        DisplayMetrics display = this.getResources().getDisplayMetrics();
        if(Camera.getNumberOfCameras() > 1) {
            //Disable buttons while switching camera's
            ImageButton startTransmissionButton = (ImageButton) mActivty.findViewById(R.id.start_transmission);
            startTransmissionButton.setEnabled(false);
            ImageButton startVideoButton  = (ImageButton) mActivty.findViewById(R.id.start_video);
            startVideoButton.setEnabled(false);

            if (viewControl.isSwitchCamera()) {
                viewControl.setSwitchCamera(false);
                GL2JNILib.restartCamera(1);
            } else {
                viewControl.setSwitchCamera(true);
                GL2JNILib.restartCamera(0);
            }

            startTransmissionButton.setEnabled(true);
            startVideoButton.setEnabled(true);

        }
    }

    public void recording(final View view) {

        Authentication auth =  DatabaseHandler.getInstance(this).getDefaultAuthentication();

        ImageButton startRecordingButton  = (ImageButton) mActivty.findViewById(R.id.start_recording);
        startRecordingButton.setEnabled(false);
        if (viewControl.isRecording()) {
            viewControl.setRecording(false);
            startRecordingButton.setImageResource(R.drawable.camera);
            startRecordingButton.setEnabled(true);
            GL2JNILib.stopRecording();
        } else {
            viewControl.setRecording(true);
            startRecordingButton.setEnabled(true);
            startRecordingButton.setImageResource(R.drawable.stop_camera);
            String zeroMqUrl = getString(R.string.zeromq_url);
            GL2JNILib.startRecording(zeroMqUrl, auth.getToken());
        }
    }
    public void init() {
        NativeToJavaMessenger zeromMessenger = new NativeToJavaMessenger(mActivty,viewControl);
        GL2JNILib.storeMessenger(zeromMessenger);
    }
}

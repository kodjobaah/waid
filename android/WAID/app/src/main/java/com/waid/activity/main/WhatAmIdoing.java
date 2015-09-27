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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.linkedin.platform.APIHelper;
import com.linkedin.platform.LISession;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.utils.Scope;
import com.waids.R;
import com.waid.activity.login.LoginActivity;
import com.waid.activity.model.ViewControl;
import com.waid.contentproviders.Authentication;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.contentproviders.LinkedInAuthenticationToken;
import com.waid.contentproviders.StateAttribute;
import com.waid.contentproviders.TwitterAuthenticationToken;
import com.waid.invite.email.InviteListTask;
import com.waid.invite.facebook.ShareContentTask;
import com.waid.invite.facebook.fragment.FaceBookFragment;
import com.waid.invite.linkedin.LinkedInShareContentTask;
import com.waid.invite.twitter.TwitterAuthorization;
import com.waid.invite.twitter.TwitterAuthorizationTask;
import com.waid.nativecamera.GL2JNILib;
import com.waid.nativecamera.GL2JNIView;
import com.waid.nativecamera.NativeToJavaMessenger;
import com.waid.sensors.SensorRotationListener;
import com.waid.sensors.WaidLocationListener;
import com.waid.services.ConnectionService;
import com.waid.tasks.ZeroMqTasks;
import com.waid.utils.AlertMessages;
import com.waid.utils.ViewGroupUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import twitter4j.auth.AccessToken;


public class WhatAmIdoing extends AppCompatActivity {


    private static final String TAG ="WhatAmIdoing";

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            //System.exit(-1);
            Log.i(TAG,"APPINIT_ERROR_INITIALIZING_OPENCV");
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
    private ViewControl viewControl;
    private ConnectionService connectionService;
    private InviteListTask mInviteListTask;
    private TwitterAuthorization twitterAuthorization;
    private FaceBookFragment facebookFragment;
    private CallbackManager facebookCallbackManager;

    public WhatAmIdoing() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        facebookCallbackManager = CallbackManager.Factory.create();

        if (mActivty != null) {
            StateAttribute sa = DatabaseHandler.getInstance(mActivty).getStateAttribute(StateAttribute.ON_PAUSE);

            if (sa == null) {
                sa = new StateAttribute(UUID.randomUUID().toString(), StateAttribute.ON_PAUSE, "false");
            } else {
                sa.setValue("true");
            }
        }

        connectionService = new ConnectionService(this);
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

        Log.i(TAG, "APPINIT_onResume[" + sa.getValue() + "]");
        if (Boolean.valueOf(sa.getValue()) == false) {
            Log.i(TAG, "APPINIT_onResume_creating");
            System.loadLibrary("gl2jni");

            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
            viewToReplace = (SurfaceView) findViewById(R.id.viewToReplace);
            final DisplayMetrics display = this.getResources().getDisplayMetrics();
            mView = new GL2JNIView(getApplication(), display,this);

            if (((GL2JNIView)mView).isCameraOpened()) {
                Log.i(TAG,"APPINIT_onResume_camera_opened");
                if (viewToReplace != null) {
                    Log.i(TAG, "APPINIT_onResume_viewToReplace_null");
                    ViewGroupUtils.replaceView(viewToReplace, mView);
                    init();

                    //TextView view = (TextView) findViewById(R.id.totalWatchers);
                   // registerSensorManagerListeners(view);
                } else {

                    if (twitterAuthorization != null) {
                        StateAttribute saBrowser = DatabaseHandler.getInstance(mActivty).getStateAttribute(StateAttribute.OPEN_BROWSER);
                        if ((sa != null) && (saBrowser.getValue().equalsIgnoreCase("true"))) {
                                twitterAuthorization.displayPinEntry();
                                twitterAuthorization = null;
                        }
                    }
                }

            } else {

                Log.i(TAG,"APPINIT_onResume_camera_not_opened");
                CharSequence text = "Can not attach to camera - Try restarting WAID or your Device";
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(mActivty, text, duration);
                toast.show();

                /*
                 *  TODO: DISABLE ALL BUTTONS
                 */
            }
        } else {
            sa.setValue("false");
            DatabaseHandler.getInstance(mActivty).putStateAttribute(sa);
            Log.i(TAG, "APPINIT_onResume_no_creating[" + sa.getContent() + "]");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "APPINIT_onStart");
    }
    @Override
    public void onRestart() {
        super.onRestart();
        StateAttribute sa = DatabaseHandler.getInstance(mActivty).getStateAttribute(StateAttribute.ON_PAUSE);
        if (sa == null) {
            sa = new StateAttribute(UUID.randomUUID().toString(),StateAttribute.ON_PAUSE,"false");
        } else {
            sa.setValue("true");
        }
        Log.i(TAG, "APPINIT_onRestart");
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
    public boolean onMenuOpened(int featureId, Menu menu)
    {
        if(featureId == Window.FEATURE_ACTION_BAR && menu != null){
            if(menu.getClass().getSimpleName().equals("MenuBuilder")){
                try{
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                }
                catch(NoSuchMethodException e){
                    Log.e(TAG, "onMenuOpened", e);
                }
                catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_what_am_idoing, menu);

        menu.findItem(R.id.linkedin_menu_item).setIcon(
                resizeImage(R.drawable.linkedin, 100, 95));
        menu.findItem(R.id.facebook_menu_item).setIcon(
                resizeImage(R.drawable.facebook, 108, 108));
        menu.findItem(R.id.twitter_menu_item).setIcon(
                resizeImage(R.drawable.twitter, 160, 160));
        menu.findItem(R.id.email_menu_item).setIcon(
                resizeImage(R.drawable.mail, 120, 120));

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

            GL2JNILib.stopCamera();
            startActivity(intent);

        }


        if (viewControl.isStartTransmission()) {

            //noinspection SimplifiableIfStatement
            if (id == R.id.email_menu_item) {
                sendEmail();
                return true;
            }

            if (id == R.id.twitter_menu_item) {

                twitterAuthorization = new TwitterAuthorization(mActivty);
                AccessToken accessToken = twitterAuthorization.getAccessToken();
                Log.i(TAG, "--------------------------- should be tweeig");
                if (accessToken == null) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TwitterAuthorizationTask taTask = new TwitterAuthorizationTask(mActivty, twitterAuthorization);
                            taTask.execute((Void) null);

                            StateAttribute sa = new StateAttribute(UUID.randomUUID().toString(), StateAttribute.OPEN_BROWSER, "true");
                            DatabaseHandler.getInstance(mActivty).putStateAttribute(sa);
                        }
                    });

                } else {
                    tweetWhatIAmDoing();
                }
            }

            if (id == R.id.facebook_menu_item) {
                shareOnFacebook();
            }

            if (id == R.id.linkedin_menu_item) {
                LISessionManager sessionManager = LISessionManager.getInstance(getApplicationContext());
                LISession session = sessionManager.getSession();
                boolean accessTokenValid = session.isValid();

                if (accessTokenValid) {
                    Log.d(TAG, "Linkedin-Already-authenticated");
                    sendToLinkedIn();

                } else {

                    LISessionManager.getInstance(getApplicationContext()).init(mActivty, buildScope(), new AuthListener() {
                        @Override
                        public void onAuthSuccess() {
                            Log.d(TAG, "Linkedin-Authentication-Success");
                            sendToLinkedIn();
                        }

                        @Override
                        public void onAuthError(LIAuthError error) {
                            Log.d(TAG, "Linkedin-Authentication-error[" + error.toString() + "]");
                            AlertMessages.displayGenericMessageDialog(mActivty,error.toString());
                        }
                    }, true);
                    return true;
                }
            }
        } else {
            AlertMessages.displayGenericMessageDialog(mActivty,"You need to start sharing before you can send an invite");
        }
        return super.onOptionsItemSelected(item);
    }

    public void sendEmail() {
        mInviteListTask = new com.waid.invite.email.InviteListTask(mActivty);
        mInviteListTask.execute((Void) null);
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
        Log.i(TAG, "APPINIT_onPause");
        //super.onStop();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
       // orientationListener.disable();
        if (mView != null)
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
        TODO: GRACEFULL STOP CAMERA
         */
        GL2JNILib.stopCamera();
        finish();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Add this line to your existing onActivityResult() method
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        LISessionManager.getInstance(getApplicationContext()).onActivityResult(this, requestCode, resultCode, data);
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
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
        final Authentication auth =  DatabaseHandler.getInstance(this).getDefaultAuthentication();

        final ImageButton startTransmissionButton = (ImageButton) mActivty.findViewById(R.id.start_transmission);

            startTransmissionButton.setEnabled(false);
            if (viewControl.isStartTransmission()) {
                viewControl.setStartTransmission(false);
                GL2JNILib.stopZeroMQ();
                startTransmissionButton.setImageResource(R.drawable.share_blue);
                Log.i(TAG, "STOPPING-ZMQ");
                startTransmissionButton.setEnabled(true);

            } else {
                ZeroMqTasks zeroMqTasks = new ZeroMqTasks(mActivty,auth.getToken(),viewControl);
                zeroMqTasks.execute();

            }

            Log.i(TAG, "START-TRANS-STATE[" + startTransmissionButton.isEnabled() + "]");

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

    /*
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
    */

    public void init() {
        NativeToJavaMessenger nativeToJavaMessenger = new NativeToJavaMessenger(mActivty,viewControl);
        GL2JNILib.storeMessenger(nativeToJavaMessenger);
    }

    public void shareOnFacebook() {
        Log.d(TAG, "------------------------------- on click sharine (1)");


        com.facebook.AccessToken accessToken = com.facebook.AccessToken.getCurrentAccessToken();

        Log.d(TAG, "Facebook-accessToken[" + accessToken + "]");
        if ((accessToken != null) && (!accessToken.isExpired())) {


            ShareContentTask shareContentTask = new ShareContentTask(mActivty);
            shareContentTask.execute((Void) null);

        } else {

            //Login Callback registration
            LoginManager.getInstance().registerCallback(facebookCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    Toast.makeText(getApplicationContext(), "in LoginResult on success", Toast.LENGTH_LONG).show();

                    ShareContentTask shareContentTask = new ShareContentTask(mActivty);
                    shareContentTask.execute((Void) null);
                }

                @Override
                public void onCancel() {
                    Toast.makeText(getApplicationContext(), "in LoginResult on cancel", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(FacebookException exception) {
                    Toast.makeText(getApplicationContext(), "in LoginResult on error", Toast.LENGTH_LONG).show();
                }
            });

            ArrayList<String> permissions = new ArrayList<String>();
            permissions.add("publish_actions");
            LoginManager.getInstance().logInWithPublishPermissions(mActivty, permissions);

            /*
            setContentView(R.layout.facebook_share);

            android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

            Fragment prev =  getSupportFragmentManager().findFragmentByTag("Facebookshare");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            facebookFragment = FaceBookFragment.newInstance("Facebook share", mActivty);
            ft.add((android.support.v4.app.Fragment) facebookFragment,"Facebookshare");
            ft.addToBackStack(null);
            ft.commit();
            */
        }
    }

    public void tweetWhatIAmDoing() {
        TwitterAuthorization ta = new TwitterAuthorization(mActivty);
        AccessToken accessToken = ta.getAccessToken();
        if (accessToken != null) {
            String inviteUrl = mActivty
                    .getString(R.string.send_invite_twitter_url);
            Authentication auth = DatabaseHandler.getInstance(mActivty)
                    .getDefaultAuthentication();
            String url = inviteUrl + "?token=" + auth.getToken();
            com.waid.invite.twitter.SendTwitterInviteTask stit = new com.waid.invite.twitter.SendTwitterInviteTask(url,
                    mActivty);
            stit.execute((Void) null);
        }
    }
    // Build the list of member permissions our LinkedIn session requires
    private static Scope buildScope() {
        return Scope.build(Scope.R_BASICPROFILE, Scope.W_SHARE);
    }

    private void sendToLinkedIn() {

        LinkedInShareContentTask linkedInShareContentTask = new LinkedInShareContentTask(mActivty);
        linkedInShareContentTask.execute((Void)null);
    }
}

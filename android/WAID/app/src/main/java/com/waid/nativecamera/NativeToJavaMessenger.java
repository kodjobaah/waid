package com.waid.nativecamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.waid.R;
import com.waid.activity.login.LoginActivity;
import com.waid.activity.model.ViewControl;
import com.waid.contentproviders.Authentication;
import com.waid.contentproviders.DatabaseHandler;

/**
 * Created by kodjobaah on 09/08/2015.
 */
public class NativeToJavaMessenger {

    private static final String TAG = "NativeToJavaMessenger";
    private final Activity activity;
    private final ViewControl viewControl;

    public NativeToJavaMessenger(Activity activity, ViewControl viewControl) {
        this.activity = activity;
        this.viewControl = viewControl;
    }


    public void invalidToken() {

        activity.runOnUiThread(new Thread(new Runnable() {
            public void run() {

               /*
                Context context = activity.getApplicationContext();
                CharSequence text = "Authentication problems..please login again";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                */

                Authentication auth = DatabaseHandler.getInstance(activity).getDefaultAuthentication();
                DatabaseHandler.getInstance(activity).removeAuthentication(auth);

                Intent intent = new Intent(activity, LoginActivity.class);
                activity.startActivity(intent);
                activity.finish();
            }
        }));

    }


    public void connectionLost() {

        activity.runOnUiThread(new Thread(new Runnable() {
            public void run() {

                Context context = activity.getApplicationContext();
                CharSequence text = "Authentication problems..lost connection..please login again";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

                Authentication auth = DatabaseHandler.getInstance(activity).getDefaultAuthentication();
                DatabaseHandler.getInstance(activity).removeAuthentication(auth);
                Intent intent = new Intent(activity, LoginActivity.class);
                activity.startActivity(intent);
                activity.finish();
            }
        }));

    }

    public void connectionDropped() {

        activity.runOnUiThread(new Thread(new Runnable() {
            public void run() {


                ImageButton startTransmissionButton  = (ImageButton) activity.findViewById(R.id.start_transmission);
                startTransmissionButton.setImageResource(R.drawable.share_blue);
                Context context = activity.getApplicationContext();
                CharSequence text = "Disconnected from server!";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                startTransmissionButton.setEnabled(true);
                GL2JNILib.stopZeroMQ();
                viewControl.setStartTransmission(false);
            }
        }));

    }


    public void unableToConnect() {

        activity.runOnUiThread(new Thread(new Runnable() {
            public void run() {


                ImageButton startTransmissionButton  = (ImageButton) activity.findViewById(R.id.start_transmission);
                startTransmissionButton.setImageResource(R.drawable.share_blue);
                Context context = activity.getApplicationContext();
                CharSequence text = "Problems connecting to server!";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                startTransmissionButton.setEnabled(true);
                viewControl.setStartTransmission(false);
                Log.i(TAG,"START-TRANSMISSIO ["+viewControl.isStartTransmission()+"]");
                GL2JNILib.stopZeroMQ();
            }
        }));

    }


}

package com.waid.nativecamera;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageButton;
import android.widget.Toast;

import com.waid.R;

/**
 * Created by kodjobaah on 09/08/2015.
 */
public class ZeroMQMessenger {

    private final Activity activity;

    public ZeroMQMessenger(Activity activity) {
        this.activity = activity;
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
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                startTransmissionButton.setEnabled(true);
            }
        }));

    }


}

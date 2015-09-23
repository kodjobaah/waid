package com.waid.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.waids.R;
import com.waid.activity.model.ViewControl;
import com.waid.nativecamera.GL2JNILib;
import com.waid.services.ConnectionService;
import com.waid.utils.AlertMessages;
import com.waid.utils.HttpConnectionHelper;

import java.net.HttpURLConnection;

/**
 * Created by kodjobaah on 09/09/2015.
 */
public class ZeroMqTasks  extends AsyncTask<String, Void, Boolean> {

    private static final String TAG = "ZeroMqTask";
    private final Activity mActivity;
    private final String authToken;
    private final ViewControl viewControl;

    public ZeroMqTasks(Activity mActivity, String authToken, ViewControl viewControl) {
        this.mActivity = mActivity;
        this.authToken = authToken;
        this.viewControl =  viewControl;

    }

    @Override
    protected Boolean doInBackground(String[] params) {
        ConnectionService connectionService = new ConnectionService(mActivity);

        return connectionService.validateAuthentication(authToken);

    }

    @Override
    protected void onPostExecute(Boolean result) {

        final ImageButton startTransmissionButton = (ImageButton) mActivity.findViewById(R.id.start_transmission);
        if(result)
        {

            startTransmissionButton.setImageResource(R.drawable.share_red);
            viewControl.setStartTransmission(true);
            String zeroMqUrl = mActivity.getString(R.string.zeromq_url);
            //GL2JNILib.startZeroMQ("tcp://192.168.0.2:12345", auth.getToken());
            GL2JNILib.startZeroMQ(zeroMqUrl, authToken);
            Log.i(TAG, "STARTING-ZMQ");
        }

        else

        {
            String source = mActivity.getString(R.string.connect_from_diff_location);
            AlertMessages.displayGenericMessageDialog(mActivity, source);
        }
        startTransmissionButton.setEnabled(true);
    }
}

package com.waid.services;

import android.app.Activity;
import android.util.Log;

import com.waids.R;
import com.waid.utils.ConnectionResult;
import com.waid.utils.HttpConnectionHelper;

import java.net.HttpURLConnection;

/**
 * Created by kodjobaah on 09/09/2015.
 */
public class ConnectionService {

    private static final String TAG = "ConnectionService";
    private final Activity mActivity;

    public ConnectionService(Activity mActivity){
        this.mActivity = mActivity;
    }
    public Boolean validateAuthentication(String userToken) {
        HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper();

        String streamBuilderUrl=  mActivity.getString(R.string.stream_builder_url);
        String url = streamBuilderUrl+"/validate/"+userToken;

        Log.i(TAG,"Validate url["+url+"]");
        ConnectionResult results = httpConnectionHelper.connectGet(url);
        Log.i(TAG,"Connection From Validate["+results+"]");

        if (results == null) {
            return false;
        } else if(results.getStatusCode() != HttpURLConnection.HTTP_OK) {
            return false;
        }

        String response = results.getResult();
        Log.i(TAG,"Response From Validate["+response+"]");
        if (response.equalsIgnoreCase("true")) {
            return true;
        }
        return false;

    }
}

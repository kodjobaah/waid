package com.waid.sensors;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by kodjobaah on 11/08/2015.
 */
public class WaidLocationListener implements LocationListener {


    private static final String TAG = "WaidLocationListener";

    @Override
    public void onLocationChanged(Location location) {
            Log.i(TAG,"Latitude:“" +location.getLatitude()+"\nLongitude:"+location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
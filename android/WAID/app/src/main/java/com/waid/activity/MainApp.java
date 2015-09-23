package com.waid.activity;

import android.app.Application;

import com.facebook.FacebookSdk;

/**
 * Created by kodjobaah on 22/09/2015.
 */
public class MainApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
    }
}

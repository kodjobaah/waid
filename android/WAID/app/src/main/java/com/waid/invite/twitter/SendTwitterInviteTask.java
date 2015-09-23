package com.waid.invite.twitter;

import java.net.HttpURLConnection;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

import com.waids.R;
import com.waid.activity.main.WhatAmIdoing;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.contentproviders.StreamToken;
import com.waid.utils.ConnectionResult;
import com.waid.utils.HttpConnectionHelper;

import android.os.AsyncTask;
import android.util.Log;

public class SendTwitterInviteTask extends AsyncTask<Void, Void, Boolean> {

    private String url;
    private WhatAmIdoing context;
    private ConnectionResult results;

    public SendTwitterInviteTask() {

    }

    public SendTwitterInviteTask(String url, WhatAmIdoing context) {
        this.url = url;
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(Void... arg0) {
        return true;
    }

    @Override
    protected void onPostExecute(final Boolean success) {

        if (success) {

            TwitterAuthorization ta = new TwitterAuthorization(context);

            Twitter twitter = ta.buildTwitterFactory().getInstance();
            AccessToken at = ta.getAccessToken();
            twitter.setOAuthAccessToken(at);

            UpdateTwitterStatusTask updateTwitterStatusTask = new UpdateTwitterStatusTask(context, twitter);
            updateTwitterStatusTask.execute((Void) null);


        } else {
            Log.d("SendTwitterInviteTask", "failure:");

        }
    }

}

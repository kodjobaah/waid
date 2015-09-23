package com.waid.invite.twitter;

import android.os.AsyncTask;
import android.util.Log;

import com.waids.R;
import com.waid.activity.main.WhatAmIdoing;
import com.waid.utils.ConnectionResult;
import com.waid.utils.HttpConnectionHelper;

import java.net.HttpURLConnection;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

public class TwitterAuthorizationTask extends AsyncTask<Void, Void, Boolean> {

    private final TwitterAuthorization twitterAuthorization;
    private WhatAmIdoing context;
	private ConnectionResult results;

	public TwitterAuthorizationTask(WhatAmIdoing mActivty, TwitterAuthorization twitterAuthorization) {
        this.twitterAuthorization = twitterAuthorization;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {


		twitterAuthorization.authorizeTwitter();
        twitterAuthorization.collectTwitterPin();
		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {
            Log.d("TwitterAuthTask", "sucess:");

		}
	}

}

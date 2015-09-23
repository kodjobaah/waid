package com.waid.invite.twitter;

import android.os.AsyncTask;
import android.util.Log;

import com.waid.activity.main.WhatAmIdoing;
import com.waid.utils.ConnectionResult;

public class TwitterVerificationTask extends AsyncTask<Void, Void, Boolean> {

    private final TwitterAuthorization twitterAuthorization;
    private WhatAmIdoing context;
	private ConnectionResult results;

	public TwitterVerificationTask(WhatAmIdoing mActivty, TwitterAuthorization twitterAuthorization) {
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
            Log.i("TwitterAuthTask", "sucess:");

		}
	}

}

package com.waid.invite.twitter;

import java.net.HttpURLConnection;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import com.waid.R;
import com.waid.activity.main.WhatAmIdoing;
import com.waid.utils.ConnectionResult;
import com.waid.utils.HttpConnectionHelper;

import android.os.AsyncTask;
import android.util.Log;

public class TwitterAuthorizationTask extends AsyncTask<Void, Void, Boolean> {

	private String url;
	private WhatAmIdoing context;
	private ConnectionResult results;

	public TwitterAuthorizationTask() {

	}

	public TwitterAuthorizationTask(String url, WhatAmIdoing context) {
		this.url = url;
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper();
		results = httpConnectionHelper.connect(url);
		if (results == null) {
			return false;
		} else if (results.getStatusCode() != HttpURLConnection.HTTP_OK) {
			return false;
		} else if (results.getResult().contains("Unable")){
			return false;
		}

		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {

				TwitterAuthorization ta = new TwitterAuthorization(context);

				Twitter twitter = ta.buildTwitterFactory().getInstance();
				AccessToken at = ta.getAccessToken();
				twitter.setOAuthAccessToken(at);

				try {
					String url = context
							.getString(R.string.invite_location_url)
							+ "="
							+ results.getResult();
					twitter4j.Status status = twitter
							.updateStatus("I am using #WAID (What Am I Doing) to share a live stream, click here:"
									+ url);
				} catch (TwitterException e) {
					e.printStackTrace();
				}

		} else {
			Log.i("sendinviteemailtask.onpostexecute", "failure:");

		}
	}

}

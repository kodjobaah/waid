package com.waid.invite.twitter;

import android.app.Dialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.waids.R;
import com.waid.activity.main.WhatAmIdoing;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.contentproviders.TwitterAuthenticationToken;
import com.waid.utils.ConnectionResult;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TwitterVerificationTask extends AsyncTask<Void, Void, Boolean> {

	private final WhatAmIdoing mContext;
	private final Twitter twitter;
	private final Dialog dialog;
    private final RequestToken requestToken;
    private final String pin;
    private WhatAmIdoing context;
	private ConnectionResult results;
    public static final String TWITTER_AUTH_ID = "twitter_auth_id";

	public TwitterVerificationTask(WhatAmIdoing mContext,
                                   Twitter twitter,
                                   Dialog dialog,
                                   RequestToken requestToken,
                                   String pin) {
		this.mContext = mContext;
		this.twitter = twitter;
		this.dialog = dialog;
        this.requestToken = requestToken;
        this.pin = pin;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {


        AccessToken accessToken = null;
        try {
            accessToken = twitter.getOAuthAccessToken(requestToken, pin);

        } catch (TwitterException e) {

            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, mContext.getString(R.string.twitter_problems), Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
        }

        User user = null;
        if (accessToken != null) {
            try {
                user = twitter.verifyCredentials();
                TwitterAuthenticationToken tat = new TwitterAuthenticationToken(TWITTER_AUTH_ID, accessToken.getToken(), accessToken.getTokenSecret());
                DatabaseHandler.getInstance(mContext).putAuthentication(tat);
            } catch (TwitterException e) {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, mContext.getString(R.string.twitter_problems), Toast.LENGTH_LONG).show();
                    }
                });
                e.printStackTrace();
            }
        }

        if (user != null) {
            mContext.tweetWhatIAmDoing();
        }
        dialog.cancel();

		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {
            Log.d("TwitterAuthTask", "sucess:");

		}
	}

}

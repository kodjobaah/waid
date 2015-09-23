package com.waid.invite.twitter;

import android.os.AsyncTask;
import android.util.Log;

import com.waids.R;
import com.waid.activity.main.WhatAmIdoing;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.contentproviders.StreamToken;
import com.waid.utils.AlertMessages;
import com.waid.utils.ConnectionResult;

import java.util.UUID;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

public class UpdateTwitterStatusTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "UpdateTwitterStatusTask";
	private final Twitter twitter;
	private WhatAmIdoing context;
	private ConnectionResult results;


	public UpdateTwitterStatusTask(WhatAmIdoing context, Twitter twitter) {
		this.context = context;
		this.twitter = twitter;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {

		try {


            String url = context.getString(R.string.send_invite_twitter_url);
            String streamToken = DatabaseHandler.getInstance(context).getDefaultStreamToken().getToken();
            Log.d(TAG,"StreamToken["+streamToken+"]");
            url = url +"?token="+streamToken;

            //String inviteId = "twitter-"+UUID.randomUUID().toString();
            //String stream = "http://www.whatamidoing.info:9000/liveStream?streamId="+streamToken+"&ref="+inviteId;
			twitter4j.Status status = twitter
                    .updateStatus("I am using #WAID (What Am I Doing) to share a live stream, click here:"
							+ url);
		} catch (TwitterException e) {
			e.printStackTrace();
			return false;
		}


		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {

			Log.d(TAG,"Twitter updated");
			AlertMessages.displayGenericMessageDialog(context,"Twitter Updated");

		} else {
			Log.i(TAG, "Twitter update failed");
			AlertMessages.displayGenericMessageDialog(context,"Twitter update failed");

		}
	}

}

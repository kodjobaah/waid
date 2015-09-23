package com.waid.invite.linkedin;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.linkedin.platform.APIHelper;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.waids.R;
import com.waid.activity.main.WhatAmIdoing;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.contentproviders.StreamToken;
import com.waid.utils.AlertMessages;

import org.json.JSONException;
import org.json.JSONObject;

public class LinkedInShareContentTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "LinkedInShareContent";
	private WhatAmIdoing context;

    public LinkedInShareContentTask(WhatAmIdoing context) {
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {

        return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {
			String url = "https://api.linkedin.com/v1/people/~/shares";


			String submitedUrl = context.getString(R.string.send_invite_linkedin_url);

			StreamToken streamToken = DatabaseHandler.getInstance(context).getDefaultStreamToken();
			submitedUrl = submitedUrl +"?token="+streamToken;

			JSONObject body = null;
			try {
				body = new JSONObject("{" +
						"\"comment\": \"WAID (What am I Doing)\"," +
						"\"visibility\": { \"code\": \"anyone\" }," +
						"\"content\": { " +
						"\"title\": \""+context.getString(R.string.facebook_feed_dialog_name)+"\"," +
						"\"description\":\""+context.getString(R.string.facebook_feed_dialog_description)+"\"," +
						"\"submitted-url\":\""+submitedUrl+"\"," +
						"\"submitted-image-url\":\""+context.getString(R.string.facebook_feed_dialog_picture)+"\"" +
						"}" +
						"}");

			} catch (JSONException e) {
				e.printStackTrace();
			}

			APIHelper apiHelper = APIHelper.getInstance(context.getApplicationContext());
			apiHelper.postRequest(context, url, body, new ApiListener() {
				@Override
				public void onApiSuccess(ApiResponse apiResponse) {
                    Log.d(TAG, "Succesful shared on linkedin");
					AlertMessages.displayGenericMessageDialog(context, "Message Posted on linkedIn");
				}

				@Override
				public void onApiError(LIApiError liApiError) {
                    liApiError.printStackTrace();
					AlertMessages.displayGenericMessageDialog(context, liApiError.getLocalizedMessage());
				}
			});


        }
	}

}

package com.waid.tasks;

import java.net.HttpURLConnection;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.waid.utils.AlertMessages;
import com.waid.utils.ConnectionResult;
import com.waid.utils.HttpConnectionHelper;
import com.waid.utils.WaidUtils;

public class SendInviteEmailTask extends AsyncTask<Void, Void, Boolean> {

	
	private String url;
	private Activity context;
	private ConnectionResult results;

	public SendInviteEmailTask() {
		
	}
	public SendInviteEmailTask(String url, Activity context) {
		this.url = url;
		this.context = context;
	}
	@Override
	protected Boolean doInBackground(Void... arg0) {
		HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper();
		results = httpConnectionHelper.connect(url);
		if (results == null) {
			return false;
		} else if(results.getStatusCode() != HttpURLConnection.HTTP_OK) {
			return false;
		}
		
		return true;
	}
	
	@Override
	protected void onPostExecute(final Boolean success) {
  
        
		if (success) {
			AlertMessages.displayGenericMessageDialog(context, "Invite Email Sent");
		} else {
			AlertMessages.displayGenericMessageDialog(context, "Unable to send invite email. Check network connection");
		}
	}

}

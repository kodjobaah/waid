package com.waid.tasks;

import java.net.HttpURLConnection;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.waid.utils.AlertMessages;
import com.waid.utils.ConnectionResult;
import com.waid.utils.HttpConnectionHelper;

public class SendLocationInformationTask extends AsyncTask<Void, Void, Boolean> {

	
	private String url;
	private Activity context;
	private ConnectionResult results;

	public SendLocationInformationTask() {
		
	}
	public SendLocationInformationTask(String url, Activity context) {
		this.url = url;
		this.context = context;
	}
	@Override
	protected Boolean doInBackground(Void... arg0) {
		HttpConnectionHelper httpConnectionHelper = new HttpConnectionHelper();
		results = httpConnectionHelper.connectGet(url);
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
			AlertMessages.displayGenericMessageDialog(context, "Location sent");

		} else {
			AlertMessages.displayGenericMessageDialog(context, "Unable to send location");
			
		}
	}

}

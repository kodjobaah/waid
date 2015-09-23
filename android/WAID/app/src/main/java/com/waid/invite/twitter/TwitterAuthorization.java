package com.waid.invite.twitter;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.waids.R;
import com.waid.activity.main.WhatAmIdoing;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.contentproviders.TwitterAuthenticationToken;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterAuthorization {
	public static final String TWITTER_CONSUMER_KEY ="KVI4hPHUGvzgr0oKPAW13ZNWQ";
	public static final String TWITTER_CONSUMER_SECRET="vRvyK0XBsXJx8CV7XZh8yXYj2VX9XhC0pJeHjdPvhu1dcYu7Qf";
	public static final String  TWITTER_CALLBACK_URL = "oob";
	
	protected static final String TAG = "TwitterAuthorization";

	private Dialog dialog;
	private RequestToken requestToken;
	private AccessToken accessToken = null;
	private WhatAmIdoing mContext;
	private Twitter twitter;

	public TwitterAuthorization(WhatAmIdoing context){
			this.mContext = context;
	}
	
	public void authorizeTwitter() {


		TwitterFactory factory = buildTwitterFactory();

		twitter = factory.getInstance();
		try {
			requestToken = twitter.getOAuthRequestToken();
			Log.d(TAG, "----TWITTER AUTHENTICATION URL[" + requestToken.getAuthenticationURL() + "]");
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public RequestToken getRequestToken() {
 		return requestToken;
	}

	public void setRequestToken(RequestToken requestToken) {
		this.requestToken = requestToken;
	}

	public void collectTwitterPin() {
		mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));

	}

	public void displayPinEntry() {

		dialog = new Dialog(mContext, R.style.ThemeWithCorners);
		dialog.setContentView(R.layout.twitter_auth_layout);
        dialog.setTitle(mContext.getString(R.string.twitter_auth_dialog));
        dialog.setCancelable(true);
        
        final EditText twitterPinText = (EditText)dialog.findViewById(R.id.twitterPin);
        
	    dialog.show();
        //Add event listeners
        Button cancel = (Button) dialog.findViewById(R.id.twitterPinButton);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String pin = twitterPinText.getText().toString().trim();
				Log.d(TAG,"----PIN["+pin+"]");
				TwitterVerificationTask twitterVerificationTask = new TwitterVerificationTask(mContext,twitter,dialog,requestToken,pin);
              	twitterVerificationTask.execute((Void) null);
              }
            
        });
    }

	public TwitterFactory buildTwitterFactory() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
		builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
		
		Configuration configuration = builder.build();
		TwitterFactory factory = new TwitterFactory(configuration);
		return factory;
	}
	
	public AccessToken getAccessToken() {		
		 TwitterAuthenticationToken tat = DatabaseHandler.getInstance(mContext).getDefaultTwitterAuthentication();
		 if (tat == null) {
			 return null;
		 }
		 
		 return new AccessToken(tat.getToken(), tat.getSecret());
	}
}

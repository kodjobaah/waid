package com.waid.invite.email;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import com.waid.activity.main.WhatAmIdoing;
import com.waid.contentproviders.Authentication;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.invite.email.callback.InviteDialogInteraction;
import com.waid.invite.email.model.Invite;
import com.waid.invite.email.task.ContactsListTask;
import com.waid.utils.ConnectionResult;
import com.waid.utils.HttpConnectionHelper;

public class InviteListTask extends AsyncTask<Void, Void, Boolean> implements InviteDialogInteraction {

    private static final String[] PROJECTION = new String[] {
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.DATA
    };

    private static final String TAG = "InviteListTasks";
    private final ArrayList<Invite> invites;
    private String inviteList = null;
	private WhatAmIdoing mContext;
	private View mInviteStatusView;
	private TextView mInviteStatusMessageView;
	private View mInviteFormView;
	
	private InviteEmailFragment inviteEmailFragment;

	public InviteListTask(final WhatAmIdoing context) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		this.mContext = context;
		
		
		FragmentTransaction ft = mContext.getSupportFragmentManager().beginTransaction();
	    Fragment prev =  mContext.getSupportFragmentManager().findFragmentByTag("whoHasAcceptedFragment");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	  	inviteEmailFragment = InviteEmailFragment.newInstance("Whos Watching?",context,this);
		inviteEmailFragment.show(ft, "whoHasAcceptedFragment");
        invites = new ArrayList<Invite>();


	}
	
	@Override
	public void showInviteProgress(boolean state) {
		showProgress(state);
	}
	
	@Override
	public void setInviteForm(View view) {
		mInviteFormView = view;;
	}

	@Override
	public void setStatusView(View view) {
		mInviteStatusView = view;
		
	}
	
	@Override
	public void setInviteStatusMessage(TextView view) {
		mInviteStatusMessageView = view;
	}
	
	
	@Override
	protected Boolean doInBackground(Void... arg0) {

        Log.i(TAG,"READING-CONTACT_LIST");
        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, PROJECTION, null, null, null);
        if ((cursor != null) && (cursor.getCount() > 0)) {

                try {
                    final int contactIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID);
                    final int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    final int emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
                    long contactId;
                    String displayName, address;
                    while (cursor.moveToNext()) {
                        contactId = cursor.getLong(contactIdIndex);
                        displayName = cursor.getString(displayNameIndex);
                        address = cursor.getString(emailIndex);
                        Invite invite = new Invite(address,displayName,null);
                        invites.add(invite);

                     }
                } finally {
                    cursor.close();
                }
        }
        cursor.close();
        Log.i(TAG,"CONTACT_LIST_READ["+invites+"]");

		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {


		if (success) {


			inviteEmailFragment.populateContacts(invites);
			showProgress(false);
		} else {
			showProgress(false);
		}
	}


	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = mContext.getResources().getInteger(
					android.R.integer.config_shortAnimTime);


			if (mInviteStatusMessageView != null ) {
				mInviteStatusView.setVisibility(View.VISIBLE);
				mInviteStatusView.animate().setDuration(shortAnimTime)
				.alpha(show ? 1 : 0)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mInviteStatusView.setVisibility(show ? View.VISIBLE
								: View.GONE);
					}
				});

			}

			if (mInviteFormView != null) {
				mInviteFormView.setVisibility(View.VISIBLE);
				mInviteFormView.animate().setDuration(shortAnimTime)
				.alpha(show ? 0 : 1)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mInviteFormView.setVisibility(show ? View.GONE
								: View.VISIBLE);
					}
				});
			}

		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.

			if (mInviteStatusView != null) {
				mInviteStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			}

			if (mInviteFormView != null) {
				mInviteFormView.setVisibility(show ? View.GONE : View.VISIBLE);
			}
		}
	}

	@Override
	protected void onCancelled() {
	}

}

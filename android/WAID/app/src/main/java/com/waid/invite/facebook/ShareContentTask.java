package com.waid.invite.facebook;

import com.waid.activity.main.WhatAmIdoing;
import com.waid.invite.facebook.fragment.FaceBookFragment;
import com.waid.utils.ConnectionResult;
import com.waid.R;

import android.os.AsyncTask;
import android.os.Bundle;

public class ShareContentTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "FacebookPostTask";
    private FaceBookFragment facebookFragment;
    private String url;
	private WhatAmIdoing context;
	private ConnectionResult results;

	public ShareContentTask(String url, WhatAmIdoing context) {
		this.url = url;
		this.context = context;

        /*
        FragmentTransaction ft = context.getSupportFragmentManager().beginTransaction();
        Fragment prev =  context.getSupportFragmentManager().findFragmentByTag("FacebookFragment");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        facebookFragment = FaceBookFragment.newInstance("Share on Facebook?", context);
        facebookFragment.show(ft, "whoHasAcceptedFragment");
*/
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {

				String url = context.getString(R.string.invite_location_url)
						+ "=" + results.getResult();
				Bundle params = new Bundle();
				params.putString("description", context
						.getString(R.string.facebook_feed_dialog_description));
				params.putString("link", url);
				params.putString("name",
						context.getString(R.string.facebook_feed_dialog_name));
				params.putString("picture", context
						.getString(R.string.facebook_feed_dialog_picture));
				params.putString("display", context
						.getString(R.string.facebook_feed_dialong_display));


        } else {

		}
	}

}

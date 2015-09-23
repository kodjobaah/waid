package com.waid.invite.linkedin;

import android.net.Uri;
import android.os.AsyncTask;

import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.waid.R;
import com.waid.activity.main.WhatAmIdoing;

public class LinkedInShareContentTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "ShareContentTask";
	private WhatAmIdoing context;
    private ShareDialog shareDialog;

    public LinkedInShareContentTask(WhatAmIdoing context) {
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {

        shareDialog = new ShareDialog(context);
        return true;
	}

	@Override
	protected void onPostExecute(final Boolean success) {

		if (success) {

            String url = context.getString(R.string.invite_location_url);

				/*
                Bundle params = new Bundle();
				params.putString("display", context
                        .getString(R.string.facebook_feed_dialong_display));
                */

			ShareLinkContent linkContent = new ShareLinkContent.Builder()
					.setContentTitle(context.getString(R.string.facebook_feed_dialog_name))
					.setContentDescription(context.getString(R.string.facebook_feed_dialog_description))
					.setContentUrl(Uri.parse(url))
                    .setImageUrl(Uri.parse(context.getString(R.string.facebook_feed_dialog_picture)))
					.build();

            ShareDialog.show(context,linkContent);

        } else {

		}
	}

}

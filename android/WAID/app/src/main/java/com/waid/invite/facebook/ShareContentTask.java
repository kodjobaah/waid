package com.waid.invite.facebook;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.waids.R;
import com.waid.activity.main.WhatAmIdoing;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.contentproviders.StreamToken;
import com.waid.invite.facebook.fragment.FaceBookFragment;
import com.waid.utils.ConnectionResult;

public class ShareContentTask extends AsyncTask<Void, Void, Boolean> {

	private static final String TAG = "ShareContentTask";
	private WhatAmIdoing context;
    private ShareDialog shareDialog;

    public ShareContentTask(WhatAmIdoing context) {
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

            String url = context.getString(R.string.send_invite_facebook_url);

            StreamToken streamToken = DatabaseHandler.getInstance(context).getDefaultStreamToken();
            url = url +"?token="+streamToken.getToken();

            ShareLinkContent linkContent = new ShareLinkContent.Builder()
					.setContentTitle(context.getString(R.string.facebook_feed_dialog_name))
					.setContentDescription(context.getString(R.string.facebook_feed_dialog_description))
					.setContentUrl(Uri.parse(url))
                    .setImageUrl(Uri.parse(context.getString(R.string.facebook_feed_dialog_picture)))
					.build();

            //ShareDialog.show(context,linkContent);

            shareDialog.show(linkContent, ShareDialog.Mode.FEED);

        } else {

		}
	}

}

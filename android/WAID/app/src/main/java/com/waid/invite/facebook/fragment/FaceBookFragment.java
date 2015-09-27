package com.waid.invite.facebook.fragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.waid.activity.main.WhatAmIdoing;
import com.waid.invite.facebook.ShareContentTask;
import com.waids.R;

/**
 * Created by kodjobaah on 21/09/2015.
 */
public class FaceBookFragment extends Fragment {
    private static final String TAG = "FaceBookFragment";
    private static FaceBookFragment frag;
    private WhatAmIdoing context;
    private LoginButton loginButton;
    private CallbackManager callbackManager;

    public FaceBookFragment() {

    }


    public static FaceBookFragment newInstance(String title, WhatAmIdoing context) {

        if (frag == null) {
            frag = new FaceBookFragment();
            frag.setContext(context);
            Bundle args = new Bundle();
            args.putString("title", title);
            frag.setArguments(args);

            //frag.mContext = context;
            //frag.inviteDialogInteration = inviteDialogInteraction;

            //We want this Dialog to be a Fragment in fact,
            //otherwise there are problems with showing another fragment, the DeviceListFragment
            //frag.setShowsDialog(false);
            //wDialog.setStyle(SherlockDialogFragment.STYLE_NORMAL,android.R.style.Theme_Holo_Light_Dialog);
            //We don't want to recreate the instance every time user rotates the phone
            //frag.setRetainInstance(true);
            //Don't close the dialog when touched outside
            //frag.setCancelable(true);
        }

        return frag;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater,container,savedInstanceState);
        callbackManager = CallbackManager.Factory.create();

        Log.d(TAG,"onCreateView");
        View view = inflater.inflate(R.layout.facebook_login, container, false);

        loginButton = (LoginButton) view.findViewById(R.id.authButton);
        loginButton.setFragment(this);
        //loginButton.setReadPermissions("user_friends");
        loginButton.setPublishPermissions("publish_actions");

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.i(TAG, "Login Successful");
                AccessToken.setCurrentAccessToken(loginResult.getAccessToken());
                //frag.dismiss();

                ShareContentTask shareContentTask = new ShareContentTask(context);
                shareContentTask.execute((Void) null);

                context.getSupportFragmentManager().beginTransaction().remove(frag).commit();
            }

            @Override
            public void onCancel() {
                Log.i(TAG,"Login cancel");
                //frag.dismiss();

            }

            @Override
            public void onError(FacebookException exception) {
                Log.i(TAG,"Login Problems");
                exception.printStackTrace();
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void setContext(WhatAmIdoing context) {
        this.context = context;
    }

    public WhatAmIdoing getContext() {
        return context;
    }
}
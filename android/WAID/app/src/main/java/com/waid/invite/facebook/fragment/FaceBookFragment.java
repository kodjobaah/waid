package com.waid.invite.facebook.fragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.waid.R;

/**
 * Created by kodjobaah on 21/09/2015.
 */
public class FaceBookFragment extends DialogFragment{
    private static final String TAG = "FaceBookFragment";
    private LoginButton loginButton;
    private CallbackManager callbackManager;


    public static FaceBookFragment newInstance(String title, Activity context) {

        FaceBookFragment frag = new FaceBookFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        //frag.mContext = context;
        //frag.inviteDialogInteration = inviteDialogInteraction;

        //We want this Dialog to be a Fragment in fact,
        //otherwise there are problems with showing another fragment, the DeviceListFragment
        frag.setShowsDialog(false);
        //wDialog.setStyle(SherlockDialogFragment.STYLE_NORMAL,android.R.style.Theme_Holo_Light_Dialog);
        //We don't want to recreate the instance every time user rotates the phone
        frag.setRetainInstance(true);
        //Don't close the dialog when touched outside
        frag.setCancelable(true);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater,container,savedInstanceState);
        callbackManager = CallbackManager.Factory.create();

        View view = inflater.inflate(R.layout.facebook_login, container, false);

        loginButton = (LoginButton) view.findViewById(R.id.authButton);
        loginButton.setReadPermissions("user_friends");

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "Login Successful")
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
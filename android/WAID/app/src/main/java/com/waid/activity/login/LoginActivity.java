package com.waid.activity.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.waid.activity.main.WhatAmIdoing;
import com.waid.contentproviders.Authentication;
import com.waid.contentproviders.DatabaseHandler;
import com.waid.utils.AlertMessages;
import com.waid.utils.ConnectionResult;
import com.waid.utils.HttpConnectionHelper;
import com.waid.utils.SessionParser;

import com.waids.R;
import com.waid.utils.WaidUtils;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {


    public static final String TAG = "LoginActivity";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    private UserForgottenPasswordTask mForgottenPasswordTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private AutoCompleteTextView mLastNameView;
    private AutoCompleteTextView mfirstNameView;
    private int problemsConnecting;
    private TextView mWelcomeMessageText;
    private Button mForgotPasswordButton;
    private LoginActivity mActivity;


    @Override
    protected void onResume() {

        super.onResume();
        Authentication auth =  DatabaseHandler.getInstance(mActivity).getDefaultAuthentication();
        if (auth != null) {
            startCamera();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.i(TAG,"onCreate");

        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.waid.debug", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        mActivity = this;
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mLastNameView = (AutoCompleteTextView) findViewById(R.id.lastName);
        mfirstNameView = (AutoCompleteTextView) findViewById(R.id.lastName);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mWelcomeMessageText = (TextView) findViewById(R.id.welcome_messge);

        mForgotPasswordButton = (Button) findViewById(R.id.email_forgot_password);
        mForgotPasswordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resendPassword();
            }
        });


    }


    private void populateAutoComplete() {
        getLoaderManager().initLoader(0, null, this);
    }


    /*
     * Sends a user an email that they can use to rese their password
     */
    private void resendPassword() {
        mEmailView.setError(null);
        mPasswordView.setError(null);
        if (!WaidUtils.isNetworkConnectionAvailable(mActivity)) {
            AlertMessages.displayGenericMessageDialog(mActivity, getResources().getString(R.string.network_connection_problems));
            return;
        }

        if (mForgottenPasswordTask != null) {
            return;
        }

        boolean cancel = false;

        String email = mEmailView.getText().toString();
        View focusView = null;

        // Check for a valid password, if the user entered one.
        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mForgottenPasswordTask = new UserForgottenPasswordTask(email);
            mForgottenPasswordTask.execute((Void) null);
        }

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        if (!WaidUtils.isNetworkConnectionAvailable(mActivity)) {
            AlertMessages.displayGenericMessageDialog(mActivity, getResources().getString(R.string.network_connection_problems));
            return;
        }
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String lastName = mLastNameView.getText().toString();
        String firstName = mfirstNameView.getText().toString();


        boolean cancel = false;
        View focusView = null;

        boolean validEmail = true;
        // Check for a valid password, if the user entered one.

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            String error = "<font color='red'>"+getString(R.string.error_field_required)+"</font>";
            mEmailView.setError(Html.fromHtml(error));
            focusView = mEmailView;
            cancel = true;
            validEmail = false;
        } else if (!isEmailValid(email)) {
            String error = "<font color='red'>"+getString(R.string.error_invalid_email)+"</font>";
            mEmailView.setError(Html.fromHtml(error));
            focusView = mEmailView;
            cancel = true;
            validEmail = false;
        }

        if (validEmail && TextUtils.isEmpty(password)) {
            mPasswordView.requestFocus();
            String error = "<font color='red'>"+getString(R.string.error_field_required)+"</font>";
            mPasswordView.setError(Html.fromHtml(error));
            focusView = mPasswordView;
            cancel = true;
        } else if (validEmail && !isPasswordValid(password)) {
            mPasswordView.requestFocus();
            String error = "<font color='red'>"+getString(R.string.error_invalid_password)+"</font>";
            mPasswordView.setError(Html.fromHtml(error));
            focusView = mPasswordView;
            cancel = true;
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password, firstName, lastName);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {

        Pattern pattern = Patterns.EMAIL_ADDRESS;
        return pattern.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }


    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
        mLastNameView.setAdapter(adapter);
        mfirstNameView.setAdapter(adapter);
    }


    /**
     * Represents an asynchronous forgotten password used to email password
     */
    public class UserForgottenPasswordTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        UserForgottenPasswordTask(String email) {
            mEmail = email;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            String forgottenPasswordUrl = getResources().getString(R.string.forgotten_password_url);
            HttpConnectionHelper connectionHelper = new HttpConnectionHelper();
            Log.i(TAG, forgottenPasswordUrl);

            String urlVal = forgottenPasswordUrl + "?email=" + mEmail;
            ConnectionResult connectionResult = connectionHelper.connect(urlVal);

            boolean sent = false;
            try {
                if ((connectionResult != null) && (connectionResult.getStatusCode() == HttpURLConnection.HTTP_OK)) {

                    String accountVerification = getResources().getString(R.string.account_waiting_verification);
                    String result = connectionResult.getResult();
                    if (accountVerification.equalsIgnoreCase(result)) {
                        problemsConnecting = 10;
                    }
                    sent = true;
                } else {

                    if (connectionResult == null) {
                        Log.i(TAG, "Problems connecting");
                        problemsConnecting = -1;
                    } else {
                        Log.i(TAG, "Problems status cost [" + connectionResult.getStatusCode() + "] result=[" + connectionResult.getResult() + "]");
                    }
                }
            } finally {
                connectionHelper.closeConnection();
            }

            return sent;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mForgottenPasswordTask = null;
            showProgress(false);
            if (success) {

                if (problemsConnecting == 10) {
                    AlertMessages.displayGenericMessageDialog(mActivity,getResources().getString(R.string.account_verification_needed));
                } else {
                    AlertMessages.displayGenericMessageDialog(mActivity, getResources().getString(R.string.forgotten_password_sent));
                }
            } else {

                if (problemsConnecting != -1) {
                    String error = "<font color='red'>"+getString(R.string.error_email_does_not_exist)+"</font>";
                    mEmailView.setError(Html.fromHtml(error));
                    mEmailView.requestFocus();
                } else {
                    AlertMessages.displayGenericMessageDialog(mActivity, getResources().getString(R.string.network_connection_problems));
                }
            }

        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private final String mFirstName;
        private final String mLastName;
        private String result;

        UserLoginTask(String email, String password, String firstName, String lastName) {
            mEmail = email;
            mPassword = password;
            mFirstName = firstName;
            mLastName = lastName;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            String loginUrl = getResources().getString(R.string.login_url);
            HttpConnectionHelper connectionHelper = new HttpConnectionHelper();

            Log.i(TAG, loginUrl);
            try {
                String urlVal = loginUrl + "?email=" + mEmail + "&password=" + mPassword;
                ConnectionResult connectionResult = connectionHelper.connect(urlVal);

                if ((connectionResult != null) && (connectionResult.getStatusCode() == HttpURLConnection.HTTP_OK)) {
                    String newAuthSuccessMessage = getResources().getString(R.string.new_auth_success_message);
                    String authSuccessMessage = getResources().getString(R.string.auth_success_message);
                    String completeRegistration = getResources().getString(R.string.auth_complete_registration);
                    Log.i(TAG, "results from login[" + connectionResult.getResult() + "]");
                    result = connectionResult.getResult();
                    if (authSuccessMessage.equalsIgnoreCase(result)) {
                        SessionParser sessionParser = connectionHelper.getPlaySession();

                        if (sessionParser != null) {
                            Authentication auth = DatabaseHandler.getInstance(getApplicationContext()).getAuthentication(mEmail);

                            if (auth == null) {
                                auth = new Authentication(mEmail, sessionParser.getToken(), sessionParser.getPlaySession());
                            } else {
                                auth.setPlaySession(sessionParser.getPlaySession());
                                auth.setToken(sessionParser.getToken());
                            }

                            //Saves or updates
                            DatabaseHandler.getInstance(getApplicationContext()).putAuthentication(auth);
                        } else {
                            Log.i(TAG, "cookie not set result=["
                                    + result + "]");
                            result = null;
                        }
                    } else if(newAuthSuccessMessage.equalsIgnoreCase(result)) {
                        problemsConnecting = 10;
                    } else if (completeRegistration.equalsIgnoreCase(result)) {
                        problemsConnecting = 11;
                    } else {
                        Log.i(TAG, "failure result=["
                                + result + "]");
                        result = null;
                    }
                } else {

                    if (connectionResult == null) {
                        Log.i(TAG, "Problems connecting");
                        problemsConnecting = -1;
                    } else {
                        Log.i(TAG, "Problems status cost [" + connectionResult.getStatusCode() + "] result=[" + connectionResult.getResult() + "]");
                    }

                    result = null;
                }
            } finally {
                connectionHelper.closeConnection();
            }

            return result == null ? false : true;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                if (problemsConnecting == 10) {
                    AlertMessages.displayGenericMessageDialog(mActivity,getString(R.string.account_created));
                } else if (problemsConnecting == 11) {
                    AlertMessages.displayGenericMessageDialog(mActivity,getString(R.string.account_verification));
                } else {
                    startCamera();
                }
                 //finish();
            } else {
                mPasswordView.requestFocus();
                String error = "<font color='red'>"+getString(R.string.error_incorrect_password)+"</font>";
                mPasswordView.setError(Html.fromHtml(error));
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    private void startCamera() {
        Intent intent = new Intent(this, WhatAmIdoing.class);
        startActivity(intent);
        finish();
    }
}

